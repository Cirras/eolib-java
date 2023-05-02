package dev.cirras.generate.type;

import java.util.Optional;

public class BlobType implements Type {
  @Override
  public String getName() {
    return "blob";
  }

  @Override
  public Optional<Integer> getFixedSize() {
    return Optional.empty();
  }

  @Override
  public boolean isBounded() {
    return false;
  }
}
