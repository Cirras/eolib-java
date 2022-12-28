package dev.cirras.encrypt;

/** A utility class for encrypting and decrypting EO data. */
public final class EncryptionUtils {
  private EncryptionUtils() {
    // utility class
  }

  /**
   * Interleaves a sequence of bytes. When encrypting EO data, bytes are "woven" into each other.
   * <br>
   * Used when encrypting packets and data files.
   *
   * <p>Example:
   *
   * <pre>
   *   {1, 2, 3, 4, 5} → {0, 5, 1, 4, 2, 3}
   * </pre>
   *
   * <p>This is an in-place operation.
   *
   * @param data the data to interleave
   */
  public static void interleave(byte[] data) {
    byte[] buffer = new byte[data.length];

    int i = 0;
    int ii = 0;

    for (; i < data.length; i += 2) {
      buffer[i] = data[ii++];
    }

    --i;

    if (data.length % 2 != 0) {
      i -= 2;
    }

    for (; i >= 0; i -= 2) {
      buffer[i] = data[ii++];
    }

    System.arraycopy(buffer, 0, data, 0, data.length);
  }

  /**
   * Deinterleaves a sequence of bytes. This is the reverse of {@code interleave}. <br>
   * Used when decrypting packets and data files.
   *
   * <p>Example:
   *
   * <pre>
   *   {1, 2, 3, 4, 5} → {0, 2, 4, 5, 3, 1}
   * </pre>
   *
   * <p>This is an in-place operation.
   *
   * @param data the data to deinterleave
   */
  public static void deinterleave(byte[] data) {
    byte[] buffer = new byte[data.length];

    int i = 0;
    int ii = 0;

    for (; i < data.length; i += 2) {
      buffer[ii++] = data[i];
    }

    --i;

    if (data.length % 2 != 0) {
      i -= 2;
    }

    for (; i >= 0; i -= 2) {
      buffer[ii++] = data[i];
    }

    System.arraycopy(buffer, 0, data, 0, data.length);
  }

  /**
   * Flips the most significant bits of each byte in a sequence of bytes. (Values {@code 0} and
   * {@code 128} are not flipped.) <br>
   * Used when encrypting and decrypting packets.
   *
   * <p>Example:
   *
   * <pre>
   *   {0, 1, 127, 128, 129, 254, 255} → {0, 129, 255, 128, 1, 126, 127}
   * </pre>
   *
   * <p>This is an in-place operation.
   *
   * @param data the data to flip most significant bits on
   */
  public static void flipMSB(byte[] data) {
    for (int i = 0; i < data.length; ++i) {
      if ((data[i] & 0x7F) != 0) {
        data[i] = (byte) (data[i] ^ 0x80);
      }
    }
  }

  /**
   * Swaps the order of contiguous bytes in a sequence of bytes that are divisible by a given
   * multiple value. <br>
   * Used when encrypting and decrypting packets and data files.
   *
   * <p>Example:
   *
   * <pre>
   *   multiple = 3
   *   {10, 21, 27} → {10, 27, 21}
   * </pre>
   *
   * <p>This is an in-place operation.
   *
   * @param data the data to swap bytes in
   * @param multiple the multiple value
   */
  public static void swapMultiples(byte[] data, int multiple) {
    if (multiple < 0) {
      throw new IllegalArgumentException("multiple must be a positive number");
    }

    if (multiple == 0) {
      return;
    }

    int sequenceLength = 0;

    for (int i = 0; i <= data.length; ++i) {
      if (i != data.length && data[i] % multiple == 0) {
        ++sequenceLength;
      } else {
        if (sequenceLength > 1) {
          for (int ii = 0; ii < sequenceLength / 2; ++ii) {
            byte b = data[i - sequenceLength + ii];
            data[i - sequenceLength + ii] = data[i - ii - 1];
            data[i - ii - 1] = b;
          }
        }

        sequenceLength = 0;
      }
    }
  }
}
