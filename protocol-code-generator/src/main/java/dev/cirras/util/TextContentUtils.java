package dev.cirras.util;

import dev.cirras.xml.ProtocolXmlError;
import java.util.List;

public final class TextContentUtils {
  private TextContentUtils() {
    // utils class
  }

  public static String getTextFromContent(List<String> content) {
    String result = null;

    for (String textContent : content) {
      String text = textContent.trim();
      if (text.isEmpty()) {
        continue;
      }

      if (result != null) {
        throw new ProtocolXmlError(String.format("Unexpected text content \"%s\"", text));
      }

      result = text;
    }

    return result;
  }
}
