FROM base 

# Copy base image tar file to /etc/pathstore
COPY base.tar /etc/pathstore/base.tar

# Copy config file
COPY pathstore.properties /etc/pathstore/pathstore.properties

# set workdir
WORKDIR /cloudpath/version_0.0.1/PathStore/pathstore

# start pathstore
CMD ["java", "-jar", "target/pathstore-0.1.0-jar-with-dependencies.jar"]
