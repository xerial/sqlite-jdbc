package org.sqlite.architecture;

import static com.tngtech.archunit.library.GeneralCodingRules.*;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "org.sqlite",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class CodingRulesTest {

    //    Disabled for now, see https://github.com/xerial/sqlite-jdbc/issues/802
    //    @ArchTest
    private final ArchRule no_access_to_standard_streams =
            NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    //    Disabled for now
    //    @ArchTest
    private final ArchRule no_generic_exceptions = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest private final ArchRule no_jodatime = NO_CLASSES_SHOULD_USE_JODATIME;

    @ArchTest private final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
