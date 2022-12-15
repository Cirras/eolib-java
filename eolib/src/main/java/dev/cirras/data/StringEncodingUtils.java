package dev.cirras.data;

/** A utility class for encoding and decoding EO strings. */
public final class StringEncodingUtils {
  /**
   * Encodes a string by inverting the bytes and then reversing them.
   *
   * <p>This is an in-place operation.
   *
   * @param bytes the byte array to encode
   */
  public static void encodeString(byte[] bytes) {
    invertCharacters(bytes);
    reverseCharacters(bytes);
  }

  /**
   * Decodes a string by reversing the bytes and then inverting them.
   *
   * <p>This is an in-place operation.
   *
   * @param bytes the byte array to decode
   */
  public static void decodeString(byte[] bytes) {
    reverseCharacters(bytes);
    invertCharacters(bytes);
  }

  private static void invertCharacters(byte[] bytes) {
    boolean flippy = (bytes.length % 2 == 1);

    for (int i = 0; i < bytes.length; ++i) {
      byte c = bytes[i];
      int f = 0;

      if (flippy) {
        f = 0x2E;
        if (c >= 0x50) {
          f *= -1;
        }
      }

      if (c >= 0x22 && c <= 0x7E) {
        bytes[i] = (byte) (0x9F - c - f);
      }

      flippy = !flippy;
    }
  }

  private static void reverseCharacters(byte[] bytes) {
    for (int i = 0; i < bytes.length / 2; i++) {
      byte b = bytes[i];
      bytes[i] = bytes[bytes.length - i - 1];
      bytes[bytes.length - i - 1] = b;
    }
  }

  private StringEncodingUtils() {
    // utility class
  }
}
