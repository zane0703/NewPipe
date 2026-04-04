# NewPipe Desktop
<p align="center">
  <img src="app/src/main/resources/ic_launcher.webp" />
</p>
An unofficial desktop port of the NewPipe Android application, built for Windows, macOS, and Linux using Java.

# Project Overview

This project is a desktop implementation of the NewPipemobile experience. It is important to note that this is not a direct UI port. Because Android’s UI framework and Java Swing are fundamentally different APIs, the interface has been rebuilt from the ground up for a desktop-native experience.

## How it Works

This port is made possible by the modular architecture of the original project. While the UI is entirely new, this application utilizes the [NewPipe Extractor](https://github.com/teamnewpipe/newpipeextractor) as the core engine



# Prerequisites

Before running building the project, ensure you have the following installed on your system:

- Java 25 (JDK 25 if want to build the project yourself)
- VLC Media Player: The application relies on VLC for media playback functionality
  
# 📦 Downloads (Pre-built Files)
You can download the latest pre-built versions of the application from the Releases section of the repository:
👉 [Download Pre-built Assets](https://github.com/zane0703/NewPipe/releases)

# Build Guide
1. ***Clone the Repository:***
```sh
git clone https://github.com/zane0703/NewPipe
cd NewPipe
```

2. ***Build the Shadow JAR:*** Run the following command in your terminal to create an executable "fat" JAR containing all dependencies:<br/>
Windows
```bat
.\gradlew.bat shadowJar
```
Unix(Linux, Mac)
```sh
./gradlew shadowJar
```

3. ***Locate the Executable:*** Once the build is successful, your executable JAR will be located at:
<mark>app/build/libs/app-all.jar</mark>

# CLI
this application support  command line argument and flag for example you to launch a specific YouTube video immediately, use the following command:
```sh
java -jar newpipe.jar https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

To view a full list of supported arguments, configuration flags, and available options, use the help command:
```sh
java -jar newpipe.jar -h
```

# FAQ
- There already FreeTube why develop a desktop port of NewPipe?
  - I hate electron
- Is this a fork?
  - No, because Java desktop and android framework are completely different but the backend engine is based on NewPipe Extractor
- Do you look at NewPipe source code to develop this port?
  - Nope, the difference in framework API is too much that it easily to write from scratch but the UI design is are inspired from the original NewPipe
# Disclaimer

This is an unofficial fan-made project. It is not affiliated with, authorized, maintained, sponsored, or endorsed by the original NewPipe developers.
