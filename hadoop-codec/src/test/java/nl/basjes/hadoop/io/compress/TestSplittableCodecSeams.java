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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CodecPool;
import org.apache.hadoop.io.compress.Compressor;
import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.SplitCompressionInputStream;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.apache.hadoop.util.LineReader;
import org.apache.hadoop.util.ReflectionUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit tests to see if splitting codecs make their splits accurately.
 * This verifies if the seams between the splits are 100% accurate by comparing
 * all splits with a non-splitted read of the same input.
 */
public class TestSplittableCodecSeams {

  private static final Log LOG = LogFactory
      .getLog(TestSplittableCodecSeams.class);

  private final int BUFFER_SIZE = 4096;

  /**
   * Test with a series of files with several fixed sizes in trailing gibberish.
   * I.e. all lines in the test file are of equal length.
   */
  @Test
  public void testSplittableGzipCodecSeamsFixedLineLengths() {
    for (int length = 1; length <= 15; length += 3) {
      try {
        int splitSize = 10000;
        validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
            100000, length, 0, splitSize, 2*splitSize, 1);
      } catch (final IOException e) {
        fail("Exception was thrown: " + e.toString());
      }
    }
  }

  /**
   * Test with a series of files with several varying sizes in trailing
   * gibberish. I.e. there is great variety in the line lengths.
   */
  @Test
  public void testSplittableGzipCodecSeamsRandomLineLengths() {
    try {
      int splitSize = 25000;
      validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
          10000, 500, 250, splitSize, 2*splitSize);
    } catch (final IOException e) {
      fail("Exception was thrown: " + e.toString());
    }
  }

  /**
   * Test with a file with several varying split sizes.
   * This was created to push the system into a bad split size for the last split
   * NOTE: At 5011 there used to be a nasty edge case.
   */
  @Test (expected = IllegalArgumentException.class)
  public void testSplittableGzipCodecSeamsVariousSplitSizes() throws IOException {
    for (int splitSize = 5010; splitSize <= 5020; splitSize++) {
      validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
          1000, 500, 250, splitSize, 6000);
    }
  }

  /**
   * Test with a file with 1 character lines.
   */
  @Test
  public void testSplittableGzipCodecSeamsSingleCharLines() throws IOException {
    int splitSize = 4096;
    validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
        10000000, 1, 0, splitSize, 2*splitSize, 100);
  }

  /**
   * Test with a file with a bad split size.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSplittableGzipCodecSeamsBadSplitSize() throws IOException {
    int splitSize = 2000;
    validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
            1000, 500, 250, splitSize, 4096);
  }

  // ------------------------------------------

  /**
   * Test with a very small (below the minimum split size) file.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testSplittableGzipCodecSeamsVerySmallFile() throws IOException {
    int splitSize = 2000;
    try {
        validateSplitSeamsWithSyntheticFile(SplittableGzipCodec.class,
            1, 1, 0, splitSize, splitSize);
    } catch (Exception e) {
        throw e;
    }
  }

  // ------------------------------------------

  private void validateSplitSeamsWithSyntheticFile(
          final Class<? extends SplittableCompressionCodec> codecClass,
          final long records,
          final int  recordLength,
          final int  recordLengthJitter,
          final long splitSize,
          final long lastSplitSizeLimit) throws IOException {
    validateSplitSeamsWithSyntheticFile(codecClass, records, recordLength, recordLengthJitter, splitSize, lastSplitSizeLimit, 1);
    validateSplitSeamsWithSyntheticFile(codecClass, records, recordLength, recordLengthJitter, splitSize, lastSplitSizeLimit, 1000);
  }

  /**
    * This creates a synthetic file and then uses it to run the split seam check.
    */
  private void validateSplitSeamsWithSyntheticFile(
          final Class<? extends SplittableCompressionCodec> codecClass,
          final long records,
          final int  recordLength,
          final int  recordLengthJitter,
          final long splitSize,
          final long lastSplitSizeLimit,
          final int  randomizeEveryNChars) throws IOException {
    final Configuration conf = new Configuration();

    if (recordLength + recordLengthJitter > splitSize) {
      fail("Test definition error: Make the splits bigger than the records.");
    }

    if (splitSize > lastSplitSizeLimit) {
      fail("Test definition error: The last split must be the same or larger as the other splits.");
    }

    final FileSystem fs = FileSystem.getLocal(conf);
    final Path filename = writeSplitTestFile(conf, codecClass, records,
            recordLength, recordLengthJitter, randomizeEveryNChars);

    LOG.info("Input is SYNTHETIC: "
            + "records=" + records + ", "
            + "recordLength=" + recordLength
            + (recordLengthJitter == 0 ? "" : "+random[0;" + recordLengthJitter + "]."));

    validateSplitSeams(conf, fs, filename, codecClass, splitSize, records, lastSplitSizeLimit);

    fs.delete(filename, true);
  }

  // ------------------------------------------

  /**
   * This test checks if reading the file in a splitted way results
   * in the same lines as reading the file as a single 'split'.
   */
  private void validateSplitSeams(final Configuration conf,
                                  final FileSystem fs, final Path filename,
                                  final Class<? extends SplittableCompressionCodec> codecClass,
                                  final long splitSize,
                                  final long recordsInFile,
                                  final long lastSplitSizeLimit) throws IOException {
    // To make the test predictable
    conf.setInt("io.file.buffer.size", BUFFER_SIZE);

    final FileStatus infile = fs.getFileStatus(filename);
    final long inputLength = infile.getLen();

    if (inputLength > Integer.MAX_VALUE) {
      fail("Bad test file length.");
    }

    LOG.info("Input is " + inputLength + " bytes. "
            +"making a split every " + splitSize + " bytes.");

    final SplittableCompressionCodec codec
      = ReflectionUtils.newInstance(codecClass, conf);

    /*
     * The validation is done as follows:
     * 1) We open the entire file as a single split as the reference
     * 2) We create a sequence of splits and validate each line with the
     *    reference split.
     * The lines from these two must match 100%.
     */

    final Text refLine = new Text();
    final Decompressor refDcmp = CodecPool.getDecompressor(codec);
    assertNotNull("Unable to load the decompressor for codec \"" + codec.getClass().getName() + "\"", refDcmp);

    final SplitCompressionInputStream refStream = codec
      .createInputStream(fs.open(infile.getPath()), refDcmp, 0, inputLength,
        SplittableCompressionCodec.READ_MODE.BYBLOCK);
    final LineReader refReader = new LineReader(refStream, conf);

    final Text line = new Text();
    final Decompressor dcmp = CodecPool.getDecompressor(codec);
    assertNotNull("Unable to load the decompressor for codec \"" + codec.getClass().getName() + "\"", refDcmp);

    try {
      long start = 0;
      long end = splitSize;
      int splitCount = 0;
      long refLineNumber = 0;
      long splitLineNumber;

      do {
        splitLineNumber = 0;
        ++splitCount;
        LOG.debug("-------------------------------------------------------");
        dcmp.reset(); // Reset the Decompressor for reuse with the new stream

        final SplitCompressionInputStream splitStream = codec
            .createInputStream(fs.open(infile.getPath()), dcmp, start, end,
                SplittableCompressionCodec.READ_MODE.BYBLOCK);

        final long adjustedStart = splitStream.getAdjustedStart();
        final long adjustedEnd = splitStream.getAdjustedEnd();

        if (LOG.isDebugEnabled()) {
          LOG.debug("Doing split " + splitCount
                  + " on range " + " (" + start + "-" + end + ")"
                  + " adjusted to (" + adjustedStart + "-" + adjustedEnd + ")");
        }

        final LineReader lreader = new LineReader(splitStream, conf);

        if (start != 0) {
          // Not the first split so we discard the first (incomplete) line.
          int readChars = lreader.readLine(line);
          if (LOG.isTraceEnabled()) {
            LOG.trace("DISCARD LINE " + 0 + " in split " + splitCount
                + " pos=" + splitStream.getPos()
                + " length=" + readChars + ": \"" + line + "\"");
          }
        }

        // Now read until the end of this split
        while (nextKeyValue(splitStream, lreader, adjustedEnd, line)) {
          ++splitLineNumber;

          // Get the reference value
          if (!nextKeyValue(refStream, refReader, inputLength, refLine)) {
            LOG.error(String.format("S>%05d: %s", splitLineNumber, line));
            fail("Split goes beyond the end of the reference with line number " + splitLineNumber);
          }
          ++refLineNumber;

          if (LOG.isDebugEnabled() && refLineNumber > (recordsInFile-10)) {
            LOG.debug(String.format("R<%05d: %s", refLineNumber, refLine));
            LOG.debug(String.format("S>%05d: %s", splitLineNumber, line));
          }

          assertEquals("Line must be same in reference and in split at line "
              +refLineNumber, refLine, line);

          if (LOG.isTraceEnabled()) {
            LOG.trace("LINE " + splitLineNumber + " in split " + splitCount
                    + " (" + refLineNumber + ") pos=" + splitStream.getPos()
                    + " length=" + line.getLength() + ": \"" + line + "\"");
          }
        }

        // We just read through the entire split
        LOG.debug("Checked split " + splitCount + " (" + adjustedStart + "-" + adjustedEnd + ") "
                + "containing " + splitLineNumber + " lines.");

        if (end == inputLength) {
          LOG.info("====================> Finished the last split <====================");
          break; // We've reached the end of the last split
        }

        // Determine start and end for the next split
        start = end;

        if ((end + lastSplitSizeLimit) > inputLength) {
          end = inputLength;
          LOG.info("====================> Starting the last split ("+start+" - "+end+") <====================");
        } else {
          end += splitSize;
          LOG.info("====================> Starting the next split ("+start+" - "+end+") <====================");
        }

      } while (end <= inputLength);

      if (nextKeyValue(refStream, refReader, inputLength, refLine)) {
        ++refLineNumber;
        LOG.error(String.format("R<%05d: %s", refLineNumber, refLine));
        fail("The reference is at least one line longer than the last split ( " +
                "splitSize="    + splitSize     + ", " +
                "inputLength= " + inputLength   + ", " +
                "split start="  + start         + ", " +
                "split end="    + end           + ", " +
                "line="         + refLineNumber + ")"  );
      }

      LOG.info("Verified " + refLineNumber + " lines in " + splitCount + " splits.");

      assertEquals("Wrong number of records read", recordsInFile, refLineNumber);
    } finally {
      CodecPool.returnDecompressor(dcmp);
      CodecPool.returnDecompressor(refDcmp);
    }
  }

  // ------------------------------------------

  /**
   * Mostly copied from LineRecordReader (MapReduce) to pull an example of
   * actual usage into this test.
   */
  public boolean nextKeyValue(final SplitCompressionInputStream in,
      final LineReader lr, final long end, Text value) throws IOException {
    final int maxLineLength = Integer.MAX_VALUE;
    if (value == null) {
      value = new Text();
    }
    int newSize = 0;
    // We always read one extra line, which lies outside the upper
    // split limit i.e. (end - 1)
    while (in.getPos() <= end) {
      newSize = lr.readLine(value, maxLineLength, maxLineLength);
      if (newSize == 0) {
        break;
      }
      if (newSize < maxLineLength) {
        break;
      }

      // line too long. try again
      LOG.info("Skipped line of size " + newSize + " at pos "
          + (in.getPos() - newSize));
    }
    if (newSize == 0) {
      value = null;
      return false;
    } else {
      return true;
    }
  }

  // ------------------------------------------

  /**
   * Write the specified number of records to file in test dir using codec.
   * Records are simply lines random ASCII
   */
  private static Path writeSplitTestFile(final Configuration conf,
      final Class<? extends SplittableCompressionCodec> codecClass,
      final long records, final int recordLength,
      final int trailingSizeJitter,
      final int randomizeEveryNChars) throws IOException {

    RAND.setSeed(1); // Make the tests better reproducable

    final FileSystem fs = FileSystem.getLocal(conf);
    final SplittableCompressionCodec codec = ReflectionUtils.newInstance(
        codecClass, conf);

    final Path wd = new Path(new Path(System.getProperty("test.build.data",
        "/tmp")).makeQualified(fs.getUri(), fs.getWorkingDirectory()), codec
        .getClass().getSimpleName());

    final Path file = new Path(wd, "test-" + records + "-" + recordLength + "-"
        + trailingSizeJitter + codec.getDefaultExtension());
    DataOutputStream out = null;
    final Compressor cmp = CodecPool.getCompressor(codec);
    try {
      out = new DataOutputStream(codec.createOutputStream(
          fs.create(file, true), cmp));

      for (long seq = 1; seq <= records; ++seq) {
        final String line = randomGibberish(recordLength
            + (trailingSizeJitter > 0 ? RAND
                .nextInt(trailingSizeJitter) : 0), randomizeEveryNChars) + "\n";
        // There must be a simpler way to output ACSII instead of 2 byte UNICODE
        out.writeBytes(new String(line.getBytes("UTF-8"), "US-ASCII"));
      }
    } finally {
      IOUtils.cleanup(LOG, out);
      CodecPool.returnCompressor(cmp);
    }
    return file;
  }

  // ----------------------------------------------

  // Fixed the seed to make the test results reproducible
  private static final Random RAND = new Random(1);
  private static final char[] LETTERS = {
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    '!', '@', '#', '$', '%', '^', '&', '*', '(', ')'
  };

  private static long charsWritten = 0;
    private static char nextChar = 'a';
  private static String randomGibberish(final int length, final int randomizeEveryNChars) {
    if (length == 0) {
      return "";
    }
    final StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      if (charsWritten % randomizeEveryNChars == 0) {
          nextChar = LETTERS[RAND.nextInt(LETTERS.length)];
      }
      charsWritten++;
      sb.append(nextChar);
    }
    return sb.toString();
  }
}
