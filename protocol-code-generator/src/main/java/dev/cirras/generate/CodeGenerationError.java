package dev.cirras.generate;

public class CodeGenerationError extends RuntimeException {
  CodeGenerationError(String message) {
    super(message);
  }
}
