package dev.cirras.xml;

import jakarta.xml.bind.Unmarshaller;
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

  @XmlAttribute private boolean optional;

  @XmlAttribute private boolean delimited;

  @XmlAttribute(name = "trailing-delimiter")
  private Boolean trailingDelimiter;

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

  public boolean isOptional() {
    return optional;
  }

  public boolean isDelimited() {
    return delimited;
  }

  public boolean hasTrailingDelimiter() {
    return trailingDelimiter;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (trailingDelimiter == null) {
      trailingDelimiter = delimited;
    }
  }
}
