# RedLib
RedLib is a Spigot plugin development library, designed to make your life easier and soothe the pain points of plugin development. Below, find instructions for the various components of RedLib.

# Installation

To build the RedLib jar, it is recommended to use Jitpack with Gradle or Maven if possible. If you don't want to do that, you can build the jar yourself manually. For Windows, use Git Bash. For Linux or OSX, just ensure you have Git installed. Navigate to the directory where you want to clone the repository, and run:

```
git clone https://github.com/Redempt/RedLib
cd RedLib
./gradlew jar
```

After running these commands, the jar will be at `build/libs/RedLib.jar`. RedLib is a standalone plugin, so you will need to install it on any servers that have plugins which depend on it, and specify it as a dependency in your plugin.yml like this:

`depend: [RedLib]`
You may also need to add the jar to your classpath. After that, you should be good to go!

# Usage

For info on how to use RedLib, please see the (wiki)[https://github.com/Redempt/RedLib/wiki].
