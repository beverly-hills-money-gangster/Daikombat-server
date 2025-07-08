package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Property {

  @XmlAttribute
  private String name;
  @XmlAttribute
  private String value;
}