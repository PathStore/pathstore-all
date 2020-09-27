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
cd version_0.0.1/PathStore/pathstore-startup-utility
mvn package
java -jar target/main.jar
```

The first prompt will ask you for the directory that PathStore is in. Provide the utility with the absolute path on your machine to version_0.0.1/PathStore in the repository code

Follow the instructions on the screen and you will have deployed the first node in your network.

After this is complete you can naviagte to the ip address of the machine you provided during start up on port 8080 in your web browser of choice to make changes to your network (scale, application creation / deployment, health monitoring, etc)
