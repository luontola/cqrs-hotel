#!/usr/bin/env bash
set -eux

docker-compose up -d db
mvn clean verify
yarn install
yarn run test
docker-compose build --pull
