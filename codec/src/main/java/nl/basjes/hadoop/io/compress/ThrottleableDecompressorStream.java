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

import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.io.compress.Decompressor;
import org.apache.hadoop.io.compress.DecompressorStream;

/**
 * A {@link DecompressorStream} that allows a dynamic reduction in block size
 * of the file read from disk. The purpose is to make determining the position
 * in the underlying compressed data stream more accurate.
 */
public class ThrottleableDecompressorStream extends DecompressorStream {

  private int readStep = 0;
  private long totalBytesRead = 0;

  // ------------------------------------------

  public ThrottleableDecompressorStream(final InputStream in,
      final Decompressor decompressor, final int bufferSize)
    throws IOException {
    super(in, decompressor, bufferSize);
    setReadStep(0); // Set readStep to the size of the buffer
  }

  // ------------------------------------------

  public ThrottleableDecompressorStream(final InputStream in,
      final Decompressor decompressor) throws IOException {
    this(in, decompressor, 512);
  }

  // ------------------------------------------

  /**
   * The compressed data is read from disk in blocks the size of the allocated
   * buffer. The accuracy of the reported position in the input stream greatly
   * depends on the size of the block read from disk. So if we need to have a
   * more accurate position we need to slow down this reading by setting a
   * lower readStep value.
   *
   * @param newReadStep
   *          If the new value is outside the valid range of the allocated
   *          buffer it is set to the size of the buffer: setReadStep(0)
   * @return the new value
   */
  public final int setReadStep(final int newReadStep) {
    if (buffer == null) {
      readStep = 0;
    } else {
      if (newReadStep <= 0 || newReadStep >= buffer.length) {
        readStep = buffer.length;
      } else {
        readStep = newReadStep;
      }
    }
    return readStep;
  }

  // ------------------------------------------

  /**
   * Reads the compressed input into the buffer.
   * This Override limits the number of bytes read to "readStep" and records
   * how many bytes have been read so far.
   * @throws IOException
   */
  @Override
  protected int getCompressedData() throws IOException {
    checkStream();

    // note that the _caller_ is now required to call setInput() or throw
    final int bytesRead = in.read(buffer, 0, readStep);
    totalBytesRead += bytesRead;
    return bytesRead;
  }

  // ------------------------------------------

  /**
   * How many bytes have been read from disk so far?
   */
  public long getBytesRead() {
    return totalBytesRead;
  }

  // ------------------------------------------

}

