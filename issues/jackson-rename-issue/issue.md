<!--
Thank you for reporting an issue with OpenRewrite!
We appreciate you taking the time to help us improve.
Please fill out the template below to help us understand and reproduce the issue.
Feel free to delete any sections that don't apply to your issue.
-->

## What version of OpenRewrite are you using?

<!--
Whenever possible please try to replicate your issue with the latest versions of OpenRewrite.
The latest major and minor versions of OpenRewrite projects are usually listed here:
https://docs.openrewrite.org/reference/latest-versions-of-every-openrewrite-module
For patch releases check the GitHub Releases page for the respective project.

We release every few weeks, so it's possible that your issue has already been fixed.

If you want to try the most recent changes that haven't been fully released yet, you can check out our snapshot releases
https://docs.openrewrite.org/reference/snapshot-instructions
-->

rewrite-maven-plugin v6.34.0
rewrite-spring v6.27.1
rewrite-jackson v1.19.1

## How are you running OpenRewrite?

<!--
Are you using the Maven plugin, Gradle plugin, Moderne CLI, Moderne.io or something else?
Is your project a single module or a multi-module project?

Can you share your configuration so that we can rule out any configuration issues?

Is your project public? If so, can you share a link to it?
Code snippets can also be shared privately via [our public Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA).
-->

https://github.com/thethyka/issue-reproducing

Maven plugin, single module. POM:

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.34.0</version>
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
            <version>6.27.1</version>
        </dependency>
    </dependencies>
</plugin>
```

## What is the smallest, simplest way to reproduce the problem?

<!--
Sometimes the logs indicate a recipe stumbled over a particular pattern of code.
If you can share a code snippet that reproduces the issue, that will help us fix it faster.
We also accept [pull requests that merely replicate an issue](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes), as a step up to a full fix.

A code snippet can be something simple like this, or similar for other languages:
-->

When calling `UpgradeSpringBoot_4_0` (which includes `UpgradeJackson_2_3` via `UpgradeSpringFramework_7_0`) on code that references `com.fasterxml.jackson.databind.ser.std.JsonValueSerializer`, the recipe correctly
migrates the package prefix (`com.fasterxml.jackson` → `tools.jackson`) via `UpgradeJackson_2_3_PackageChanges`, but does **not** rename the class itself from `JsonValueSerializer` to `ObjectValueSerializer`.
The resulting import `tools.jackson.databind.ser.std.JsonValueSerializer` does not exist in Jackson 3.

Source file (with `jackson-databind 2.18.3` and `spring-boot-autoconfigure 3.4.3` on the classpath):

```java
package com.example;

import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

public class JacksonRenameIssue {

    static class MyCustomizer implements Jackson2ObjectMapperBuilderCustomizer {
        @Override
        public void customize(Jackson2ObjectMapperBuilder builder) {
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    static JsonValueSerializer createSerializer() {
        return null;
    }
}
```

## What did you expect to see?

The recipe should rename `JsonValueSerializer` to `ObjectValueSerializer` (in addition to moving the package):

```diff
-import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
+import tools.jackson.databind.ser.std.ObjectValueSerializer;

-    static JsonValueSerializer createSerializer() {
+    static ObjectValueSerializer createSerializer() {
```

Similarly, `Jackson2ObjectMapperBuilderCustomizer` should be renamed to `JsonMapperBuilderCustomizer` if that rename is part of the Spring Boot 4 migration.

## What did you see instead?

Running `mvn -pl issues/jackson-rename-issue rewrite:dryRun` produces the following patch:

```diff
-import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
+import tools.jackson.databind.ser.std.JsonValueSerializer;
```

The class name `JsonValueSerializer` is left unchanged. The resulting import
`tools.jackson.databind.ser.std.JsonValueSerializer` does not exist in Jackson 3 —
the correct Jackson 3 type is `tools.jackson.databind.ser.std.ObjectValueSerializer`.

`Jackson2ObjectMapperBuilderCustomizer` and its usage are likewise untouched by any recipe.

Full patch from `target/rewrite/rewrite.patch`:

```diff
diff --git a/issues/jackson-rename-issue/pom.xml b/issues/jackson-rename-issue/pom.xml
@@ -19,17 +19,17 @@
         <dependency>
             <groupId>org.springframework.boot</groupId>
             <artifactId>spring-boot-autoconfigure</artifactId>
-            <version>3.4.3</version>
+            <version>4.0.4</version>
         </dependency>
         <dependency>
             <groupId>org.springframework</groupId>
             <artifactId>spring-web</artifactId>
-            <version>6.2.3</version>
+            <version>7.0.6</version>
         </dependency>
         <dependency>
-            <groupId>com.fasterxml.jackson.core</groupId>
+            <groupId>tools.jackson.core</groupId>
             <artifactId>jackson-databind</artifactId>
-            <version>2.18.3</version>
+            <version>3.0.4</version>
         </dependency>

diff --git a/issues/jackson-rename-issue/src/main/java/com/example/JacksonRenameIssue.java b/...
@@ -1,6 +1,6 @@
 package com.example;

-import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
+import tools.jackson.databind.ser.std.JsonValueSerializer;
 import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
 import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@@ -37,7 +37,7 @@
         public void customize(Jackson2ObjectMapperBuilder builder) {
             builder.featuresToDisable(
-                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
+                    tools.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

## Root cause analysis

`UpgradeJackson_2_3_PackageChanges` uses `ChangePackage` to rewrite
`com.fasterxml.jackson.databind` → `tools.jackson.databind` recursively, which handles the package relocation correctly.

However, `UpgradeJackson_2_3_TypeChanges` has no `ChangeType` entry for:

```yaml
- org.openrewrite.java.ChangeType:
    oldFullyQualifiedTypeName: com.fasterxml.jackson.databind.ser.std.JsonValueSerializer
    newFullyQualifiedTypeName: tools.jackson.databind.ser.std.ObjectValueSerializer
```

Because `ChangePackage` runs after `ChangeType`, the package rename fires but the class rename is never applied, leaving code that references a non-existent Jackson 3 type.

Additional renames that may also be missing (to be confirmed against the Jackson 3 source):

| Jackson 2 type (com.fasterxml.jackson.databind) | Jackson 3 type (tools.jackson.databind) |
| ----------------------------------------------- | --------------------------------------- |
| `ser.std.JsonValueSerializer`                   | `ser.std.ObjectValueSerializer`         |
| `deser.std.JsonValueDeserializer`               | `deser.std.ObjectValueDeserializer`     |

And for the Spring Boot migration:

| Spring Boot 3 type                                                                     | Spring Boot 4 type                                                           |
| -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer` | `org.springframework.boot.autoconfigure.jackson.JsonMapperBuilderCustomizer` |
