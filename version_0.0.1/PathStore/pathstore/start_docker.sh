#!/bin/sh

# build images
cd ../apache-cassandra-3.9
docker build -t cassandra .
cd ../pathstore
docker build -t pathstore_prod --build-arg key="$(cat deploy_key)" .

# start images
docker run --network=host -dit --rm --name cassandra cassandra
sleep 10
docker run --network=host -dit --rm --name pathstore_prod pathstore_prod
