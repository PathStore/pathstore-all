from openjdk:11 

RUN mkdir -p /program

COPY . /program

WORKDIR /program

CMD ["./bin/cassandra", "-f", "-R", "-Dcassandra.superuser_setup_delay_ms=0"]
