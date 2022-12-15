package dev.cirras.protocol;

public final class SerializationError extends RuntimeException {
  public SerializationError(String message) {
    super(message);
  }
}
