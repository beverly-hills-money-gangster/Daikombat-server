package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.state.Vector;
import org.springframework.stereotype.Component;

@Component
public class AntiCheat {

    private static final double MAX_SHOOTING_DISTANCE = 10;

    private static final double MAX_PUNCHING_DISTANCE = 1;

    public boolean isShootingTooFar(final Vector shooterPosition, final Vector victimPosition) {
        return Vector.getDistance(shooterPosition, victimPosition) > MAX_SHOOTING_DISTANCE;
    }

    public boolean isPunchingTooFar(final Vector shooterPosition, final Vector victimPosition) {
        return Vector.getDistance(shooterPosition, victimPosition) > MAX_PUNCHING_DISTANCE;
    }

}
