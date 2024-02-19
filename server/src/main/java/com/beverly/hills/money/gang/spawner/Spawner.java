package com.beverly.hills.money.gang.spawner;

import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

// TODO spawn in the least populated place
@Component
public class Spawner {

    private static final Random RANDOM = new Random();

    private final List<PlayerState.PlayerCoordinates> playerCoordinates = List.of(

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-24.657965F).y(23.160273F).build())
                    .direction(
                            Vector.builder().x(-0.00313453F).y(-0.9999952F).build()).build(),

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-20.251183F).y(28.641932F).build())
                    .direction(
                            Vector.builder().x(-0.9999984F).y(0.0018975139F).build()).build(),

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-29.052916F).y(28.827929F).build())
                    .direction(
                            Vector.builder().x(0.9985066F).y(0.05463622F).build()).build(),

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-30.03456F).y(19.154572F).build())
                    .direction(
                            Vector.builder().x(-0.0112161245F).y(0.99993724F).build()).build(),

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-19.544775F).y(20.086754F).build())
                    .direction(
                            Vector.builder().x(-0.032291915F).y(0.99947816F).build()).build(),

            PlayerState.PlayerCoordinates.builder().position(
                            Vector.builder().x(-30.80954F).y(23.183435F).build())
                    .direction(
                            Vector.builder().x(0.9999779F).y(0.0065644206F).build()).build()
    );

    public PlayerState.PlayerCoordinates spawn() {
        return playerCoordinates.get(RANDOM.nextInt(playerCoordinates.size()));
    }
}
