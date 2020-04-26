FROM openjdk:11 

# define build-args
ARG key
ARG branch

# Set workdir
WORKDIR /

# Update container
RUN apt-get update && \
apt-get install -y git && \
mkdir -p /root/.ssh && \
echo "$key" > /root/.ssh/id_rsa && \
chmod 400 /root/.ssh/id_rsa && \
touch /root/.ssh/known_hosts && \
ssh-keyscan github.com >> /root/.ssh/known_hosts && \
git clone --depth 1 --single-branch --branch $branch git@github.com:delara/cloudpath.git && \
rm -rf /root/.ssh

# start cassandra
CMD ["./cloudpath/version_0.0.1/PathStore/apache-cassandra-3.9/bin/cassandra", "-R", "-f"]
