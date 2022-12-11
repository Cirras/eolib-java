package dev.cirras.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "protocol")
public final class Protocol {
  @XmlElement(name = "enum")
  private final List<ProtocolEnum> enums = new ArrayList<>();

  @XmlElement(name = "struct")
  private final List<ProtocolStruct> structs = new ArrayList<>();

  @XmlElement(name = "packet")
  private final List<ProtocolPacket> packets = new ArrayList<>();

  public List<ProtocolEnum> getEnums() {
    return enums;
  }

  public List<ProtocolStruct> getStructs() {
    return structs;
  }

  public List<ProtocolPacket> getPackets() {
    return packets;
  }
}
