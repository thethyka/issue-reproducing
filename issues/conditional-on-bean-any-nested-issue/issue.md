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

OpenRewrite Maven plugin org.openrewrite.maven:rewrite-maven-plugin:6.32.0
Recipe module org.openrewrite.recipe:rewrite-spring:6.26.0

## How are you running OpenRewrite?

<!--
Are you using the Maven plugin, Gradle plugin, Moderne CLI, Moderne.io or something else?
Is your project a single module or a multi-module project?

Can you share your configuration so that we can rule out any configuration issues?

Is your project public? If so, can you share a link to it?
Code snippets can also be shared privately via [our public Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA).
-->

I am using the Maven plugin in a Maven project.

Public repro repo: [https://github.com/thethyka/issue-reproducing](https://github.com/thethyka/issue-reproducing)
(conditional-on-bean-any-nested-issue)

Recipe being applied: [https://docs.openrewrite.org/recipes/java/spring/boot2/conditionalonbeananynestedcondition](https://docs.openrewrite.org/recipes/java/spring/boot2/conditionalonbeananynestedcondition)

It is pulled in as part of UpgradeSpringBoot_4_0 via the nested Spring Boot upgrade chain.

```xml
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
```

## What is the smallest, simplest way to reproduce the problem?

<!--
Sometimes the logs indicate a recipe stumbled over a particular pattern of code.
If you can share a code snippet that reproduces the issue, that will help us fix it faster.
We also accept [pull requests that merely replicate an issue](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes), as a step up to a full fix.

A code snippet can be something simple like this, or similar for other languages:
-->

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FooService {}
class BarService {}
class MyBean {}

@Configuration
class ConditionalOnBeanAnyNestedIssue {

    @Bean
    @ConditionalOnBean({FooService.class, BarService.class})
    public MyBean myBean() {
        return new MyBean();
    }
}
```

Then run the Spring Boot 4 migration recipe.

- Related Spring Boot behavior change: [https://github.com/spring-projects/spring-boot/issues/5279](https://github.com/spring-projects/spring-boot/issues/5279)

## What did you expect to see?

No semantic rewrite here for code that is already on Spring Boot 2.x+.

@ConditionalOnBean({FooService.class, BarService.class}) already means AND on Boot 2.x+, so this should remain unchanged.

<!-- A code snippet, or a description of the behavior you expected helps us write a test to ensure the issue is fixed. -->

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class FooService {}
class BarService {}
class MyBean {}

@Configuration
class ConditionalOnBeanAnyNestedIssue {

    @Bean
    @ConditionalOnBean({FooService.class, BarService.class})
    public MyBean myBean() {
        return new MyBean();
    }
}
```

## What did you see instead?

OpenRewrite rewrites it to AnyNestedCondition, which changes the logic to OR:

<!-- A code snippet, or a description of the behavior you saw instead of the above expected result. -->

```java
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

class FooService {}
class BarService {}
class MyBean {}

@Configuration
class ConditionalOnBeanAnyNestedIssue {

    @Bean
    @Conditional(ConditionBarServiceOrFooService.class)
    public MyBean myBean() {
        return new MyBean();
    }

    private static class ConditionBarServiceOrFooService extends AnyNestedCondition {
        ConditionBarServiceOrFooService() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnBean(BarService.class)
        class BarServiceCondition {
        }

        @ConditionalOnBean(FooService.class)
        class FooServiceCondition {
        }
    }
}
```

## Are you interested in [contributing a fix to OpenRewrite](https://github.com/openrewrite/.github/blob/main/CONTRIBUTING.md#contributing-fixes)?

<!-- Indicate if this is something you would like to work on, and how we can best support you in doing so. -->

Yes
