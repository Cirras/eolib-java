package dev.cirras.xml;

public class ProtocolXmlError extends RuntimeException {
  public ProtocolXmlError(String message) {
    super(message);
  }

  public ProtocolXmlError(String message, Throwable cause) {
    super(message, cause);
  }
}
