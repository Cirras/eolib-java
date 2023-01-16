package dev.cirras.data;

/** A utility class for encoding and decoding EO numbers. */
public final class NumberEncodingUtils {
  /**
   * Encodes a number to a sequence of bytes.
   *
   * @param number the number to encode
   * @return the encoded sequence of bytes
   */
  public static byte[] encodeNumber(int number) {
    int value = number;
    int d = 0xFE;
    if (Integer.compareUnsigned(number, EoNumericLimits.THREE_MAX) >= 0) {
      d = Integer.divideUnsigned(value, EoNumericLimits.THREE_MAX) + 1;
      value = Integer.remainderUnsigned(value, EoNumericLimits.THREE_MAX);
    }

    int c = 0xFE;
    if (Integer.compareUnsigned(number, EoNumericLimits.SHORT_MAX) >= 0) {
      c = Integer.divideUnsigned(value, EoNumericLimits.SHORT_MAX) + 1;
      value = Integer.remainderUnsigned(value, EoNumericLimits.SHORT_MAX);
    }

    int b = 0xFE;
    if (Integer.compareUnsigned(number, EoNumericLimits.CHAR_MAX) >= 0) {
      b = Integer.divideUnsigned(value, EoNumericLimits.CHAR_MAX) + 1;
      value = Integer.remainderUnsigned(value, EoNumericLimits.CHAR_MAX);
    }

    int a = value + 1;

    return new byte[] {(byte) a, (byte) b, (byte) c, (byte) d};
  }

  /**
   * Decodes a number from a sequence of bytes.
   *
   * @param bytes the sequence of bytes to decode
   * @return the decoded number
   */
  public static int decodeNumber(byte[] bytes) {
    int result = 0;
    int length = Math.min(bytes.length, 4);

    for (int i = 0; i < length; ++i) {
      byte b = bytes[i];

      if (b == (byte) 0xFE) {
        break;
      }

      int value = Byte.toUnsignedInt(b) - 1;

      switch (i) {
        case 0:
          result += value;
          break;
        case 1:
          result += EoNumericLimits.CHAR_MAX * value;
          break;
        case 2:
          result += EoNumericLimits.SHORT_MAX * value;
          break;
        case 3:
          result += EoNumericLimits.THREE_MAX * value;
          break;
      }
    }

    return result;
  }

  private NumberEncodingUtils() {
    // utility class
  }
}
