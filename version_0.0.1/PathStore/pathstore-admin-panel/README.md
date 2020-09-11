PathStore Admin Panel
===
Purpose
---
* This website is used to interact with the data storage system for edge computing called PathStore. Some of the key 
points are as follows:
    1. Create and update the network topology
    2. Install user-defined applications
        1. Applications are cassandra keyspaces
        2. Keyspace are composed of database tables and other features associated with Cassandra, like UDT's
    3. Deploy applications onto the network
        1. Each node can have a unique set of applications installed on them under the assumption every parent node has 
        all the applications its children do
    4. View node's status
        1. Relative to their application deployment status
        2. Information associated with each node like:
            1. What IP the server has
            2. What user account was used to deploy pathstore
            3. View pathstore's internal log from the website
 * Deployment of the root node. This is done as there has to be some starting point to the network, and since the website 
 and the root node are tightly coupled together the website is responsible for deployment of the root node.
 
 How to setup a server to be able to run a PathStore node
 ---
 PathStore has been tested to work on two operating systems (Ubuntu 18.04 LTS, Gentoo Linux [Kernel 5.5.13]). All these steps should
 work on any other linux distribution but your mileage may vary. The steps to setup a server to run a PathStore Node
 are as follows:
 
 1. Install docker and enable docker to start on machine startup.
 2. Ensure you don't have a 127.0.1.1 record in your /etc/hosts as this will cause child nodes to not be able to connect
 to the RMI server.
 3. Setup a user account specifically used for the process of deployment WITHOUT sudo access. This account needs
 to be added to the docker group.
 
 These commands on Ubuntu 18.04 LTS would look as follows:
 
 ```bash
1. apt-get -y update && apt-get -y install docker.io && systemctl start docker && systemctl enable docker
2. // Edit the hosts file to ensure there is only a 127.0.0.1 record
3. addUser pathstore && usermod -aG docker pathstore
```

That's all you need to do to setup a server to run a PathStore node.
 
 How to use the Admin Panel
 ---
Requirements:
1. JRE version of at least 11
2. Maven
3. At least one server is setup as the previous section describes

Most operating systems come with the latest Java JRE if you're downloading Maven.
 
Setting up a pathstore network can be done by running the following commands
 ```bash
mkdir -p /etc/pathstore // you will need administrator privilleges

// This step is optional but will be more convenient during initial setup
chown -R your_account:your_account /etc/pathstore // you will need administrator privileges

git clone https://github.com/delara/cloudpath

// This step is optional and only used for development / new release builds
git checkout $branch 

cd cloudpath/version_0.0.1/PathStore/pathstore-admin-panel
mvn package
java -jar target/pathstore-admin-panel-0.0.1-SNAPSHOT.jar
```
This will start the website. It will first prompt you if you want to create a new pathstore network. Follow the on screen
steps until it prompts you to provide the server settings where you will enter the ssh information to connect to the server
you want to have as the root node. Make sure when it prompts you to enter the branch you enter the same branch name that 
you checkout as pathstore re-clones the repository from github using the branch name you specify into the base docker 
container. After this initial root node deployment is setup it will write the website properties file to 
/etc/pathstore/pathstore.properties if you have permission to do so. Otherwise it will prompt you to add it manually. 
After this is done the website will start and you can navigate to http://localhost:8080 and your setup is complete.

API Endpoints
---
* TODO

Contributing to the website
---
* TODO