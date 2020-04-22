FROM ubuntu as intermediate

MAINTAINER Myles Thiessen<myles.thiessen@mail.utoronto.ca>

# define build-args
ARG key
ARG branch

# Set workdir
WORKDIR /

# Update container
RUN apt-get update
RUN apt-get install -y git

# setup deploy key
RUN mkdir -p /root/.ssh
RUN echo "$key" > /root/.ssh/id_rsa
RUN chmod 400 /root/.ssh/id_rsa

# setup github.com as a known host
RUN touch /root/.ssh/known_hosts
RUN ssh-keyscan github.com >> /root/.ssh/known_hosts

# clone repo
RUN git clone git@github.com:delara/cloudpath.git

# checkout branch that is wanted
RUN cd cloudpath && git checkout $branch

# remove deploy keys
RUN rm -rf /root/.ssh

FROM openjdk:11

# create cassandra dir
RUN mkdir -p /cassandra

# setup as workdir
WORKDIR /cassandra

# copy cassandra data into workdir
COPY --from=intermediate /cloudpath/version_0.0.1/PathStore/apache-cassandra-3.9 /cassandra

# start cassandra
CMD ["./bin/cassandra", "-R", "-f"]
