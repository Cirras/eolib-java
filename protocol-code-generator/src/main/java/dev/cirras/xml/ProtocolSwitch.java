package dev.cirras.xml;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "switch")
public class ProtocolSwitch {
  @XmlAttribute(required = true)
  private String field;

  @XmlElement(name = "case", required = true)
  private final List<ProtocolCase> cases = new ArrayList<>();

  public String getField() {
    return field;
  }

  public List<ProtocolCase> getCases() {
    return cases;
  }

  @SuppressWarnings("unused")
  private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (cases.stream().filter(ProtocolCase::isDefault).count() > 1) {
      throw new ProtocolXmlError("Only one default case is allowed.");
    }

    for (int i = 0; i + 1 < cases.size(); ++i) {
      ProtocolCase protocolCase = cases.get(i);
      if (protocolCase.isDefault()) {
        throw new ProtocolXmlError("Default case is only allowed at the end of the switch.");
      }
    }
  }
}
