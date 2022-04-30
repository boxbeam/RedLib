# RedLib
RedLib is a Spigot plugin development library, designed to make your life easier and soothe the pain points of plugin development. Below, find instructions for the various components of RedLib.

Support Discord: https://discord.gg/agu5xGy2YZ

Docs: https://redempt.dev/javadoc/com/github/Redempt/RedLib/index.html

# Installation for Development

RedLib is a standalone plugin, but can also be used as a shaded dependency if you do not want to distribute RedLib directly. To use it as a plugin dependency, you must add it as a dependency in your plugin.yml:

```yaml
depend: [RedLib]
```

To get the jar, either download it from the releases tab either here on [GitHub](https://github.com/Redempt/RedLib/releases) or on [Spigot](https://www.spigotmc.org/resources/redlib.78713/), or [build it locally](https://github.com/Redempt/RedLib#build-locally).

## Gradle

```groovy
repositories {
        maven { url = 'https://redempt.dev' }
}

```

```groovy
dependencies {
        compileOnly 'com.github.Redempt:RedLib:Tag'
}
```

Replace `Tag` with a release tag for RedLib. You can see the latest version [here](https://github.com/Redempt/RedLib/releases/latest).

To shade RedLib, change the dependency from `compileOnly` to `implementation`, and install the [gradle shadow plugin](https://github.com/johnrengelman/shadow).

If you are having a problem while building, such as plugin.yml is duplicate, try setting duplicatesStrategy to DuplicatesStrategy.EXCLUDE.
```groovy
tasks {
        processResources {
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
}
```

## Maven:

```xml
<repository>
        <id>redempt.dev</id>
        <url>https://redempt.dev</url>
</repository>
```

```xml
<dependency>
        <groupId>com.github.Redempt</groupId>
        <artifactId>RedLib</artifactId>
        <version>Tag</version>
        <scope>provided</scope>
</dependency>
```
Replace `Tag` with a release tag for RedLib. You can see the latest version [here](https://github.com/Redempt/RedLib/releases/latest).

To shade RedLib, change the scope from `provided` to `compile`.

## Build locally:

For Windows, use Git Bash. For Linux or OSX, just ensure you have Git installed.Navigate to the directory where you want to clone the repository, and run:

```
git clone https://github.com/Redempt/RedLib
cd RedLib
./gradlew jar
```

After running these commands, the jar will be at `build/libs/RedLib.jar`.
You may also need to add the jar to your classpath. After that, you should be good to go!

# Usage

For info on how to use RedLib, please see the [wiki](https://github.com/Redempt/RedLib/wiki).
