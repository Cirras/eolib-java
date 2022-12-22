package dev.cirras.protocol;

/** This exception reports errors in serializing a protocol data structure. */
public final class SerializationError extends RuntimeException {
  /**
   * Constructs a {@code SerializationError} with the specified detail message.
   *
   * @param message the detail message
   */
  public SerializationError(String message) {
    super(message);
  }
}
