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

@XmlRootElement(name = "value")
public final class ProtocolValue {
  @XmlAttribute(required = true)
  private String name;

  @XmlAttribute(name = "default")
  private boolean isDefault;

  @XmlElement private ProtocolComment comment;

  @XmlMixed private final List<String> textContent = new ArrayList<>();

  private int value;

  public String getName() {
    return name;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public int getOrdinalValue() {
    return value;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    String text = TextContentUtils.getTextFromContent(textContent);

    if (text == null) {
      throw new ProtocolXmlError("Enum value is required");
    }

    try {
      this.value = Integer.parseInt(text);
    } catch (NumberFormatException e) {
      throw new ProtocolXmlError(String.format("Invalid enum value \"%s\"", text), e);
    }
  }
}
