package org.sqlite.architecture;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.are;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.core.domain.properties.HasOwner;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import java.sql.DriverManager;
import org.sqlite.util.OSInfo;

@AnalyzeClasses(
        packages = "org.sqlite",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class CodingRulesTest {

    @ArchTest
    void no_access_to_standard_streams(JavaClasses importedClasses) {
        NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(
                importedClasses.that(are(not(equivalentTo(OSInfo.class)))));
    }

    @ArchTest
    private final ArchRule no_generic_exceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest private final ArchRule no_jodatime = NO_CLASSES_SHOULD_USE_JODATIME;

    @ArchTest private final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    private final ArchRule no_driver_manager_println =
            noClasses()
                    .should(
                            ArchConditions.callMethodWhere(
                                    JavaCall.Predicates.target(HasName.Predicates.name("println"))
                                            .and(
                                                    JavaCall.Predicates.target(
                                                            HasOwner.Predicates.With.owner(
                                                                    JavaClass.Predicates
                                                                            .assignableTo(
                                                                                    DriverManager
                                                                                            .class))))));
}
