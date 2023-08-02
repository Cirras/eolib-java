package dev.cirras.data;

import java.nio.charset.Charset;

/**
 * A class for reading EO data from a sequence of bytes.
 *
 * <p>{@code EoReader} features a chunked reading mode, which is important for accurate emulation of
 * the official game client.
 *
 * @see <a href="https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md">Chunked
 *     Reading</a>
 */
public final class EoReader {
  private final byte[] data;
  private final int offset;
  private final int limit;
  private int position;
  private boolean chunkedReadingMode;
  private int chunkStart;
  private int nextBreak;

  /**
   * Creates a new {@code EoReader} instance for the specified data.
   *
   * @param data the byte array containing the input data
   */
  public EoReader(byte[] data) {
    this(data, 0, data.length);
  }

  private EoReader(byte[] data, int offset, int limit) {
    this.data = data;
    this.offset = offset;
    this.limit = limit;
    this.position = 0;
    this.chunkedReadingMode = false;
    this.chunkStart = 0;
    this.nextBreak = -1;
  }

  /**
   * Creates a new {@code EoReader} whose input data is a shared subsequence of this reader's data.
   *
   * <p>The input data of the new reader will start at this reader's current position and contain
   * all remaining data. The two reader's position and chunked reading mode will be independent.
   *
   * <p>The new reader's position will be zero, and its chunked reading mode will be false.
   *
   * @return the new reader
   */
  public EoReader slice() {
    return slice(position);
  }

  /**
   * Creates a new {@code EoReader} whose input data is a shared subsequence of this reader's data.
   *
   * <p>The input data of the new reader will start at position {@code index} in this reader and
   * contain all remaining data. The two reader's position and chunked reading mode will be
   * independent.
   *
   * <p>The new reader's position will be zero, and its chunked reading mode will be false.
   *
   * @param index the position in this reader at which the data of the new reader will start; must
   *     be non-negative.
   * @throws IndexOutOfBoundsException if {@code index} is negative. <br>
   *     This exception will <b>not</b> be thrown if {@code index} is greater than the size of the
   *     input data. Consistent with the existing over-read behaviors, an empty reader will be
   *     returned.
   * @return the new reader
   */
  public EoReader slice(int index) {
    return slice(index, Math.max(0, limit - index));
  }

  /**
   * Creates a new {@code EoReader} whose input data is a shared subsequence of this reader's data.
   *
   * <p>The input data of the new reader will start at position {@code index} in this reader and
   * contain all remaining data. The two reader's position and chunked reading mode will be
   * independent.
   *
   * <p>The new reader's position will be zero, and its chunked reading mode will be false.
   *
   * @param index the position in this reader at which the data of the new reader will start; must
   *     be non-negative.
   * @param length the length of the shared subsequence of data to supply to the new reader; must be
   *     non-negative.
   * @throws IndexOutOfBoundsException if {@code index} or {@code length} is negative. <br>
   *     This exception will <b>not</b> be thrown if {@code index + length} is greater than the size
   *     of the input data. Consistent with the existing over-read behaviors, the new reader will be
   *     supplied a shared subsequence of all remaining data starting from {@code index}.
   * @return the new reader
   */
  public EoReader slice(int index, int length) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("negative index: " + index);
    }

    if (length < 0) {
      throw new IndexOutOfBoundsException("negative length: " + length);
    }

    int sliceOffset = Math.max(0, Math.min(limit, index));
    int sliceLimit = Math.min(limit - sliceOffset, length);

    return new EoReader(data, sliceOffset, sliceLimit);
  }

  /**
   * Reads a raw byte from the input data.
   *
   * @return a raw byte
   */
  public int getByte() {
    return Byte.toUnsignedInt(readByte());
  }

  /**
   * Reads an array of raw bytes from the input data.
   *
   * @return an array of raw bytes
   */
  public byte[] getBytes(int length) {
    return readBytes(length);
  }

  /**
   * Reads an encoded 1-byte integer from the input data.
   *
   * @return a decoded 1-byte integer
   */
  public int getChar() {
    return NumberEncodingUtils.decodeNumber(readBytes(1));
  }

  /**
   * Reads an encoded 2-byte integer from the input data.
   *
   * @return a decoded 2-byte integer
   */
  public int getShort() {
    return NumberEncodingUtils.decodeNumber(readBytes(2));
  }

  /**
   * Reads an encoded 3-byte integer from the input data.
   *
   * @return a decoded 3-byte integer
   */
  public int getThree() {
    return NumberEncodingUtils.decodeNumber(readBytes(3));
  }

  /**
   * Reads an encoded 4-byte integer from the input data.
   *
   * @return a decoded 4-byte integer
   */
  public int getInt() {
    return NumberEncodingUtils.decodeNumber(readBytes(4));
  }

  /**
   * Reads a string from the input data.
   *
   * @return a string
   */
  public String getString() {
    byte[] bytes = readBytes(getRemaining());
    return new String(bytes, Charset.forName("windows-1252"));
  }

  /**
   * Reads a string with a fixed length from the input data.
   *
   * @param length the length of the string
   * @return a decoded string
   * @throws IllegalArgumentException if the length is negative
   */
  public String getFixedString(int length) {
    return getFixedString(length, false);
  }

  /**
   * Reads a string with a fixed length from the input data.
   *
   * @param length the length of the string
   * @param padded true if the string is padded with trailing {@code 0xFF} bytes
   * @return a decoded string
   * @throws IllegalArgumentException if the length is negative
   */
  public String getFixedString(int length, boolean padded) {
    if (length < 0) {
      throw new IllegalArgumentException("Negative length");
    }
    byte[] bytes = readBytes(length);
    if (padded) {
      bytes = removePadding(bytes);
    }
    return new String(bytes, Charset.forName("windows-1252"));
  }

  /**
   * Reads an encoded string from the input data.
   *
   * @return a decoded string
   */
  public String getEncodedString() {
    byte[] bytes = readBytes(getRemaining());
    StringEncodingUtils.decodeString(bytes);
    return new String(bytes, Charset.forName("windows-1252"));
  }

  /**
   * Reads an encoded string with a fixed length from the input data.
   *
   * @param length the length of the string
   * @return a decoded string
   * @throws IllegalArgumentException if the length is negative
   */
  public String getFixedEncodedString(int length) {
    return getFixedEncodedString(length, false);
  }

  /**
   * Reads an encoded string with a fixed length from the input data.
   *
   * @param length the length of the string
   * @param padded true if the string is padded with trailing {@code 0xFF} bytes
   * @return a decoded string
   * @throws IllegalArgumentException if the length is negative
   */
  public String getFixedEncodedString(int length, boolean padded) {
    if (length < 0) {
      throw new IllegalArgumentException("Negative length");
    }
    byte[] bytes = readBytes(length);
    StringEncodingUtils.decodeString(bytes);
    if (padded) {
      bytes = removePadding(bytes);
    }
    return new String(bytes, Charset.forName("windows-1252"));
  }

  /**
   * Sets the chunked reading mode for the reader.
   *
   * <p>In chunked reading mode:
   *
   * <ul>
   *   <li>the reader will treat {@code 0xFF} bytes as the end of the current chunk.
   *   <li>{@link EoReader#nextChunk} can be called to move to the next chunk.
   * </ul>
   *
   * @param chunkedReadingMode the new chunked reading mode
   */
  public void setChunkedReadingMode(boolean chunkedReadingMode) {
    this.chunkedReadingMode = chunkedReadingMode;
    if (nextBreak == -1) {
      nextBreak = findNextBreakIndex();
    }
  }

  /**
   * Gets the chunked reading mode for the reader.
   *
   * @return true if the reader is in chunked reading mode
   */
  public boolean getChunkedReadingMode() {
    return chunkedReadingMode;
  }

  /**
   * If chunked reading mode is enabled, gets the number of bytes remaining in the current chunk.
   * Otherwise, gets the total number of bytes remaining in the input data.
   *
   * @return the number of bytes remaining
   */
  public int getRemaining() {
    if (chunkedReadingMode) {
      return nextBreak - Math.min(position, nextBreak);
    } else {
      return limit - position;
    }
  }

  /**
   * Moves the reader position to the start of the next chunk in the input data.
   *
   * @throws IllegalStateException if not in chunked reading mode
   */
  public void nextChunk() {
    if (!chunkedReadingMode) {
      throw new IllegalStateException("Not in chunked reading mode.");
    }

    position = nextBreak;
    if (position < limit) {
      // Skip the break byte
      ++position;
    }

    chunkStart = position;
    nextBreak = findNextBreakIndex();
  }

  /**
   * Gets the current position in the input data.
   *
   * @return the current position in the input data
   */
  public int getPosition() {
    return position;
  }

  private byte readByte() {
    if (getRemaining() > 0) {
      return data[offset + position++];
    }
    return 0;
  }

  private byte[] readBytes(int length) {
    length = Math.min(length, getRemaining());

    byte[] result = new byte[length];
    System.arraycopy(data, offset + position, result, 0, length);

    position += length;

    return result;
  }

  private static byte[] removePadding(byte[] bytes) {
    for (int i = 0; i < bytes.length; ++i) {
      if (bytes[i] == (byte) 0xFF) {
        byte[] result = new byte[i];
        System.arraycopy(bytes, 0, result, 0, i);
        return result;
      }
    }
    return bytes;
  }

  private int findNextBreakIndex() {
    int i;
    for (i = chunkStart; i < limit; ++i) {
      if (data[offset + i] == (byte) 0xFF) {
        break;
      }
    }
    return i;
  }
}
