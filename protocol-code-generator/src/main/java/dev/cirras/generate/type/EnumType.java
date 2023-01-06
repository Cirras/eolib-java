package dev.cirras.generate.type;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class EnumType implements CustomType, HasUnderlyingType {
  private final String name;
  private final String packageName;
  private final IntegerType underlyingType;
  private final List<EnumValue> values;

  public EnumType(
      String name, String packageName, IntegerType underlyingType, List<EnumValue> values) {
    this.name = name;
    this.packageName = packageName;
    this.underlyingType = underlyingType;
    this.values =
        Collections.unmodifiableList(
            values.stream()
                .sorted(Comparator.comparingInt(EnumValue::getOrdinalValue))
                .collect(Collectors.toList()));
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return underlyingType.getFixedSize();
  }

  @Override
  public boolean isBounded() {
    return underlyingType.isBounded();
  }

  @Override
  public String getPackageName() {
    return packageName;
  }

  @Override
  public IntegerType getUnderlyingType() {
    return underlyingType;
  }

  public List<EnumValue> getValues() {
    return values;
  }

  public Optional<EnumValue> getEnumValueByOrdinal(int ordinalValue) {
    return values.stream().filter(value -> value.getOrdinalValue() == ordinalValue).findFirst();
  }

  public Optional<EnumValue> getEnumValueByProtocolName(String protocolName) {
    return values.stream()
        .filter(value -> value.getProtocolName().equals(protocolName))
        .findFirst();
  }

  public static final class EnumValue {
    private final int ordinalValue;
    private final String protocolName;
    private final String javaName;

    public EnumValue(int ordinalValue, String protocolName, String javaName) {
      this.ordinalValue = ordinalValue;
      this.protocolName = protocolName;
      this.javaName = javaName;
    }

    public int getOrdinalValue() {
      return ordinalValue;
    }

    public String getProtocolName() {
      return protocolName;
    }

    public String getJavaName() {
      return javaName;
    }
  }
}
