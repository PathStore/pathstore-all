FROM openjdk:11

MAINTAINER Myles Thiessen<myles.thiessen@mail.utoronto.ca>

RUN mkdir -p /cassandra
WORKDIR /cassandra
COPY . .
CMD ["./bin/cassandra", "-R", "-f"]
