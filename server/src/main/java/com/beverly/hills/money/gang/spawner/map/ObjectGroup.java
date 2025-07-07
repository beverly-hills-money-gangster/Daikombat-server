package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ObjectGroup {
  @XmlAttribute
  private int id;
  @XmlAttribute
  private String name;

  @XmlElement(name = "object")
  private List<MapObject> object;
}