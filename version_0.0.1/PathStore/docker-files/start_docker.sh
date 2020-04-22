#!/bin/sh

# cassandra 
cd cassandra
docker build -t cassandra --build-arg key="$(cat ../deploy_key)" --build-arg branch="pathstore_init_script" .
docker run --network=host -dit --rm --name cassandra cassandra
cd ..

# to ensure that cassandra is up before pathstore starts
sleep 15

#pathstore
cd pathstore
docker build -t pathstore --build-arg key="$(cat ../deploy_key)" --build-arg branch="pathstore_init_script" .
docker run --network=host -dit --rm --name pathstore pathstore
