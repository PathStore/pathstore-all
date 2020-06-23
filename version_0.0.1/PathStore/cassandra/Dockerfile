from openjdk:11 

RUN mkdir -p /program

COPY . /program

WORKDIR /program

RUN ls -l 

CMD ["./bin/cassandra", "-f", "-R"]
