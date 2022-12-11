package dev.cirras.xml;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "case")
public class ProtocolCase {
  @XmlAttribute private String value;

  @XmlAttribute(name = "default")
  private boolean isDefault;

  @XmlElement private ProtocolComment comment;

  @XmlElements({
    @XmlElement(name = "field", type = ProtocolField.class),
    @XmlElement(name = "array", type = ProtocolArray.class),
    @XmlElement(name = "dummy", type = ProtocolDummy.class),
    @XmlElement(name = "switch", type = ProtocolSwitch.class),
    @XmlElement(name = "chunked", type = ProtocolChunked.class),
    @XmlElement(name = "break", type = ProtocolBreak.class)
  })
  private final List<Object> instructions = new ArrayList<>();

  public String getValue() {
    return value;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }

  public List<Object> getInstructions() {
    return instructions;
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (isDefault && value != null) {
      throw new ProtocolXmlError("Default case must not specify a value.");
    }

    if (!isDefault && value == null) {
      throw new ProtocolXmlError("Non-default case must specify a value.");
    }
  }
}
