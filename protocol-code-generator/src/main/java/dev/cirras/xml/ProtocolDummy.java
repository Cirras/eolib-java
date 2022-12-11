package dev.cirras.xml;

import dev.cirras.util.TextContentUtils;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlMixed;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "dummy")
public class ProtocolDummy {
  @XmlAttribute(required = true)
  private String type;

  @XmlElement private ProtocolComment comment;

  @XmlMixed private final List<String> textContent = new ArrayList<>();

  private String value;

  public String getType() {
    return type;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }

  public String getValue() {
    return value;
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) throws ValidationException {
    this.value = TextContentUtils.getTextFromContent(textContent);
    if (this.value.isEmpty()) {
      throw new ValidationException("Hardcoded value is required for <dummy> element");
    }
  }
}
