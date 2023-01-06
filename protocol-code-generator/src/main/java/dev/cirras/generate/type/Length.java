package dev.cirras.generate.type;

import dev.cirras.util.NumberUtils;
import java.util.Optional;

public final class Length {
  private final String string;
  private final Integer integer;

  private Length(String lengthString) {
    string = lengthString;
    integer = NumberUtils.tryParseInt(lengthString);
  }

  public static Length fromString(String lengthString) {
    return new Length(lengthString);
  }

  public static Length unspecified() {
    return new Length(null);
  }

  public Optional<Integer> asInteger() {
    return Optional.ofNullable(integer);
  }

  public boolean isSpecified() {
    return string != null;
  }
}
