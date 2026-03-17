package com.example;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reproduces: ConditionalOnBeanAnyNestedCondition recipe incorrectly changes AND to OR semantics.
 *
 * On Spring Boot 2.x+, @ConditionalOnBean({A.class, B.class}) means AND:
 *   - The bean is only created if BOTH FooService AND BarService are present.
 *
 * The recipe (org.openrewrite.java.spring.boot2.ConditionalOnBeanAnyNestedCondition) converts
 * this to AnyNestedCondition, which means OR:
 *   - The bean would be created if EITHER FooService OR BarService is present.
 *
 * This silently changes the application's conditional logic when upgrading a project
 * that is already on Spring Boot 2.x+.
 *
 * Expected: The recipe should NOT apply to projects already on Spring Boot 2.x+,
 * since @ConditionalOnBean already has AND semantics in 2.x+.
 *
 * Actual: The recipe converts AND semantics to OR (AnyNestedCondition), breaking the logic.
 *
 * Run: mvn -pl issues/conditional-on-bean-any-nested-issue rewrite:dryRun
 */
class FooService {}

class BarService {}

class MyBean {}

@Configuration
class ConditionalOnBeanAnyNestedIssue {

    /**
     * This bean should only be created when BOTH FooService AND BarService exist (AND semantics).
     * After the recipe runs, it will be created when EITHER exists (OR semantics) — wrong!
     */
    @Bean
    @ConditionalOnBean({FooService.class, BarService.class})
    public MyBean myBean() {
        return new MyBean();
    }
}
