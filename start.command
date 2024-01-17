#!/bin/zsh

cd /Users/ruslanhumeniuk/IdeaProjects/sockets || exit

mvn clean package && java -cp target/sockets-1.0-SNAPSHOT.jar org.example.Main
