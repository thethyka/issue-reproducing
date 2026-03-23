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

rewrite-maven-plugin v6.30.0
rewrite-spring v6.25.1

## How are you running OpenRewrite?

<!--
Are you using the Maven plugin, Gradle plugin, Moderne CLI, Moderne.io or something else?
Is your project a single module or a multi-module project?

Can you share your configuration so that we can rule out any configuration issues?

Is your project public? If so, can you share a link to it?
Code snippets can also be shared privately via [our public Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA).
-->

https://github.com/kthethy-coles/issue-reproducing

Maven plugin with POM:

```xml
<plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.30.0</version>
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
                            <version>6.25.1</version>
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

If we use an ObjectMapper double-brace initialization where setSerializationInclusion(..) is called with an implicit receiver inside the instance initializer.

```java
package com.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

class BraceInitIssue {
    void foo(Object dummyReq) throws Exception {
        var paramRequestString = new ObjectMapper() {{
            setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        }}.writeValueAsString(dummyReq);
    }
}
```

## What did you expect to see?

<!-- A code snippet, or a description of the behavior you expected helps us write a test to ensure the issue is fixed. -->

The recipe should complete without error (or gracefully skip if double brace init is not supported).

## What did you see instead?

Code crashed.

## What is the full stack trace of any errors you encountered?

<!-- When errors occur, please include the output of `--stacktrace` for Gradle or `--debug` for Maven. -->

```
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  15.146 s
[INFO] Finished at: 2026-03-05T16:53:13+11:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal org.openrewrite.maven:rewrite-maven-plugin:6.30.0:dryRun (default-cli) on project rewrite-boot4-npe-repro: Execution default-cli of goal org.openrewrite.maven:rewrite-maven-plugin:6.30.0:dryRun failed: Error while visiting src/main/java/com/example/BraceInitIssue.java: java.lang.NullPointerException: Cannot invoke "org.openrewrite.java.tree.J.withPrefix(org.openrewrite.java.tree.Space)" because "parameter" is null
[ERROR]   org.openrewrite.java.internal.template.Substitutions.substituteTypedPattern(Substitutions.java:143)
[ERROR]   org.openrewrite.java.internal.template.Substitutions.lambda$substitute$0(Substitutions.java:74)
[ERROR]   org.openrewrite.internal.PropertyPlaceholderHelper.parseStringValue(PropertyPlaceholderHelper.java:116)
[ERROR]   org.openrewrite.internal.PropertyPlaceholderHelper.replacePlaceholders(PropertyPlaceholderHelper.java:92)
[ERROR]   org.openrewrite.java.internal.template.Substitutions.substitute(Substitutions.java:61)
[ERROR]   org.openrewrite.java.JavaTemplate.doApply(JavaTemplate.java:123)
[ERROR]   org.openrewrite.java.JavaTemplate.apply(JavaTemplate.java:109)
[ERROR]   org.openrewrite.java.jackson.UpdateSerializationInclusionConfiguration$1.visitMethodInvocation(UpdateSerializationInclusionConfiguration.java:85)
[ERROR]   org.openrewrite.java.jackson.UpdateSerializationInclusionConfiguration$1.visitMethodInvocation(UpdateSerializationInclusionConfiguration.java:57)
[ERROR]   org.openrewrite.java.tree.J$MethodInvocation.acceptJava(J.java:4275)
[ERROR]   org.openrewrite.java.tree.J.accept(J.java:55)
[ERROR]   org.openrewrite.TreeVisitor.visit(TreeVisitor.java:242)
[ERROR]   org.openrewrite.TreeVisitor.visitAndCast(TreeVisitor.java:309)
[ERROR]   org.openrewrite.java.JavaVisitor.visitRightPadded(JavaVisitor.java:1310)
[ERROR]   org.openrewrite.java.JavaVisitor.lambda$visitBlock$4(JavaVisitor.java:392)
[ERROR]   org.openrewrite.internal.ListUtils.map(ListUtils.java:245)
[ERROR]   ...
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/PluginExecutionException
```

## Are you interested in [contributing a fix to OpenRewrite](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes)?

<!-- Indicate if this is something you would like to work on, and how we can best support you in doing so. -->

Yes.
