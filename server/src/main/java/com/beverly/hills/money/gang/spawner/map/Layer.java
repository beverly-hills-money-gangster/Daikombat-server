package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Data;

@Data
@XmlRootElement(name = "layer")
@XmlAccessorType(XmlAccessType.FIELD)
public class Layer {

  @XmlAttribute(name = "id")
  private int id;

  @XmlAttribute(name = "name")
  private String name;

  @XmlAttribute(name = "width")
  private int width;

  @XmlAttribute(name = "height")
  private int height;

  @XmlElement(name = "data")
  private Data data;

  @lombok.Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Data {

    @XmlAttribute(name = "encoding")
    private String encoding;

    @XmlValue
    private String value;

  }
}
