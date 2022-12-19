package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

@XmlRootElement(name = "length")
public class ProtocolLength {
  @XmlAttribute(required = true)
  private String name;

  @XmlAttribute(required = true)
  private String type;

  @XmlAttribute(name = "offset")
  private int offset;

  @XmlAttribute private boolean optional;

  @XmlElement private ProtocolComment comment;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public int getOffset() {
    return offset;
  }

  public boolean isOptional() {
    return optional;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }
}
