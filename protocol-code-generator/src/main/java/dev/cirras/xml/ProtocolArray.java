package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

@XmlRootElement(name = "array")
public class ProtocolArray {
  @XmlAttribute(required = true)
  private String name;

  @XmlAttribute(required = true)
  private String type;

  @XmlAttribute private String length;

  @XmlAttribute(name = "length-offset")
  private int lengthOffset;

  @XmlAttribute private boolean optional;

  @XmlAttribute private boolean delimited;

  @XmlElement private ProtocolComment comment;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getLength() {
    return length;
  }

  public int getLengthOffset() {
    return lengthOffset;
  }

  public boolean isOptional() {
    return optional;
  }

  public boolean isDelimited() {
    return delimited;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }
}
