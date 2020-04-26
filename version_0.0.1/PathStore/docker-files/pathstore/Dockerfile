FROM openjdk:11

# Define build-args
ARG key
ARG branch

WORKDIR /

# Install git
RUN apt-get update && \ 
apt-get install -y git maven && \
mkdir -p /root/.ssh && \
echo "$key" > /root/.ssh/id_rsa && \ 
chmod 400 /root/.ssh/id_rsa && \
touch /root/.ssh/known_hosts && \
ssh-keyscan github.com >> /root/.ssh/known_hosts && \
git clone --depth 1 --single-branch --branch $branch git@github.com:delara/cloudpath.git && \
rm -rf /root/.ssh && \
mkdir -p /etc/pathstore

COPY pathstore.properties /etc/pathstore/pathstore.properties

# set workdir
WORKDIR /cloudpath/version_0.0.1/PathStore/pathstore

# package product
RUN mvn package

CMD ["java", "-jar", "target/pathstore-0.1.0-jar-with-dependencies.jar"]
