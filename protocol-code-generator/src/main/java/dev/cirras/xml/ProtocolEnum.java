package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "enum")
public final class ProtocolEnum {
  @XmlAttribute(required = true)
  private String name;

  @XmlAttribute(required = true)
  private String type;

  @XmlElement(name = "value", required = true)
  private final List<ProtocolValue> values = new ArrayList<>();

  @XmlElement private ProtocolComment comment;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public List<ProtocolValue> getValues() {
    return values;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }
}
