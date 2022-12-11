package dev.cirras.util;

public final class NumberUtils {
  private NumberUtils() {
    // utils class
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static boolean isInteger(String string) {
    try {
      Integer.parseUnsignedInt(string);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static Integer tryParseInt(String input) {
    try {
      return Integer.parseUnsignedInt(input);
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
