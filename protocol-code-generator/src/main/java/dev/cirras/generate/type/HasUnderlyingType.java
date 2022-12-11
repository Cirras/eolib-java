package dev.cirras.generate.type;

public interface HasUnderlyingType extends Type {
  IntegerType getUnderlyingType();
}
