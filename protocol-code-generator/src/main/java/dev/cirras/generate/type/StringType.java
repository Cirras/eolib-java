package dev.cirras.generate.type;

import java.util.Optional;

public class StringType implements BasicType {
  private final String name;
  private final Length length;

  public StringType(String name, Length length) {
    this.name = name;
    this.length = length;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return length.asInteger();
  }

  @Override
  public boolean isBounded() {
    return length.isSpecified();
  }
}
