package dev.cirras.xml;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import jakarta.xml.bind.helpers.ValidationEventImpl;
import java.lang.reflect.InvocationTargetException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProtocolValidationEventHandler extends DefaultValidationEventHandler {
  private static final Logger LOG = LogManager.getLogger(ProtocolValidationEventHandler.class);

  @Override
  public boolean handleEvent(ValidationEvent event) {
    Throwable linkedException = event.getLinkedException();
    if (event.getMessage() == null && linkedException instanceof InvocationTargetException) {
      Throwable targetException = linkedException.getCause();
      if (targetException != null) {
        event =
            new ValidationEventImpl(
                event.getSeverity(),
                targetException.getMessage(),
                event.getLocator(),
                event.getLinkedException());
      }
    }

    LOG.error("{} [line {}]", event.getMessage(), getLine(event));

    return false;
  }

  private int getLine(ValidationEvent event) {
    if (event.getLocator() != null) {
      return event.getLocator().getLineNumber();
    }
    return -1;
  }
}
