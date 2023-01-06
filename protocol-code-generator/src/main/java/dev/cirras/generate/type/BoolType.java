package dev.cirras.generate.type;

import java.util.Optional;

public final class BoolType implements BasicType, HasUnderlyingType {
  private final IntegerType underlyingType;

  public BoolType(IntegerType underlyingType) {
    this.underlyingType = underlyingType;
  }

  @Override
  public String getName() {
    return "bool";
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return underlyingType.getFixedSize();
  }

  @Override
  public boolean isBounded() {
    return true;
  }

  @Override
  public IntegerType getUnderlyingType() {
    return underlyingType;
  }
}
