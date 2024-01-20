package dev.cirras.data;

import java.nio.charset.Charset;

/** A class for writing EO data to a sequence of bytes. */
public final class EoWriter {
  private byte[] data = new byte[16];
  private int length = 0;
  private boolean stringSanitizationMode = false;

  /**
   * Adds a raw byte to the writer data.
   *
   * @param value the byte to add
   * @throws IllegalArgumentException if the value is above {@code 0xFF}.
   */
  public void addByte(int value) {
    checkNumberSize(value, 0xFF);
    if (length + 1 > data.length) {
      expand(2);
    }
    data[length++] = (byte) value;
  }

  /**
   * Adds an array of raw bytes to the writer data.
   *
   * @param bytes the array of bytes to add
   */
  public void addBytes(byte[] bytes) {
    addBytes(bytes, bytes.length);
  }

  /**
   * Adds an encoded 1-byte integer to the writer data.
   *
   * @param number the number to encode and add
   * @throws IllegalArgumentException if the value is not below {@link EoNumericLimits#CHAR_MAX}.
   */
  public void addChar(int number) {
    checkNumberSize(number, EoNumericLimits.CHAR_MAX - 1);
    byte[] bytes = NumberEncodingUtils.encodeNumber(number);
    addBytes(bytes, 1);
  }

  /**
   * Adds an encoded 2-byte integer to the writer data.
   *
   * @param number the number to encode and add
   * @throws IllegalArgumentException if the value is not below {@link EoNumericLimits#SHORT_MAX}.
   */
  public void addShort(int number) {
    checkNumberSize(number, EoNumericLimits.SHORT_MAX - 1);
    byte[] bytes = NumberEncodingUtils.encodeNumber(number);
    addBytes(bytes, 2);
  }

  /**
   * Adds an encoded 3-byte integer to the writer data.
   *
   * @param number the number to encode and add
   * @throws IllegalArgumentException if the value is not below {@link EoNumericLimits#THREE_MAX}.
   */
  public void addThree(int number) {
    checkNumberSize(number, EoNumericLimits.THREE_MAX - 1);
    byte[] bytes = NumberEncodingUtils.encodeNumber(number);
    addBytes(bytes, 3);
  }

  /**
   * Adds an encoded 4-byte integer to the writer data.
   *
   * @param number the number to encode and add
   * @throws IllegalArgumentException if the value is not below {@link EoNumericLimits#INT_MAX}.
   */
  public void addInt(int number) {
    checkNumberSize(number, EoNumericLimits.INT_MAX - 1);
    byte[] bytes = NumberEncodingUtils.encodeNumber(number);
    addBytes(bytes, 4);
  }

  /**
   * Adds a string to the writer data.
   *
   * @param string the string to be added
   */
  public void addString(String string) {
    byte[] bytes = string.getBytes(Charset.forName("windows-1252"));
    sanitizeString(bytes);
    addBytes(bytes);
  }

  /**
   * Adds a fixed-length string to the writer data.
   *
   * @param string the string to be added
   * @param length the expected length of the string
   * @throws IllegalArgumentException if the string does not have the expected length
   */
  public void addFixedString(String string, int length) {
    addFixedString(string, length, false);
  }

  /**
   * Adds a fixed-length string to the writer data.
   *
   * @param string the string to be added
   * @param length the expected length of the string
   * @param padded true if the string should be padded to the length with trailing {@code 0xFF}
   *     bytes.
   * @throws IllegalArgumentException if the string does not have the expected length
   */
  public void addFixedString(String string, int length, boolean padded) {
    checkStringLength(string, length, padded);
    byte[] bytes = string.getBytes(Charset.forName("windows-1252"));
    sanitizeString(bytes);
    if (padded) {
      bytes = addPadding(bytes, length);
    }
    addBytes(bytes);
  }

  /**
   * Adds an encoded string to the writer data.
   *
   * @param string the string to be encoded and added
   */
  public void addEncodedString(String string) {
    byte[] bytes = string.getBytes(Charset.forName("windows-1252"));
    sanitizeString(bytes);
    StringEncodingUtils.encodeString(bytes);
    addBytes(bytes);
  }

  /**
   * Adds a fixed-length encoded string to the writer data.
   *
   * @param string the string to be encoded and added
   * @param length the expected length of the string
   * @throws IllegalArgumentException if the string does not have the expected length
   */
  public void addFixedEncodedString(String string, int length) {
    addFixedEncodedString(string, length, false);
  }

  /**
   * Adds a fixed-length encoded string to the writer data.
   *
   * @param string the string to be encoded and added
   * @param length the expected length of the string
   * @param padded true if the string should be padded to the length with trailing {@code 0xFF}
   *     bytes
   * @throws IllegalArgumentException if the string does not have the expected length
   */
  public void addFixedEncodedString(String string, int length, boolean padded) {
    checkStringLength(string, length, padded);
    byte[] bytes = string.getBytes(Charset.forName("windows-1252"));
    sanitizeString(bytes);
    if (padded) {
      bytes = addPadding(bytes, length);
    }
    StringEncodingUtils.encodeString(bytes);
    addBytes(bytes);
  }

  /**
   * Sets the string sanitization mode for the writer.
   *
   * <p>With string sanitization enabled, the writer will switch {@code 0xFF} bytes in strings (ÿ)
   * to {@code 0x79} (y).
   *
   * @param stringSanitizationMode the new string sanitization mode
   * @see <a href="https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md#sanitization">
   *     Chunked Reading: Sanitization</a>
   */
  public void setStringSanitizationMode(boolean stringSanitizationMode) {
    this.stringSanitizationMode = stringSanitizationMode;
  }

  /**
   * Gets the string sanitization mode for the writer.
   *
   * @return true if string sanitization is enabled
   * @see <a href="https://github.com/Cirras/eo-protocol/blob/master/docs/chunks.md#sanitization">
   *     Chunked Reading: Sanitization</a>
   */
  public boolean getStringSanitizationMode() {
    return stringSanitizationMode;
  }

  /**
   * Gets the length of the writer data.
   *
   * @return the length of the writer data
   */
  public int getLength() {
    return length;
  }

  /**
   * Gets the writer data as a byte array.
   *
   * @return a copy of the writer data as a byte array
   */
  public byte[] toByteArray() {
    byte[] copy = new byte[length];
    System.arraycopy(data, 0, copy, 0, length);
    return copy;
  }

  private void addBytes(byte[] bytes, int bytesLength) {
    int expandFactor = 1;
    while (length + bytesLength > data.length * expandFactor) {
      expandFactor *= 2;
    }

    if (expandFactor > 1) {
      expand(expandFactor);
    }

    System.arraycopy(bytes, 0, data, length, bytesLength);
    length += bytesLength;
  }

  private void expand(int expandFactor) {
    byte[] expanded = new byte[data.length * expandFactor];
    System.arraycopy(data, 0, expanded, 0, length);
    data = expanded;
  }

  private void sanitizeString(byte[] bytes) {
    if (stringSanitizationMode) {
      for (int i = 0; i < bytes.length; ++i) {
        if (bytes[i] == (byte) 0xFF /* ÿ */) {
          bytes[i] = 0x79 /* y */;
        }
      }
    }
  }

  private static byte[] addPadding(byte[] bytes, int length) {
    if (bytes.length == length) {
      return bytes;
    }

    byte[] result = new byte[length];
    System.arraycopy(bytes, 0, result, 0, bytes.length);
    for (int i = bytes.length; i < length; ++i) {
      result[i] = (byte) 0xFF;
    }

    return result;
  }

  private static void checkNumberSize(int number, int max) {
    if (Integer.compareUnsigned(number, max) > 0) {
      throw new IllegalArgumentException(
          String.format("Value %d exceeds maximum of %d.", number, max));
    }
  }

  private static void checkStringLength(String string, int length, boolean padded) {
    if (padded) {
      if (length >= string.length()) {
        return;
      }
      throw new IllegalArgumentException(
          String.format("Padded string \"%s\" is too large for a length of %d.", string, length));
    }

    if (string.length() != length) {
      throw new IllegalArgumentException(
          String.format("String \"%s\" does not have expected length of %d.", string, length));
    }
  }
}
