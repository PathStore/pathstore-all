FROM pull

RUN mkdir cassandra  && \
    cp -r /cloudpath/version_0.0.1/PathStore/apache-cassandra-3.9 /cassandra && \
    rm -rf /cloudpath && \
    useradd -ms /bin/bash cassandra && \
    chown -R cassandra:cassandra /cassandra

USER cassandra

# start cassandra
CMD ["./cassandra/bin/cassandra", "-f"]
