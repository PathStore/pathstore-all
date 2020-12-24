FROM base

# set workdir to cloudpath to pull
WORKDIR /cloudpath

# pull new contents
RUN git pull

# set workdir to pathstore
WORKDIR /cloudpath/version_0.0.1/PathStore/pathstore

# package pathstore jar
RUN mvn package

# set workdir to website
WORKDIR /cloudpath/version_0.0.1/PathStore/pathstore-admin-panel

# package website
RUN mvn package

# Reset workdir after build.
WORKDIR /