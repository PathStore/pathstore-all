FROM pull

RUN cp -r /cloudpath/version_0.0.1/PathStore/apache-cassandra-3.9 / && \
    rm -rf /cloudpath && \
    useradd -ms /bin/bash cassandra && \
    chown -R cassandra:cassandra /apache-cassandra-3.9

USER cassandra

# start cassandra
CMD ["./apache-cassandra-3.9/bin/cassandra", "-f"]
