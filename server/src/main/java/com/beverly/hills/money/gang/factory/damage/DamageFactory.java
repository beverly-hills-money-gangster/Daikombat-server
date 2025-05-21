package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public interface DamageFactory {

  Damage getDamage(GameReader gameReader);
}
