#!/bin/sh

host="ec2-user@ec2-52-59-243-95.eu-central-1.compute.amazonaws.com"

./mvnw clean package -Pproduction
ssh -i "lmq.pem" $host "sudo systemctl stop lmq.service"
scp -i "lmq.pem" target/live-music-quiz-1.0-SNAPSHOT.jar $host:/opt/lmq-service/app.jar
ssh -i "lmq.pem" $host "sudo systemctl start lmq.service"
