package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameWeaponType;
import java.util.Map;


public interface AmmoStorageReader {

  Map<GameWeaponType, Integer> getCurrentAmmo();

}
