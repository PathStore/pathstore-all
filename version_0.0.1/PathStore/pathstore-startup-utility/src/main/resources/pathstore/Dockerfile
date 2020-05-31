FROM pull

RUN cp -r /cloudpath/version_0.0.1/PathStore/pathstore / && \
    rm -rf /cloudpath

# set workdir
WORKDIR /pathstore

# start pathstore
CMD ["java", "-jar", "target/pathstore-0.1.0-jar-with-dependencies.jar"]
