FROM ubuntu as intermediate

MAINTAINER Myles Thiessen<myles.thiessen@mail.utoronto.ca>

# Define build-args
ARG key
ARG branch

# Set workdir
WORKDIR /

# Install git
RUN apt-get update
RUN apt-get install -y git

# Clone deploy key into container
RUN mkdir -p /root/.ssh
RUN echo "$key" > /root/.ssh/id_rsa
RUN chmod 400 /root/.ssh/id_rsa

# Update known hosts to include github.com
RUN touch /root/.ssh/known_hosts
RUN ssh-keyscan github.com >> /root/.ssh/known_hosts

# Clone repo
RUN git clone git@github.com:delara/cloudpath.git 

#temp switch branch (Switch to master in offical build)
RUN cd cloudpath && git checkout $branch 

# remove deploy key
RUN rm -rf /root/.ssh

# Running container
FROM openjdk:11

# Create pathstore config directory
RUN mkdir -p /etc/pathstore
COPY pathstore.properties /etc/pathstore/pathstore.properties

# Copy repo from intermediate container
COPY --from=intermediate /cloudpath /cloudpath

# set workdir
WORKDIR /cloudpath/version_0.0.1/PathStore/pathstore

# install maven
RUN apt-get update
RUN apt-get install -y maven

# package product
RUN mvn package

CMD ["java", "-jar", "target/pathstore-0.1.0-jar-with-dependencies.jar"]
