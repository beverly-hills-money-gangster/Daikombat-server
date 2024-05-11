package com.beverly.hills.money.gang.scheduler;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.Game;

public interface Scheduler {

  void schedule(int afterMls, Runnable runnable);

}
