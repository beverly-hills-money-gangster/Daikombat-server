package com.beverly.hills.money.gang.validator;

import com.beverly.hills.money.gang.spawner.map.MapData;
import com.beverly.hills.money.gang.spawner.map.ObjectGroup;
import com.google.common.math.DoubleMath;
import org.apache.commons.lang3.StringUtils;

public class MapValidator implements Validator<MapData> {

  private static final int MAX_MAP_SIZE = 128;

  @Override
  public void validate(MapData map) {
    if (map.getWidth() > MAX_MAP_SIZE || map.getHeight() > MAX_MAP_SIZE) {
      throw new IllegalStateException("Map is too big. Max size is " + MAX_MAP_SIZE);
    }
    map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("decorations")).findFirst()
        .map(ObjectGroup::getObject)
        .ifPresent(decorations -> decorations.forEach(decoration -> {
          if (StringUtils.isBlank(decoration.getName())) {
            throw new IllegalStateException(
                "Decoration missing name. See decoration " + decoration.getId());
          }
        }));

    map.getObjectgroup().stream()
        .filter(group -> group.getName().equals("rects")).findFirst()
        .map(ObjectGroup::getObject)
        .ifPresent(walls -> walls.forEach(wall -> {
          if (!(divisibleBy16(wall.getHeight()) && divisibleBy16(wall.getWidth()))) {
            throw new IllegalStateException(
                "Wall size should be divisible by 16. See wall " + wall.getId());
          }
        }));
  }

  private boolean divisibleBy16(float number) {
    return DoubleMath.isMathematicalInteger(number) && ((int) number) % 16 == 0;
  }
}
