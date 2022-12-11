package dev.cirras.generate.type;

import java.util.Optional;

public class StringType implements BasicType {
  private final String name;
  private final Integer length;

  public StringType(String name, Integer length) {
    this.name = name;
    this.length = length;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return Optional.ofNullable(length);
  }
}
