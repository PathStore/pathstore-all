FROM pull

RUN cp -r /cloudpath/version_0.0.1/PathStore/pathstore-admin-panel / && \
    rm -rf /cloudpath && \
    useradd -ms /bin/bash pathstore-admin-panel && \
    chown -R pathstore-admin-panel:pathstore-admin-panel /pathstore-admin-panel

USER pathstore-admin-panel

# set workdir
WORKDIR /pathstore-admin-panel

# start pathstore
CMD ["java", "-jar", "target/pathstore-admin-panel-0.0.1-SNAPSHOT.jar"]
