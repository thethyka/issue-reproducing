# OpenRewrite Issue Reproduction

This repository is a multi-module Maven project designed to isolate and reproduce various issues encountered when running [OpenRewrite](https://docs.openrewrite.org/). 

Each issue is isolated within its own Maven module under the `issues/` directory, making it easy to test specific recipes against specific code structures without interference.

## How to Create a New Module

To easily create a new module for reproducing an issue, follow these steps:

1. **Create the module directory and structure:**
   Create a new folder under the `issues/` directory. For example, `issues/my-new-issue`.
   Inside it, create the standard Maven source structure: `src/main/java/com/example/` (and `src/test/java/` if needed).

2. **Create the module's `pom.xml`:**
   In your new module folder (`issues/my-new-issue/pom.xml`), add a basic POM that points to the parent:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
       <modelVersion>4.0.0</modelVersion>
       
       <parent>
           <groupId>com.example</groupId>
           <artifactId>issue-reproducing-parent</artifactId>
           <version>1.0-SNAPSHOT</version>
           <relativePath>../../pom.xml</relativePath>
       </parent>
       
       <artifactId>my-new-issue</artifactId>
       <packaging>jar</packaging>

       <build>
           <plugins>
               <plugin>
                   <groupId>org.openrewrite.maven</groupId>
                   <artifactId>rewrite-maven-plugin</artifactId>
                   <version>6.32.0</version>
                   <configuration>
                       <exportDatatables>true</exportDatatables>
                       <activeRecipes>
                           <recipe>org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0</recipe>
                       </activeRecipes>
                   </configuration>
                   <dependencies>
                       <dependency>
                           <groupId>org.openrewrite.recipe</groupId>
                           <artifactId>rewrite-spring</artifactId>
                           <version>6.26.0</version>
                       </dependency>
                   </dependencies>
               </plugin>
           </plugins>
       </build>
   </project>
   ```

3. **Register the module in the root `pom.xml`:**
   Open the root `pom.xml` and add your new module to the `<modules>` section:
   ```xml
   <modules>
       <module>issues/brace-init-issue</module>
       <module>issues/my-new-issue</module> <!-- Add your module here -->
   </modules>
   ```

4. **Add your code:**
   Place the Java file(s) that reproduce the issue into the `src/main/java/...` folder of your new module.

## Running OpenRewrite

You can run OpenRewrite by targeting specific modules with `-pl`. This is the recommended pattern to keep isolated dependency graphs and ensure only the expected recipes run:

```bash
# Run dry-run for a specific issue
mvn -pl issues/mockwebserver-shutdown-issue rewrite:dryRun

# Apply the changes for a specific issue
mvn -pl issues/mockwebserver-shutdown-issue rewrite:run
``` 