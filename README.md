# LMQ or Live Music Quiz by Michał Janiec

Application is composed of 3 parts:
 * module for person conducting the quiz - maestro. It allows to define quiz and steer it.
 * module for big-screen. Part that should be displayed on the screen for each player to see.
 * and module for individual players.  

Flow:
 * maestro uses `/maestro/start` and chooses the quiz
 * then DJ view is displayed with link to big screen that should be displayed for all the players to see
 * shares the big screen under, players can read a link to game on it
 * players login to game
 * maestro is the master of the game

## Prerequisite

 * application requires PostgresSQL database.
 * setup it localy, or remotely, then either create a spring profile file with `application-local.properties` 
  with the following contnent:

  ```
spring.datasource.url=jdbc:postgresql://localhost:5432/<dbname>
spring.datasource.username=<username>
spring.datasource.password=<pass>
  ```
  make sure to ignore this file

## Building the app

```shell
./mvnw clean package -Pproduction
```

then running:

```shell
java -Dspring.profiles.active=local -jar target/live-music-quiz-1.0-SNAPSHOT.jar
```


## Production

Deployed to amazon via SSH. 
use `deploy.sh` script