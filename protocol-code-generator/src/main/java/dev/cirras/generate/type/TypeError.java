package dev.cirras.generate.type;

public final class TypeError extends RuntimeException {
  TypeError(String message) {
    super(message);
  }
}
