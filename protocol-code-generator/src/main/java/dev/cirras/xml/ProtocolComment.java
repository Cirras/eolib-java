package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.util.regex.Pattern;

@XmlRootElement(name = "comment")
public final class ProtocolComment {
  @XmlValue private String text;

  public String getText() {
    Pattern leadingWhitespace = Pattern.compile("^\\s+", Pattern.MULTILINE);
    return leadingWhitespace.matcher(text).replaceAll("");
  }
}
