package dev.cirras.data;

/**
 * Constants for the maximum values of the EO numeric types.
 *
 * <p>The largest valid value for each type is {@code TYPE_MAX - 1}.
 */
public final class EONumericLimits {
  /** The maximum value of an EO char (1-byte encoded integer type) */
  public static final int CHAR_MAX = 253;

  /** The maximum value of an EO short (2-byte encoded integer type) */
  public static final int SHORT_MAX = CHAR_MAX * CHAR_MAX;

  /** The maximum value of an EO three (3-byte encoded integer type) */
  public static final int THREE_MAX = CHAR_MAX * CHAR_MAX * CHAR_MAX;

  /**
   * The maximum value of an EO int (4-byte encoded integer type)
   *
   * <p>NOTE: This constant stores an unsigned value of 4097152081. The java {@code int} type is
   * signed, meaning this value overflows. You must use the {@code Integer} utility methods for
   * unsigned arithmetic and comparisons.
   */
  public static final int INT_MAX = (int) ((long) CHAR_MAX * CHAR_MAX * CHAR_MAX * CHAR_MAX);

  private EONumericLimits() {
    // constants class
  }
}
