package com.beverly.hills.money.gang.spawner.map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;
import lombok.Data;

@Data
@XmlRootElement(name = "map")
@XmlAccessorType(XmlAccessType.FIELD)
public class MapData {
  @XmlAttribute
  private String version;
  @XmlAttribute
  private String tiledversion;
  @XmlAttribute
  private String orientation;
  @XmlAttribute
  private String renderorder;
  @XmlAttribute
  private int width;
  @XmlAttribute
  private int height;
  @XmlAttribute
  private int tilewidth;
  @XmlAttribute
  private int tileheight;
  @XmlAttribute(name = "nextlayerid")
  private int nextLayerId;
  @XmlAttribute(name = "nextobjectid")
  private int nextObjectId;

  @XmlElement(name = "objectgroup")
  private List<ObjectGroup> objectgroup;
}
