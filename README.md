![ServerJars](https://serverjars.com/assets/img/icon_small.png)

# ServerJars-Updater
An automatic updater for serverjars 

# New And Improved
Check out https://github.com/jacobtread/ServerUpdater for a new and improved version

# Important Notice
Any Issues will be ignored. The Songoda company screwed me over and I will no longer be writing code for them however I will leave this code up for anyone who wishes to modify it. 


You are looking at our brand new Automatic Updater which you can run like any normal jar on our website. However when there is a new version available either on the version you predefine or latest it will automaticly update your server to that version on each restart. The jar is ran in place of your usual jar, just make sure to configure the wanted version and jar type correctly though the serverjars.properties which is automatically created when you first run the jar. 

The newest version now supports java versions above 8 

(This was an issue with class loading)

There is a workaroud in place which will give you a reflection warning on newer versions of java
to get past this add ```-Djava.system.class.loader=com.serverjars.updater.ServerJarsLoader'``` to your
vm arguments

e.g
```
java -Djava.system.class.loader=com.serverjars.updater.ServerJarsLoader -jar Updater-2.1.jar
```
![Updater](https://cdn.discordapp.com/attachments/592847823796437023/734384246528933938/unknown.png)
