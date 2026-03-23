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

I am using

- Maven plugin v6.32.0
- rewrite-spring v6.26.0

## How are you running OpenRewrite?

<!--
Are you using the Maven plugin, Gradle plugin, Moderne CLI, Moderne.io or something else?
Is your project a single module or a multi-module project?

Can you share your configuration so that we can rule out any configuration issues?

Is your project public? If so, can you share a link to it?
Code snippets can also be shared privately via [our public Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA).
-->

I am using the Maven plugin, and my project is a single module project.

https://github.com/thethyka/issue-reproducing/tree/main
(mockwebserver-shutdown-issue)

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.32.0</version>
    <configuration>
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
```

## What is the smallest, simplest way to reproduce the problem?

<!--
Sometimes the logs indicate a recipe stumbled over a particular pattern of code.
If you can share a code snippet that reproduces the issue, that will help us fix it faster.
We also accept [pull requests that merely replicate an issue](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes), as a step up to a full fix.

A code snippet can be something simple like this, or similar for other languages:
-->

When running the Spring Boot upgrade recipes that upgrade OkHttp mockwebserver dependencies (from v4 to v5), okhttp3.mockwebserver.MockWebServer gets replaced with mockwebserver3.MockWebServer correctly, but the shutdown() method doesn't get rewritten to close().

```java
package com.example;

import okhttp3.mockwebserver.MockWebServer;
import java.io.IOException;

public class MockWebServerShutdownIssueTest {

    public void testServerClose() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Perform tests...

        mockWebServer.shutdown();
    }
}
```

## What did you expect to see?

The mockwebserver3.MockWebServer [usage](https://square.github.io/okhttp/5.x/mockwebserver3/mockwebserver3/-mock-web-server/index.html) dictates using close() since it implements Closeable. I expect the shutdown() calls to be updated to close().

<!-- A code snippet, or a description of the behavior you expected helps us write a test to ensure the issue is fixed. -->

```java
package com.example;

import mockwebserver3.MockWebServer;
import java.io.IOException;

public class MockWebServerShutdownIssueTest {

    public void testServerClose() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Perform tests...

        mockWebServer.close();
    }
}
```

## What did you see instead?

The dependency and import are rewritten correctly, but the compiler fails later because the shutdown() method call remains untouched.

<!-- A code snippet, or a description of the behavior you saw instead of the above expected result. -->

```java
package com.example;

import mockwebserver3.MockWebServer;
import java.io.IOException;

public class MockWebServerShutdownIssueTest {

    public void testServerClose() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Perform tests...

        mockWebServer.shutdown();
    }
}
```

## What is the full stack trace of any errors you encountered?

<!-- When errors occur, please include the output of `--stacktrace` for Gradle or `--debug` for Maven. -->

```
cannot find symbol
  symbol:   method shutdown()
  location: variable mockWebServer of type mockwebserver3.MockWebServer
```

## Are you interested in [contributing a fix to OpenRewrite](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes)?

<!-- Indicate if this is something you would like to work on, and how we can best support you in doing so. -->

Yes.
