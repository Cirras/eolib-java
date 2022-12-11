package dev.cirras.util;

public final class NameUtils {
  private NameUtils() {
    // utils class
  }

  public static String pascalCaseToScreamingSnakeCase(String name) {
    StringBuilder builder = new StringBuilder();

    for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (i > 0
          && i + 1 < name.length()
          && Character.isUpperCase(c)
          && !Character.isUpperCase(name.charAt(i + 1))) {
        builder.append("_");
      }
      builder.append(Character.toUpperCase(c));
    }

    return builder.toString();
  }

  public static String snakeCaseToCamelCase(String name) {
    StringBuilder builder = new StringBuilder();
    boolean uppercaseNext = false;

    for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (c == '_') {
        uppercaseNext = builder.length() > 0;
        continue;
      }

      if (uppercaseNext) {
        c = Character.toUpperCase(c);
        uppercaseNext = false;
      } else {
        c = Character.toLowerCase(c);
      }

      builder.append(c);
    }

    return builder.toString();
  }

  public static String snakeCaseToPascalCase(String name) {
    return StringUtils.capitalize(snakeCaseToCamelCase(name));
  }
}
