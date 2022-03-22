package org.sqlite;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import java.io.File;
import java.util.Optional;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class ExecutionConditionForExtention implements ExecutionCondition {
    private static final ConditionEvaluationResult DEFAULT = disabled(
            ExecutionConditionForExtention.class + " is not present"
    );

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Optional<SqliteExtention> sqliteExtention = findAnnotation(context.getElement(), SqliteExtention.class);
        if (sqliteExtention.isPresent()) {
            SqliteExtention sqliteExtentionInstance = sqliteExtention.get();
            String value = sqliteExtentionInstance.value();
            File file = new File(value);
            String absolutePath = file.getAbsolutePath();
            if (file.exists()) {
                return ConditionEvaluationResult.enabled(String.format("%s is present.  Test enabled. ", absolutePath));
            }
            return disabled(String.format("%s is not present, therefore test will be disabled.", absolutePath));
        }
        return disabled(" no ext source file path to evaluate this test condition was provided in the annotation; therefore, test will be disabled by definition.");

    }
}