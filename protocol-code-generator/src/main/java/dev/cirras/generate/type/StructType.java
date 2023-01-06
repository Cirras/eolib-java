package dev.cirras.generate.type;

import java.util.Optional;

public final class StructType implements CustomType {
  private final String name;
  private final Integer size;
  private final boolean bounded;
  private final String packageName;

  public StructType(String name, Integer size, boolean bounded, String packageName) {
    this.name = name;
    this.size = size;
    this.bounded = bounded;
    this.packageName = packageName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return Optional.ofNullable(size);
  }

  @Override
  public boolean isBounded() {
    return bounded;
  }

  @Override
  public String getPackageName() {
    return packageName;
  }
}
