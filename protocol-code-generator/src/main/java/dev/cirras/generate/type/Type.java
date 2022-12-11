package dev.cirras.generate.type;

import java.util.Optional;

public interface Type {
  String getName();

  Optional<Integer> getFixedSize();
}
