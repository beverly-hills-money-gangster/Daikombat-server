package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class MapObject {

  @XmlAttribute
  private int id;
  @XmlAttribute
  private String name;
  @XmlAttribute
  private Float x;
  @XmlAttribute
  private Float y;
  @XmlAttribute
  private Float width;
  @XmlAttribute
  private Float height;
  @XmlAttribute
  private Integer gid;

  @XmlElementWrapper(name = "properties")
  @XmlElement(name = "property")
  private List<Property> properties;

  public Optional<Property> findProperty(final String name) {
    return properties.stream()
        .filter(property -> property.getName().equals(name)).findFirst();
  }

  public Property getProperty(final String name) {
    return findProperty(name).orElseThrow(() -> new IllegalStateException(
        "Property '" + name + "' not found in object '" + id + "'"));
  }
}
