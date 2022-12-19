package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@XmlRootElement(name = "packet")
public final class ProtocolPacket {
  @XmlAttribute(required = true)
  private String family;

  @XmlAttribute(required = true)
  private String action;

  @XmlElements({
    @XmlElement(name = "field", type = ProtocolField.class),
    @XmlElement(name = "array", type = ProtocolArray.class),
    @XmlElement(name = "length", type = ProtocolLength.class),
    @XmlElement(name = "dummy", type = ProtocolDummy.class),
    @XmlElement(name = "switch", type = ProtocolSwitch.class),
    @XmlElement(name = "chunked", type = ProtocolChunked.class),
    @XmlElement(name = "break", type = ProtocolBreak.class)
  })
  private final List<Object> instructions = new ArrayList<>();

  @XmlElement private ProtocolComment comment;

  public String getFamily() {
    return family;
  }

  public String getAction() {
    return action;
  }

  public List<Object> getInstructions() {
    return instructions;
  }

  public Optional<ProtocolComment> getComment() {
    return Optional.ofNullable(comment);
  }
}
