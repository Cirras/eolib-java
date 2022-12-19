package dev.cirras.xml;

import dev.cirras.util.TextContentUtils;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "field")
public class ProtocolField {
  @XmlAttribute private String name;

  @XmlAttribute(required = true)
  private String type;

  @XmlAttribute private String length;

  @XmlAttribute private boolean padded;

  @XmlAttribute private boolean optional;

  @XmlElement private ProtocolComment comment;

  @XmlMixed private final List<String> textContent = new ArrayList<>();

  private String value;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getLength() {
    return length;
  }

  public boolean isPadded() {
    return padded;
  }

  public boolean isOptional() {
    return optional;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }

  public String getValue() {
    return value;
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    this.value = TextContentUtils.getTextFromContent(textContent);

    if (name == null) {
      if (value == null) {
        throw new ProtocolXmlError("Unnamed fields must specify a hardcoded field value.");
      }

      if (optional) {
        throw new ProtocolXmlError("Unnamed fields may not be optional.");
      }
    }

    if (length == null && padded) {
      throw new ProtocolXmlError("Padded fields must specify a length.");
    }
  }
}
