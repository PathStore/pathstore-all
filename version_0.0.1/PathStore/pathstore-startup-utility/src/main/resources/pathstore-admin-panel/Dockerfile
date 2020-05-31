FROM pull

RUN mkdir /pathstore-admin-panel && \
    cp -r /cloudpath/version_0.0.1/PathStore/pathstore-admin-panel /pathstore-admin-panel && \
    rm -rf /cloudpath

# set workdir
WORKDIR /pathstore-admin-panel

# start pathstore
CMD ["java", "-jar", "target/pathstore-admin-panel-0.0.1-SNAPSHOT.jar"]
