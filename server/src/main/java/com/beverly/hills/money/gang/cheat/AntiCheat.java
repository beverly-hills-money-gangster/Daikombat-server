package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.state.Vector;
import org.springframework.stereotype.Component;

@Component
public class AntiCheat {

    private static final double MAX_SPEED = 0.75;

    private static final double MAX_SHOOTING_DISTANCE = 10;

    public boolean isMovingTooFast(final Vector oldPosition, final Vector newPosition) {
        return getDistance(oldPosition, newPosition) > MAX_SPEED;
    }

    public boolean isShootingTooFar(final Vector shooterPosition, final Vector victimPosition) {
        return getDistance(shooterPosition, victimPosition) > MAX_SHOOTING_DISTANCE;
    }

    private double getDistance(Vector v1, Vector v2) {
        float x = v1.getX() - v2.getX();
        float y = v1.getY() - v2.getY();
        return Math.sqrt(x * x + y * y);
    }
}
