# Daikombat Server

Daikombat Server is the server code repository for a simple Doom-like online shooter game with a non-stop deathmatch mode. It is built using Maven, Netty, Spring, and Java 14.

## Prerequisites

- Java 14
- Maven


## Architecture overview

The server opens a TCP connection and serves protobuf messages(find `.proto` schema in `schema` module). 
The client publishes commands and then subscribes to server events by collecting them in an in-memory queue that is meant to be polled during game rendering. 
All communications (server-to-client and client-to-server) are totally non-blocking.

### Known issues

- TCP is a bad choice for fast-paced online shooter games due to head-of-line blocking. In the future, the protocol is likely to be changed to either UDP or QUIC.
- The server is totally not scalable at this moment. High availability and load balancing are not provided.

## Development

Here are some commands that might be useful while coding.

### Update all submodules' versions
```
mvn versions:set -DnewVersion=<version>
```

### Run static code analysis
```
mvn spotbugs:check 
```

### Update protobuf schema

```
rm .\schema\src\main\java\com\beverly\hills\money\gang\proto\* 
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-response.proto
protoc --java_out=./schema/src/main/java/ .\schema\src\main\resources\server-command.proto
```


## Testing and QA

The project provides good quality of service by:
- Unit and integration testing (Junit). Minimum coverage requirements can be found in Jacoco configs.
- Static code analysis (Spotbugs)
- Dependency check (Snyk)
- Resiliency testing (ToxiProxy)
- Game client manual acceptance testing is also involved in the process. Dev mode is provided for the desktop version.

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
- `GAME_SERVER_PASSWORD` Server access password. Used in HMAC that is appended to every message. Default - `daikombat`.

Game client is also configurable through environments variables:

- `CLIENT_MAX_SERVER_INACTIVE_MLS` Maximum server inactivity time(in milliseconds). Default - `10_000`.

## Deployment and integration

All submodules can be used as maven/gradle dependencies using [jitpack](https://jitpack.io/#beverly-hills-money-gangster/Daikombat-server).

TODO: Docker deployment.

## Run

Just run `main(String[] args)` in `com.beverly.hills.money.gang.Main`. No arguments are required. 

TODO: Fat jar running.
