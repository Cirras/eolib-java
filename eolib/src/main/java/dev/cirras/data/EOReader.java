package dev.cirras.data;

import java.nio.charset.Charset;

public final class EOReader {
  private final byte[] data;
  private int position = 0;
  private boolean chunkedReadingMode = false;
  private int chunkStart = 0;
  private int nextBreak = -1;

  /**
   * Creates a new EOReader instance for the specified data.
   *
   * @param data the byte array containing the input data
   */
  public EOReader(byte[] data) {
    this.data = data;
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
   * @return a string
   */
  public String getFixedString(int length) {
    byte[] bytes = readBytes(length);
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
   * @return a decoded string
   */
  public String getFixedEncodedString(int length) {
    byte[] bytes = readBytes(length);
    StringEncodingUtils.decodeString(bytes);
    return new String(bytes, Charset.forName("windows-1252"));
  }

  /**
   * Sets the chunked reading mode for the reader.
   *
   * <p>In chunked reading mode:
   * <li>the reader will treat <code>0xFF</code> bytes as the end of the current chunk.
   * <li>{@link EOReader#nextChunk} can be called to move to the next chunk.
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
      return nextBreak - position;
    } else {
      return data.length - position;
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
    if (position < data.length) {
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
      return data[position++];
    }
    return 0;
  }

  private byte[] readBytes(int length) {
    length = Math.min(length, getRemaining());

    byte[] result = new byte[length];
    System.arraycopy(data, position, result, 0, length);

    position += length;

    return result;
  }

  private int findNextBreakIndex() {
    int i;
    for (i = chunkStart; i < data.length; ++i) {
      if (data[i] == (byte) 0xFF) {
        break;
      }
    }
    return i;
  }
}
