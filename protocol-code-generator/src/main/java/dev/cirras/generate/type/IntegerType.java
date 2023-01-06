package dev.cirras.generate.type;

import java.util.Optional;

public class IntegerType implements BasicType {
  private final String name;
  private final int size;

  public IntegerType(String name, int size) {
    this.name = name;
    this.size = size;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return Optional.of(size);
  }

  @Override
  public boolean isBounded() {
    return true;
  }
}
