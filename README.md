# Daikombat Server [![](https://jitpack.io/v/beverly-hills-money-gangster/Daikombat-server.svg)](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server) [![](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server/badge.svg)](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server)

Daikombat Server is the server code repository for a simple Doom-like online shooter game with a
non-stop death-match mode. It is built using Maven, Netty, Spring, and Java 14.
Client/desktop code is also publicly available
on [GitHub](https://github.com/beverly-hills-money-gangster/DaikombatDesktop).

## Prerequisites

- Java 14
- Maven 3.6.3

## Architecture overview

The server opens a TCP connection and serves in-game events as protobuf messages(find `.proto`
schema in `schema` module).
The client publishes game commands and then subscribes to server events by collecting them in an
in-memory queue that is meant to be polled during game rendering.
All communications (server-to-client and client-to-server) are totally non-blocking.

### Known issues

- TCP is a bad choice for fast-paced online shooter games due to head-of-line blocking. In the
  future, the protocol is likely to be changed to either UDP or QUIC.
- The server is totally not scalable at this moment. High availability and load balancing are not
  provided.
- The server is NOT authoritative
- Basic anti-cheat

## Configuration

Game server can be configured using the following environment variables:

- `GAME_SERVER_PORT`  TCP server port at which client connections are going to be accepted.
  Default - `7777`.
- `GAME_SERVER_GAMES_TO_CREATE` Games to create. Default - `10`.
- `GAME_SERVER_MAX_PLAYERS_PER_GAME` Maximum number of players to join a game. Default - `25`. Total
  number of players on the server is `GAME_SERVER_GAMES_TO_CREATE*GAME_SERVER_MAX_PLAYERS_PER_GAME`.
- `GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS` Frequency(in milliseconds) at which server notifies
  players about other players' positioning on the map. Default - `50`.
- `GAME_SERVER_MAX_IDLE_TIME_MLS` Maximum idle time(in milliseconds) for a player. "Idle" - no
  network activity, which includes in-game events + ping responds. For example, if player connects
  to a game and doesn't move but responds to PING requests, then it's NOT considered idle. This
  check is mostly needed when a TCP connection was closed abruptly(no FIN). Default - `10_000`.
- `GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE` Gunshot damage. Default - `20`.
- `GAME_SERVER_DEFAULT_RAILGUN_DAMAGE` Railgun damage. Default - `75`.
- `GAME_SERVER_DEFAULT_PUNCH_DAMAGE` Punch damage. Default - `50`.
- `GAME_SERVER_DEFAULT_MINIGUN_DAMAGE` Minigun damage. Default - `5`.
- `GAME_SERVER_FAST_TCP` Enables fast TCP configurations(used mostly for testing, not recommended to
  be set to `false` in prod). Default - `true`.
- `GAME_SERVER_FRAGS_PER_GAME` Frags to win a game. Default - `25`.
- `GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS` Time (in millisecond) it takes to spawn a quad damage power-up
  orb. Default - `45_000`.
- `GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS` Time (in millisecond) quad damage lasts for.
  Default - `10_000`.
- `GAME_SERVER_DEFENCE_SPAWN_MLS` Time (in millisecond) it takes to spawn a defence power-up orb.
  Default - `35_000`.
- `GAME_SERVER_DEFENCE_LASTS_FOR_MLS` Time (in millisecond) defence lasts for. Default - `10_000`.
- `GAME_SERVER_INVISIBILITY_SPAWN_MLS` Time (in millisecond) it takes to spawn an invisibility
  power-up orb. Default - `30_000`.
- `GAME_SERVER_HEALTH_SPAWN_MLS` Time (in millisecond) it takes to spawn a health power-up orb.
  Default - `35_000`.
- `GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS` Time (in millisecond) invisibility lasts for.
  Default - `15_000`.
- `GAME_SERVER_POWER_UPS_ENABLED` Turns power-ups on and off. Default - `true`.
- `GAME_SERVER_TELEPORTS_ENABLED` Turns teleports on and off. Default - `true`.
- `GAME_SERVER_PLAYER_SPEED` Player speed. Default - `5`.
- `GAME_SERVER_PLAYER_STATS_TIMEOUT_MLS` Time (in milliseconds) to store player's stats for
  connection recovery. Default - `120000`.
- `GAME_SERVER_PLAYER_SPEED_CHECK_FREQUENCY_MLS` Frequency(in milliseconds) at which server checks
  all players' speed. Anybody moving faster than `GAME_SERVER_PLAYER_SPEED` will be kicked-out.
  Default - `10_000`
- `GAME_SERVER_PUNCH_DELAY_MLS` Punch attack delay(in milliseconds). Default - `300`
- `GAME_SERVER_SHOTGUN_DELAY_MLS` Shotgun attack delay(in milliseconds). Default - `450`
- `GAME_SERVER_RAILGUN_DELAY_MLS` Railgun attack delay(in milliseconds). Default - `1_700`
- `GAME_SERVER_MINIGUN_DELAY_MLS` Minigun attack delay(in milliseconds). Default - `155`
- `SENTRY_DSN` Sentry DSN. Not specified by default.

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

### Testing and QA

The project provides good quality of service by:

- Unit and integration testing (Junit). Minimum coverage requirements can be found in Jacoco
  configs.
- Static code analysis (Spotbugs)
- Dependency check (Snyk)
- Resiliency testing (ToxiProxy)
- Load testing. See `com.beverly.hills.money.gang.bots.BotRunner` for spawning dummy bots
- Game client manual acceptance testing is also involved in the process. Dev mode is provided for
  the desktop version.