# Daikombat Server

Daikombat Server is the server code for a simple Doom-like online shooter game with a deathmatch mode. It is built using Maven, Netty, Spring, and Java 14.

## Prerequisites

- Java 14
- Maven

## Develop

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


## Test
TODO
## Deploy
TODO
## Run
TODO
