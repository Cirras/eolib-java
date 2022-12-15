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
    if (Integer.compareUnsigned(number, EONumericLimits.THREE_MAX) >= 0) {
      d = Integer.divideUnsigned(value, EONumericLimits.THREE_MAX) + 1;
      value = Integer.remainderUnsigned(value, EONumericLimits.THREE_MAX);
    }

    int c = 0xFE;
    if (Integer.compareUnsigned(number, EONumericLimits.SHORT_MAX) >= 0) {
      c = Integer.divideUnsigned(value, EONumericLimits.SHORT_MAX) + 1;
      value = Integer.remainderUnsigned(value, EONumericLimits.SHORT_MAX);
    }

    int b = 0xFE;
    if (Integer.compareUnsigned(number, EONumericLimits.CHAR_MAX) >= 0) {
      b = Integer.divideUnsigned(value, EONumericLimits.CHAR_MAX) + 1;
      value = Integer.remainderUnsigned(value, EONumericLimits.CHAR_MAX);
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
          result += EONumericLimits.CHAR_MAX * value;
          break;
        case 2:
          result += EONumericLimits.SHORT_MAX * value;
          break;
        case 3:
          result += EONumericLimits.THREE_MAX * value;
          break;
      }
    }

    return result;
  }

  private NumberEncodingUtils() {
    // utility class
  }
}
