package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.*;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.Data;

import java.util.List;

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

  public Optional<Property> findProperty(final String name){
    return properties.stream()
        .filter(property -> property.getName().equals(name)).findFirst();
  }
}
