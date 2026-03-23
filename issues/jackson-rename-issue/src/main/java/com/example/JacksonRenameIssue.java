package com.example;

import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Reproduces missing class renames when running UpgradeSpringBoot_4_0 for
 * the Jackson 2 -> 3 migration.
 *
 * The recipe chain includes org.openrewrite.java.jackson.UpgradeJackson_2_3
 * via spring-framework-70 -> UpgradeJackson_2_3_PackageChanges, which migrates
 * the com.fasterxml.jackson package to tools.jackson. However, it does NOT
 * handle the following class-level renames:
 *
 *   (1) JsonValueSerializer  -> ObjectValueSerializer
 *         com.fasterxml.jackson.databind.ser.std.JsonValueSerializer
 *      -> tools.jackson.databind.ser.std.ObjectValueSerializer
 *
 *   (2) Jackson2ObjectMapperBuilderCustomizer -> JsonMapperBuilderCustomizer
 *         org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
 *      -> org.springframework.boot.autoconfigure.jackson.JsonMapperBuilderCustomizer
 *
 * Expected: Both type names and imports are correctly migrated to the new names.
 * Actual:   The import packages are updated (com.fasterxml -> tools.jackson) but
 *           the class names themselves are left unchanged, producing imports that
 *           do not exist in Jackson 3 / Spring Boot 4.
 *
 * Run: mvn -pl issues/jackson-rename-issue rewrite:dryRun
 */
public class JacksonRenameIssue {

    // (1) Jackson2ObjectMapperBuilderCustomizer should be renamed to JsonMapperBuilderCustomizer
    //     in Spring Boot 4 (the "Jackson2" prefix is dropped since Jackson 3 removes that
    //     naming distinction).
    static class MyCustomizer implements Jackson2ObjectMapperBuilderCustomizer {
        @Override
        public void customize(Jackson2ObjectMapperBuilder builder) {
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    // (2) JsonValueSerializer should be renamed to ObjectValueSerializer in Jackson 3.
    //     The UpgradeJackson_2_3_PackageChanges recipe migrates the package
    //     (com.fasterxml.jackson.databind -> tools.jackson.databind) but does NOT
    //     rename the class itself, leaving a broken import in Jackson 3.
    static JsonValueSerializer createSerializer() {
        return null;
    }
}
