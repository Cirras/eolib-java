package dev.cirras.util;

public final class StringUtils {
  private StringUtils() {
    // utils class
  }

  public static String capitalize(String string) {
    StringBuilder builder = new StringBuilder();
    if (!string.isEmpty()) {
      builder.append(Character.toUpperCase(string.charAt(0)));
      if (string.length() > 1) {
        builder.append(string.substring(1));
      }
    }
    return builder.toString();
  }
}
