#!/usr/bin/env bash
set -eux

docker-compose up -d db
mvn clean verify
npm install
npm run build
docker-compose build
