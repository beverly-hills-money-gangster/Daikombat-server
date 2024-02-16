
# Daikombat Server [![](https://jitpack.io/v/beverly-hills-money-gangster/Daikombat-server.svg)](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server) [![](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server/badge.svg)](https://snyk.io/test/github/beverly-hills-money-gangster/Daikombat-server)

Daikombat Server is the server code repository for a simple Doom-like online shooter game with a non-stop death-match mode. It is built using Maven, Netty, Spring, and Java 14.

## Prerequisites

- Java 14
- Maven 3.6.3


## Architecture overview

The server opens a TCP connection and serves in-game events as protobuf messages(find `.proto` schema in `schema` module). 
The client publishes game commands and then subscribes to server events by collecting them in an in-memory queue that is meant to be polled during game rendering. 
All communications (server-to-client and client-to-server) are totally non-blocking.

### Known issues

- TCP is a bad choice for fast-paced online shooter games due to head-of-line blocking. In the future, the protocol is likely to be changed to either UDP or QUIC.
- The server is totally not scalable at this moment. High availability and load balancing are not provided.
- The server is NOT authoritative
- Very basic anti-cheat(can move and shoot through walls)

## Configuration

Game server can be configured using the following environment variables:

- `GAME_SERVER_PORT`  TCP server port at which client connections are going to be accepted. Default - `7777`.
- `GAME_SERVER_GAMES_TO_CREATE` Games to create. Default - `10`.
- `GAME_SERVER_MAX_PLAYERS_PER_GAME` Maximum number of players to join a game. Default - `25`. Total number of players on the server is `GAME_SERVER_GAMES_TO_CREATE*GAME_SERVER_MAX_PLAYERS_PER_GAME`.
- `GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS` Frequency(in milliseconds) at which server notifies players about other players' positioning on the map. Default - `100`.
- `GAME_SERVER_PING_FREQUENCY_MLS` Frequency(in milliseconds) at which server pings all connected players. Default - `2_500`.
- `GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS` Frequency(in milliseconds) at which server checks if player connection is idle. Default - `10_000`.
- `GAME_SERVER_MAX_IDLE_TIME_MLS` Maximum idle time(in milliseconds) for a player. "Idle" - no network activity, which includes in-game events + ping responds. For example, if player connects to a game and doesn't move but responds to PING requests, then it's NOT considered idle. This check is mostly needed when a TCP connection was closed incorrectly(no FIN). Default - `10_000`.
- `GAME_SERVER_DEFAULT_DAMAGE` Shot damage. Default - `20`.
- `GAME_SERVER_PIN_CODE` Server access pin code(digits only, no less than 4). Used in HMAC that is appended to every message. Default - `5555`.

Game client is also configurable through environments variables:

- `CLIENT_MAX_SERVER_INACTIVE_MLS` Maximum server inactivity time(in milliseconds). Default - `10_000`.

## Deployment and integration

### Game client integration
All submodules, including network client, can be used as maven/gradle dependencies using [jitpack](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server).

### Docker

Server image is deployed on every release to [Github container registry](https://ghcr.io/beverly-hills-money-gangster/daikombat_server).
A sample [docker-compose file](/docker-compose.yaml) can be found in the root folder.

## Monitoring

### Server monitoring

Server JVM metrics can be monitored through JMX remotely. See [docker-compose file](/docker-compose.yaml). 
In the sample, JMX agent is running on port 9999 listening to incoming connections initiated by 127.0.0.1. 
If server is running in the cloud, then it's possible to port-forward JMX using the following command:

```
ssh -L 9999:127.0.0.1:9999 <user>@<remote_host>
```

Apart from JMX metrics, the server is configured to drop a heap dump file on out-of-memory.

### Client monitoring

`GameConnection.java` has `networkStats` field that gathers basic network stats including:
- number of sent protobuf messages
- number of received protobuf message
- total outbound payload size
- total inbound payload size

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
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-response.proto
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-command.proto
```


### Testing and QA

The project provides good quality of service by:
- Unit and integration testing (Junit). Minimum coverage requirements can be found in Jacoco configs.
- Static code analysis (Spotbugs)
- Dependency check (Snyk)
- Resiliency testing (ToxiProxy)
- Game client manual acceptance testing is also involved in the process. Dev mode is provided for the desktop version.