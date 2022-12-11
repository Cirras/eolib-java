package dev.cirras.generate.type;

import java.util.Optional;

public final class StructType implements CustomType {
  private final String name;
  private final Integer size;
  private final String packageName;

  public StructType(String name, Integer size, String packageName) {
    this.name = name;
    this.size = size;
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
  public String getPackageName() {
    return packageName;
  }
}
