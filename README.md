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

## Building the app

```shell
./mvnw clean package -Pproduction
```

then running:

```shell
java -jar target/live-music-quiz-1.0-SNAPSHOT.jar
```

build a docker image 
```shell
docker build -t lmq:latest .
```
run image:
```shell
docker run -p8887:8080 lmq:latest
```

## DB

Postgres is used

## Production

Deployed to amazon via SSH. 
use `deploy.sh` script