# Daikombat Server [![](https://jitpack.io/v/beverly-hills-money-gangster/Daikombat-server.svg)](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server) [![](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server/badge.svg)](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server)

Daikombat Server is the server code repository for a simple Doom-like online shooter game with a
non-stop death-match mode. It is built using Maven, Netty, Spring, and Java 14.
Client/desktop code is also publicly available
on [GitHub](https://github.com/beverly-hills-money-gangster/DaikombatDesktop).

## Prerequisites

- Java 14
- Maven 3.6.3

## Architecture overview

The server opens a network connection and pushes in-game events as protobuf messages(find `.proto`
schema in `schema` module).
The client publishes game commands and then subscribes to server events by collecting them in an
in-memory queue that is meant to be polled during game rendering.
All communications (server-to-client and client-to-server) are totally non-blocking.

## Core dev principles

- All configuration defaults should be prod-ready
- The code should be simple, so I can understand it at 2 AM after taking a two-month
  break
- Add as many tests as possible even if it looks non-practical at a moment(you will appreciate it
  later)
- No AI

### Known issues

- The server is not scalable at this moment. High availability and load balancing are not
  provided.
- The server is not fully authoritative
- Basic anti-cheat

## Configuration

Game server can be configured using the following environment variables:

- `GAME_SERVER_PORT` TCP server port at which client connections are going to be accepted. For
  simplicity, UDP port is `GAME_SERVER_PORT`+1. Default - `7777`.
- `GAME_SERVER_BLACKLISTED_WORDS` Blacklisted words. Affects chat messages and player names. Words
  should be separated by a comma (`,`) symbol. Case-insensitive. Example: `ABC,XYZ,QWE`. Not set by
  default.
- `GAME_SERVER_BIG_UDP_WARNING` Pushes a warning to logs if a big UDP datagram was sent over a
  network. Default - `false`.
- `GAME_SERVER_GAMES_TO_CREATE` Games to create. Default - `1`.
- `GAME_SERVER_MAX_PLAYERS_PER_GAME` Maximum number of players to join a game. Default - `25`. Total
  number of players on the server is `GAME_SERVER_GAMES_TO_CREATE*GAME_SERVER_MAX_PLAYERS_PER_GAME`.
- `GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS` Frequency(in milliseconds) at which server notifies
  players about other players' positioning on the map. Default - `50`.
- `GAME_SERVER_MAX_IDLE_TIME_MLS` Maximum idle time(in milliseconds) for a player. "Idle" - no
  network activity, which includes in-game events + ping responds. For example, if player connects
  to a game and doesn't move but responds to PING requests, then it's NOT considered idle. This
  check is mostly needed when a TCP connection was closed abruptly(no FIN). Default - `10_000`.
- `GAME_SERVER_MAX_VISIBILITY` Maximum visibility on the map. Default - `14`. Events outside of this
  radius won't be transmitted over the network to save traffic.
- `GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE` Gunshot damage. Default - `20`.
- `GAME_SERVER_DEFAULT_RAILGUN_DAMAGE` Railgun damage. Default - `75`.
- `GAME_SERVER_DEFAULT_PUNCH_DAMAGE` Punch damage. Default - `50`.
- `GAME_SERVER_DEFAULT_ROCKET_DAMAGE` Rocket damage. Default - `75`.
- `GAME_SERVER_DEFAULT_MINIGUN_DAMAGE` Minigun damage. Default - `7`.
- `GAME_SERVER_DEFAULT_PLASMA_DAMAGE` Plasma damage. Default - `10`.
- `GAME_SERVER_FAST_TCP` Enables fast TCP configurations(used mostly for testing, not recommended to
  be set to `false` in prod). Default - `true`.
- `GAME_SERVER_FRAGS_PER_GAME` Frags to win a game. Default - `25`.
- `GAME_SERVER_SPAWN_IMMORTAL_MLS` Time (in millisecond) players are immortal after spawn/respawn.
  Default - `2000`
- `GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS` Time (in millisecond) it takes to spawn a quad damage power-up
  orb. Default - `45_000`.
- `GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS` Time (in millisecond) quad damage lasts for.
  Default - `15_000`.
- `GAME_SERVER_DEFENCE_SPAWN_MLS` Time (in millisecond) it takes to spawn a defence power-up orb.
  Default - `35_000`.
- `GAME_SERVER_DEFENCE_LASTS_FOR_MLS` Time (in millisecond) defence lasts for. Default - `10_000`.
- `GAME_SERVER_INVISIBILITY_SPAWN_MLS` Time (in millisecond) it takes to spawn an invisibility
  power-up orb. Default - `30_000`.
- `GAME_SERVER_MAP_NAME` Map name. Must be one of built-in maps (
  see `/server/main/resources/maps` folder). Default - `classic`.
- `GAME_SERVER_AMMO_SPAWN_MLS` Time (in millisecond) it takes to spawn ammo. Default - `30_000`.
- `GAME_SERVER_HEALTH_SPAWN_MLS` Time (in millisecond) it takes to spawn a health power-up orb.
  Default - `35_000`.
- `GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS` Time (in millisecond) invisibility lasts for.
  Default - `15_000`.
- `GAME_SERVER_POWER_UPS_ENABLED` Turns power-ups on and off. Default - `true`.
- `GAME_SERVER_TELEPORTS_ENABLED` Turns teleports on and off. Default - `true`.
- `GAME_SERVER_PLAYER_SPEED` Base player speed. Changes depending on class. Default - `7`.
- `GAME_SERVER_PLAYER_STATS_TIMEOUT_MLS` Time (in milliseconds) to store player's stats for
  connection recovery. Default - `120000`.
- `GAME_SERVER_PUNCH_DELAY_MLS` Punch attack delay(in milliseconds). Default - `300`
- `GAME_SERVER_ROCKET_LAUNCHER_DELAY_MLS` Rocket launcher delay(in milliseconds). Default - `1_500`.
- `GAME_SERVER_PLASMAGUN_DELAY_MLS` Plasma gun delay(in milliseconds). Default - `155`.
- `GAME_SERVER_SHOTGUN_DELAY_MLS` Shotgun attack delay(in milliseconds). Default - `450`
- `GAME_SERVER_RAILGUN_DELAY_MLS` Railgun attack delay(in milliseconds). Default - `1_700`
- `GAME_SERVER_MINIGUN_DELAY_MLS` Minigun attack delay(in milliseconds). Default - `155`
- `GAME_SERVER_SHOTGUN_MAX_AMMO` Shotgun max ammo. Default - `20`
- `GAME_SERVER_RAILGUN_MAX_AMMO` Railgun max ammo. Default - `15`
- `GAME_SERVER_MINIGUN_MAX_AMMO` Minigun max ammo. Default - `100`
- `GAME_SERVER_PLASMAGUN_MAX_AMMO` Plasmagun max ammo. Default - `50`
- `GAME_SERVER_ROCKET_LAUNCHER_MAX_AMMO` Rocket launcher max ammo. Default - `10`
- `GAME_SERVER_BEAST_SPAWN_MLS` Time (in millisecond) it takes to spawn a beast power-up
  orb. Default - `60_000`.
- `GAME_SERVER_BEAST_LASTS_FOR_MLS` Time (in millisecond) defence lasts for. Default - `25_000`.
- `SENTRY_DSN` Sentry DSN. Not specified by default.

Some environment variables can be overridden for a specific game room. The format
is `<ROOM_ID>_<ENV_VAR_NAME>`. For example `0_GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE=99` overrides
`GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE` variable for game room 0. Here is the list of all room-specific
configs:

- `GAME_SERVER_ROOM_TITLE`
- `GAME_SERVER_ROOM_DESCRIPTION`
- `GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE`
- `GAME_SERVER_DEFAULT_RAILGUN_DAMAGE`
- `GAME_SERVER_DEFAULT_MINIGUN_DAMAGE`
- `GAME_SERVER_DEFAULT_PLASMA_DAMAGE`
- `GAME_SERVER_DEFAULT_ROCKET_DAMAGE`
- `GAME_SERVER_DEFAULT_PUNCH_DAMAGE`
- `GAME_SERVER_PUNCH_DELAY_MLS`
- `GAME_SERVER_SHOTGUN_DELAY_MLS`
- `GAME_SERVER_RAILGUN_DELAY_MLS`
- `GAME_SERVER_ROCKET_LAUNCHER_DELAY_MLS`
- `GAME_SERVER_PLASMAGUN_DELAY_MLS`
- `GAME_SERVER_MINIGUN_DELAY_MLS`
- `GAME_SERVER_PLAYER_SPEED`
- `GAME_SERVER_MAX_VISIBILITY`
- `GAME_SERVER_SHOTGUN_MAX_AMMO`
- `GAME_SERVER_RAILGUN_MAX_AMMO`
- `GAME_SERVER_MINIGUN_MAX_AMMO`
- `GAME_SERVER_PLASMAGUN_MAX_AMMO`
- `GAME_SERVER_ROCKET_LAUNCHER_MAX_AMMO`
- `GAME_SERVER_MAP_NAME`

Game client is also configurable through environments variables:

- `CLIENT_MAX_SERVER_INACTIVE_MLS` Maximum server inactivity time(in milliseconds).
  Default - `10_000`.
- `CLIENT_FAST_TCP` Enables fast TCP configurations(used mostly for testing, not recommended to be
  set to `false` in prod). Default - `true`.

Note: all default (both client and server) configurations are production-ready.

## Deployment and integration

### Game client integration

All submodules, including network client, can be used as maven/gradle dependencies
using [jitpack](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server).

### Docker

Server image is deployed on every release
to [Github container registry](https://ghcr.io/beverly-hills-money-gangster/daikombat_server).
A sample [docker-compose file](/docker-compose.yaml) can be found in the root folder.

## Monitoring

### Server monitoring

#### JVM metrics

Server JVM metrics can be monitored through JMX remotely.
See [docker-compose file](/docker-compose.yaml).
In the sample, JMX agent is running on port 9999 listening to incoming connections initiated by
127.0.0.1.
If server is running in the cloud, then it's possible to port-forward JMX using the following
command:

```
ssh -L 9999:127.0.0.1:9999 <user>@<remote_host>
```

#### Heap dumps

Apart from JMX metrics, the server is configured to drop a heap dump file on out-of-memory.

#### Micrometer

All command handlers publish Micrometer timer metrics through JMX. See "metrics" in Jconsole "
MBeans" tab.

#### Sentry

All errors are automatically published to Sentry. See `SENTRY_DSN` env var.

### Client monitoring

`GameConnection.java` has `networkStats` field that gathers basic network stats including:

- ping (and also 50th, 75th, and 99th percentiles)
- number of sent protobuf messages
- number of received protobuf messages
- total protobuf outbound payload size
- total protobuf inbound payload size

## Development

### Handy Maven commands

Here are some commands that might be useful while coding.

#### Update all submodules' versions

```
mvn versions:set -DnewVersion=<version>
```

#### Run static code analysis

```
mvn spotbugs:check 
```

#### Update protobuf schema

```
rm .\schema\src\main\java\com\beverly\hills\money\gang\proto\* 
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-common.proto
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-response.proto
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-command.proto
```