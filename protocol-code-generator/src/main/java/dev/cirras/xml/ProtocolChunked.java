package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "chunked")
public class ProtocolChunked {
  @XmlElements({
    @XmlElement(name = "field", type = ProtocolField.class),
    @XmlElement(name = "array", type = ProtocolArray.class),
    @XmlElement(name = "dummy", type = ProtocolDummy.class),
    @XmlElement(name = "switch", type = ProtocolSwitch.class),
    @XmlElement(name = "chunked", type = ProtocolChunked.class),
    @XmlElement(name = "break", type = ProtocolBreak.class)
  })
  private final List<Object> instructions = new ArrayList<>();

  public List<Object> getInstructions() {
    return instructions;
  }
}
