<pre>
 ________  ________  _________  ___  ___  ________  _________  ________  ________  _______      
|\   __  \|\   __  \|\___   ___\\  \|\  \|\   ____\|\___   ___\\   __  \|\   __  \|\  ___ \     
\ \  \|\  \ \  \|\  \|___ \  \_\ \  \\\  \ \  \___|\|___ \  \_\ \  \|\  \ \  \|\  \ \   __/|    
 \ \   ____\ \   __  \   \ \  \ \ \   __  \ \_____  \   \ \  \ \ \  \\\  \ \   _  _\ \  \_|/__  
  \ \  \___|\ \  \ \  \   \ \  \ \ \  \ \  \|____|\  \   \ \  \ \ \  \\\  \ \  \\  \\ \  \_|\ \ 
   \ \__\    \ \__\ \__\   \ \__\ \ \__\ \__\____\_\  \   \ \__\ \ \_______\ \__\\ _\\ \_______\
    \|__|     \|__|\|__|    \|__|  \|__|\|__|\_________\   \|__|  \|_______|\|__|\|__|\|_______|
                                            \|_________|                                        
</pre>
[![Actions Status](https://github.com/delara/cloudpath/workflows/package/badge.svg)](https://github.com/delara/cloudpath/actions)

# Usage

In order to startup a PathStore network you need to do the following:

```cmd
mvn package
java -jar pathstore-startup-utility/target/main.jar
```

The first prompt will ask you for the directory that PathStore is in. Provide the utility with the absolute path on your machine to the repository code (i.e. `/home/myles/cloudpath`)

Follow the instructions on the screen and you will have deployed the first node in your network.

After this is complete you can naviagte to the ip address of the machine you provided during start up on port 8080 in your web browser of choice to make changes to your network (scale, application creation / deployment, health monitoring, etc)

# Server Assumptions

Every machine that runs **any** pathstore code should have the following:

**Docker CE** (*Version*: Docker version 19.03.13, build 4484c46d9d)

```console
sudo apt-get install docker.io
```

**User account with docker group**

```console
# Create user with docker admin privileges
sudo adduser pathstore-installer
sudo usermod -aG docker pathstore-installer

# Creates the certs directory for the networks private registry
sudo mkdir -p /etc/docker/certs.d
sudo chown root:docker /etc/docker/certs.d
sudo chmod 775 /etc/docker/certs.d
```

**Maven** (*Version*: Apache Maven 3.6.0)

```console
sudo apt-get install maven
```

**Java** (*Version*: OpenJDK 11.0.6)

```console
sudo apt-get install openjdk-11-jdk
```


