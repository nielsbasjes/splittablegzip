/*
 * Making GZip Splittable for Apache Hadoop
 * Copyright (C) 2011-2019 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.hadoop.io.compress;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For each "split" the gzipped input file is read from the beginning of the
 * file till the point where the split starts, thus reading, decompressing and
 * discarding (wasting!) everything that is before the start of the split.<br>
 * <br>
 * <b>FACT: Files compressed with the Gzip codec are <i>NOT SPLITTABLE</i>.
 * Never have been, never will be.</b><br>
 * <br>
 * This codec offers a trade off between "spent resources" and "scalability"
 * when reading Gzipped input files by simply always starting at the beginning
 * of the file.<br>
 * So in general this "splittable" Gzip codec will <b>WASTE</b> CPU time and
 * FileSystem IO (HDFS) and probably other system resources (Network) too to
 * reduce the "wall clock" time in some real-life situations.<br>
 * <br>
 * <b>When is this useful?</b><br>
 * Assume you have a heavy map phase for which the input is a 1GiB Apache httpd
 * logfile. Now assume this map takes 60 minutes of CPU time to run. Then this
 * task will take 60 minutes to run because all of that CPU time must be spent
 * on a single CPU core ... Gzip is not splittable!<br>
 * <br>
 * This codec will waste CPU power by always starting from the start of the
 * gzipped file and discard all the decompressed data until the start of the
 * split has been reached.<br>
 * <br>
 * Decompressing a 1GiB Gzip file usually takes only a few (2-4) minutes.<br>
 * So if a "60 minutes" input file is split into 4 equal parts then:<br>
 * <ol>
 * <li>the 1<sup>st</sup> map task will<br>
 * <ul>
 * <li>process the 1<sup>st</sup> split (15 minutes)</li>
 * </ul>
 * </li>
 *
 * <li>the 2<sup>nd</sup> map task will<br>
 * <ul>
 * <li><i>discard</i> the 1<sup>st</sup> split ( 1 minute ).</li>
 * <li><i>process</i> the 2<sup>nd</sup> split (15 minutes).</li>
 * </ul>
 * </li>
 *
 * <li>the 3<sup>rd</sup> map task will<br>
 * <ul>
 * <li><i>discard</i> the 1<sup>st</sup> split ( 1 minute ).</li>
 * <li><i>discard</i> the 2<sup>nd</sup> split ( 1 minute ).</li>
 * <li><i>process</i> the 3<sup>rd</sup> split (15 minutes).</li>
 * </ul>
 * </li>
 *
 * <li>the 4<sup>th</sup> task will<br>
 * <ul>
 * <li><i>discard</i> the 1<sup>st</sup> split ( 1 minute ).</li>
 * <li><i>discard</i> the 2<sup>nd</sup> split ( 1 minute ).</li>
 * <li><i>discard</i> the 3<sup>rd</sup> split ( 1 minute ).</li>
 * <li><i>process</i> the 4<sup>th</sup> split (15 minutes).</li>
 * </ul>
 * </li>
 * </ol>
 * Because all tasks run in parallel the running time in this example would be
 * 18 minutes (i.e. the worst split time) instead of the normal 60 minutes. We
 * have wasted about 6 minutes of CPU time and completed the job in about 30% of
 * the original wall clock time.<br>
 * <br>
 * <b>Using this codec</b>
 * <ol>
 * <li>Enable this codec and <i>make sure the regular GzipCodec is NOT used</i>.
 * This can be done by changing the <b>io.compression.codecs</b> property to
 * something like this:<br>
 * <i>org.apache.hadoop.io.compress.DefaultCodec,
 * nl.basjes.hadoop.io.compress.SplittableGzipCodec,
 * org.apache.hadoop.io.compress.BZip2Codec</i><br>
 * </li>
 * <li>Set the split size to something that works in your situation. This can be
 * done by setting the appropriate values for
 * <b>mapreduce.input.fileinputformat.split.minsize</b> and/or
 * <b>mapreduce.input.fileinputformat.split.maxsize</b>.</li>
 * </ol>
 * <b>Tuning for optimal performance and scalability.</b><br>
 * The overall advise is to <i>EXPERIMENT</i> with the settings and <i>do
 * benchmarks</i>.<br>
 * Remember that:
 * <ul>
 * <li>Being able to split the input has a positive effect scalability IFF there
 * is room to scale out to.</li>
 * <li>This codec is only useful if there are less Gzipped input file(s) than
 * available map task slots (i.e. some slots are idle during the input/map
 * phase).</li>
 * <li>There is a way of limiting the IO impact. Note that in the above example
 * the 4th task will read and decompress the ENTIRE input file.</li>
 * <li>Splitting increases the load on (all kinds of) system resources: CPU and
 * HDFS/Network. The additional load on the system resources has a negative
 * effect on the scalability. Splitting a file into 1000 splits will really
 * hammer the datanodes storing the first block of the file 1000 times.</li>
 * <li>More splits also affect the number of reduce tasks that follow.</li>
 * <li>If you create more splits than you have map task slots you will certainly
 * have a suboptimal setting and you should increase the split size to reduce
 * the number of splits.
 * </ul>
 *
 * A possible optimum:
 * <ol>
 * <li>Upload the input files into HDFS with a blocksize that is equal (or a few
 * bytes bigger) than the file size.<br>
 * <i>hadoop fs -Ddfs.block.size=1234567890 -put access.log.gz /logs</i><br>
 * This has the effect that all nodes that have "a piece of the file" always
 * have "the entire file". This ensures that no network IO is needed for a
 * single node to read the file IFF it has it locally available.</li>
 * <li>The replication of the HDFS determines on how many nodes the input file
 * is present. So to avoid needless network traffic the number of splits must be
 * limited to AT MOST the replication factor of the underlying HDFS.</li>
 * <li>Try to make sure that all splits of an input file are roughly the same
 * size. Don't be surprised if the optimal setting for the split size turns out
 * to be 500MiB or even 1GiB.</li>
 * </ol>
 *
 * <br>
 * <b>Alternative approaches</b><br>
 * Always remember that there are alternative approaches:<br>
 * <ol>
 * <li>Decompress the original gzipped file, split it into pieces and recompress
 * the pieces before offering them to Hadoop.<br>
 * For example: http://stackoverflow.com/questions/3960651</li>
 * <li>Decompress the original gzipped file and compress using a different
 * splittable codec.<br>
 * For example {@link org.apache.hadoop.io.compress.BZip2Codec} or not
 * compressing at all</li>
 * </ol>
 * <hr>
 * <b>Implementation notes</b><br>
 * <br>
 * There were <b>two major hurdles</b> that needed to be solved to make this
 * work:
 * <ol>
 * <li><b>The reported position depends on the read blocksize.</b><br>
 * If you read information in "records" the getBytesRead() will return a value
 * that jumps incrementally. Only <i>after</i> a new disk block has been read
 * will the getBytesRead return a new value. "Read" means: read from disk an
 * loaded into the decompressor but does NOT yet mean that the uncompressed
 * information was read.<br>
 * The solution employed is that when we get close to the end of the split we
 * switch to a crawling mode. This simply means that the disk reads are reduced
 * to 1 byte, making the position reporting also 1 byte accurate.<br>
 * This was implemented in the {@link ThrottleableDecompressorStream}.</li>
 *
 * <li><b>The input is compressed.</b><br>
 * If you read 1 byte (uncompressed) you do not always get an increase in the
 * reported getBytesRead(). This happens because the value reported by
 * getBytesRead is all about the filesize on disk (= compressed) and compressed
 * files have less bytes than the uncompressed data. This makes it impossible to
 * make two splits meet accurately.<br>
 * The solution is based around the concept that we try to report the position
 * as accurately as possible but when we get really close to the end we stop
 * reporting the truth and we start lying about the position.<br>
 * The lie we use to cross the split boundry is that 1 uncompressed byte read is
 * reported as 1 compressed byte increase in position. This was implemented
 * using a simple state machine with 3 different states on what position is
 * reported through the getPos(). The state is essentially selected on the
 * distance to the end.<br>
 *
 * These states are:
 * <ol>
 * <li><b>REPORT</b><br>
 * Normally read the bytes and report the actual disk position in the getPos().
 * </li>
 * <li><b>HOLD</b><br>
 * When very close to the end we no longer change the reported file position for
 * a while.</li>
 * <li><b>SLOPE</b><br>
 * When we are at the end: start reporting 1 byte increase from the getPos for
 * every uncompressed byte that was read from the stream.</li>
 * </ol>
 * The overall effect is that the position reporting near the end of the split
 * no longer represents the actual position and this makes the position usable
 * for reliably splitting the input stream.<br>
 * The actual point where the file is split is shifted a bit to the back of the
 * file (we're talking bytes, not even KiB) where this shift actually depends on
 * the compression levels of the data in the stream. If we start too early the
 * split may happen a byte too early and in the end the last split may lose the
 * last record(s). So that's why we hold for a while and only start the slope at
 * the moment we are certain we are beyond the indicated "end".<br>
 * To ensure the split starts at exactly the same spot as the previous split
 * would end: we find the start of a split by running over the "part that must
 * be discarded" as-if it is a split.
 * </ol>
 */

public class SplittableGzipCodec extends GzipCodec implements
    SplittableCompressionCodec {

    private static final Logger LOG =
            LoggerFactory.getLogger(SplittableGzipCodec.class);

  private static final int DEFAULT_FILE_BUFFER_SIZE = 4 * 1024; // 4 KiB

  public SplittableGzipCodec() {
    super();
    LOG.info("Creating instance of SplittableGzipCodec");
  }

  public SplitCompressionInputStream createInputStream(
      final InputStream seekableIn, final Decompressor decompressor,
      final long start, final long end,
      final READ_MODE readMode) // Ignored by this codec
    throws IOException {
    LOG.info("Creating SplittableGzipInputStream (range = [{},{}])", start, end );
    return new SplittableGzipInputStream(createInputStream(seekableIn,
        decompressor), start, end, getConf().getInt("io.file.buffer.size",
          DEFAULT_FILE_BUFFER_SIZE));
  }

  // -------------------------------------------

  @Override
  public CompressionInputStream createInputStream(final InputStream in,
      final Decompressor decompressor) throws IOException {
    return new ThrottleableDecompressorStream(in,
        (decompressor == null) ? createDecompressor() : decompressor,
        getConf().getInt("io.file.buffer.size", DEFAULT_FILE_BUFFER_SIZE));
  }

  // ==========================================

  private static final class SplittableGzipInputStream extends
      SplitCompressionInputStream {

    // We start crawling when within 110% of the blocksize from the split.
    private static final float CRAWL_FACTOR = 1.1F;

    // Just to be sure we always crawl the last part a minimal crawling
    // distance is defined here... 128 bytes works fine.
    private static final int MINIMAL_CRAWL_DISTANCE = 128;

    // At what distance from the target do we HOLD the position reporting.
    // 128 bytes works fine (same as minimal crawl distance).
    private static final int POSITION_HOLD_DISTANCE = 128;

    // When setting log4j into TRACE mode we will report massive amounts
    // of info when this many bytes near the relevant areas.
    private static final int TRACE_REPORTING_DISTANCE = 64;

    private final ThrottleableDecompressorStream in;
    private final int crawlDistance;
    private final int bufferSize;

    // -------------------------------------------

    public SplittableGzipInputStream(final CompressionInputStream inputStream,
        final long start, final long end, final int inputStreamBufferSize)
      throws IOException {
      super(inputStream, start, end);

      bufferSize = inputStreamBufferSize;

      if (getAdjustedStart() > 0) { // If the entire file is really small (like 1000 bytes) we want to continue anyway.
        if (getAdjustedEnd() - getAdjustedStart() < bufferSize) {
          throw new IllegalArgumentException("The provided InputSplit " +
                  "(" + getAdjustedStart() + ";" + getAdjustedEnd() + "] " +
                  "is " + (getAdjustedEnd() - getAdjustedStart()) + " bytes which is too small. " +
                  "(Minimum is " + bufferSize + ")");
        }
      }

      // We MUST have the option of slowing down the reading of data.
      // This check will fail if someone creates a subclass that breaks this.
      if (inputStream instanceof ThrottleableDecompressorStream) {
        this.in = (ThrottleableDecompressorStream) inputStream;
      } else {
        this.in = null; // Permanently cripple this instance ('in' is final) .
        throw new IOException("The SplittableGzipCodec relies on"
            + " functionality in the ThrottleableDecompressorStream class.");
      }

      // When this close to the end of the split: crawl (read at most 1 byte
      // at a time) to avoid overshooting the end.
      // This calculates the distance at which we should switch to crawling.
      // Fact is that if the previous buffer is 1 byte further than this value
      // the end of the next block (+ 1 byte) will be the real point where we
      // will start the crawl. --> either 10% of the bufferSize or the Minimal
      // crawl distance value.
      this.crawlDistance = Math.max(Math.round(CRAWL_FACTOR * bufferSize),
          bufferSize + MINIMAL_CRAWL_DISTANCE);

      // Now we read the stream until we are at the start of this split.

      if (start == 0) {
        return; // That was quick; We're already where we want to be.
      }

      // Set the range we want to run over quickly.
      setStart(0);
      setEnd(start);

      // The target buffer to dump the discarded info to.
      final byte[] skippedBytes = new byte[bufferSize];

      LOG.debug("SKIPPING to position :{}", start);
      while (getPos() < start) {
        // This reads the input and decompresses the data.
        if (-1 == read(skippedBytes, 0, bufferSize)) {
          // An EOF while seeking for the START of the split !?!?
          throw new EOFException("Unexpected end of input stream when"
              + " seeking for the start of the split in"
              + " SplittableGzipCodec:"
              + " start=" + start + " adjustedStart=" + start + " position="
              + getPos());
        }
      }

      LOG.debug("ARRIVED at target location({}): {}", start, getPos());

      // Now we put the real split range values back.
      setStart(start);
      setEnd(end);

      // Set the reporting back to normal
      posState = POS_STATE.REPORT;
    }

    // -------------------------------------------

    /**
     * Position reporting states.
     */
    enum POS_STATE {
      REPORT, HOLD, SLOPE
    }

    private POS_STATE posState = POS_STATE.REPORT;

    /**
     * What do we call this state?
     *
     * @return String with state name useful for logging and debugging.
     */
    private String getStateName() {
      switch (posState) {
      case REPORT:
        return "REPORT";
      case HOLD:
        return "HOLD";
      case SLOPE:
        return "SLOPE";
      default:
        return "ERROR";
      }
    }

    // The reported position used in the HOLD and SLOPE states.
    private long reportedPos = 0;

    @Override
    public long getPos() {
      if (posState == POS_STATE.REPORT) {
        return getRealPos();
      }
      return reportedPos;
    }

    /**
     * The getPos position of the underlying input stream.
     *
     * @return number of bytes that have been read from the compressed input.
     */
    private long getRealPos() {
      return in.getBytesRead();
    }

    // -------------------------------------------

    @Override
    public int read(final byte[] b, final int off, final int len)
      throws IOException {
      final long currentRealPos = getRealPos();
      int maxBytesToRead = Math.min(bufferSize, len);

      final long adjustedEnd = getAdjustedEnd();
      final long adjustedStart = getAdjustedStart();
      if (adjustedStart >= adjustedEnd) {
        return -1; // Nothing to read in this split at all --> indicate EOF
      }

      final long distanceToEnd = adjustedEnd - currentRealPos;

      if (distanceToEnd <= crawlDistance) {
        // We go to a crawl as soon as we are close to the end (or over it).
        maxBytesToRead = 1;

        // We're getting close
        switch (posState) {
        case REPORT:
          // If we are within 128 bytes of the end we freeze the current value.
          if (distanceToEnd <= POSITION_HOLD_DISTANCE) {
            posState = POS_STATE.HOLD;
            reportedPos = currentRealPos;
            LOG.trace("STATE REPORT --> HOLD @ {}", currentRealPos);
          }
          break;

        case HOLD:
          // When we are ON/AFTER the real "end" then we start the slope.
          // If we start too early the last split may lose the last record(s).
          if (distanceToEnd <= 0) {
            posState = POS_STATE.SLOPE;
            LOG.trace("STATE HOLD --> SLOPE @ {}", currentRealPos);
          }
          break;

        case SLOPE:
          // We are reading 1 byte at a time and reporting 1 byte at a time.
          ++reportedPos;
          break;

        default:
          break;
        }

      } else {
        // At a distance we always do normal reporting
        // Set the state explicitly: the "end" value can change.
        posState = POS_STATE.REPORT;
      }

      // Debugging facility
      if (LOG.isTraceEnabled()) {
        // When tracing do the first few bytes at crawl speed too.
        final long distanceFromStart = currentRealPos - adjustedStart;
        if (distanceFromStart <= TRACE_REPORTING_DISTANCE) {
          maxBytesToRead = 1;
        }
      }

      // Set the input read step to tune the disk reads to the wanted speed.
      in.setReadStep(maxBytesToRead);

      // Actually read the information.
      final int bytesRead = in.read(b, off, maxBytesToRead);

      // Debugging facility
      if (LOG.isTraceEnabled()) {
        if (bytesRead == -1) {
          LOG.trace("End-of-File");
        } else {
          // Report massive info on the LAST 64 bytes of the split
          if (getPos() >= getAdjustedEnd() - TRACE_REPORTING_DISTANCE
              && bytesRead < 10) {
            final String bytes = new String(b).substring(0, bytesRead);
            LOG.trace("READ TAIL {} bytes ({} pos = {}/{}): ##{}## HEX:##{}##",
                      bytesRead, getStateName(), getPos(), getRealPos(),
                      bytes, new String(Hex.encodeHex(bytes.getBytes())));
          }

          // Report massive info on the FIRST 64 bytes of the split
          if (getPos() <= getAdjustedStart() + TRACE_REPORTING_DISTANCE
              && bytesRead < 10) {
            final String bytes = new String(b).substring(0, bytesRead);
            LOG.trace("READ HEAD {} bytes ({} pos = {}/{}): ##{}## HEX:##{}##",
                      bytesRead, getStateName(), getPos(), getRealPos(),
                      bytes, new String(Hex.encodeHex(bytes.getBytes())));
          }
        }
      }

      return bytesRead;
    }

    // -------------------------------------------

    @Override
    public void resetState() throws IOException {
      in.resetState();
    }

    // -------------------------------------------

    @Override
    public int read() throws IOException {
      return in.read();
    }

    // -------------------------------------------
  }

  // ===================================================

}
