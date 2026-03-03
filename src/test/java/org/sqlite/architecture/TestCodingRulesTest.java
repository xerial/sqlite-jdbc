package org.sqlite.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

@DisabledIfEnvironmentVariable(
        named = "SKIP_TEST_MULTIARCH",
        matches = "true",
        disabledReason = "Those tests don't need to run in multi-arch")
@DisabledInNativeImage
@AnalyzeClasses(
        packages = "org.sqlite",
        importOptions = {ImportOption.OnlyIncludeTests.class})
class TestCodingRulesTest {

    @ArchTest
    public static final ArchRule no_junit_assertions =
            noClasses()
                    .should()
                    .dependOnClassesThat()
                    .haveFullyQualifiedName("org.junit.jupiter.api.Assertions");
}
