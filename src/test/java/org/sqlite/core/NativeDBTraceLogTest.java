package org.sqlite.core;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

public class NativeDBTraceLogTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "pragma key = 'secret'",
                "PRAGMA hexkey='secret'",
                "pragma key = \"x'secret'\"",
                "pragma rekey='sec''ret'",
                "pragma main.key = 'secret'",
                "pragma key('secret')",
                "pragma textkey = secret",
                "Pragma HexReKey = 'secret'",
                "pragma textrekey = x'secret'"
            })
    public void redactSecretsHidesKeyValue(String sql) {
        String redacted = NativeDB.redactSecrets(sql);
        assertThat(redacted).doesNotContain("secret").doesNotContain("ret").contains("'***'");
    }

    @Test
    public void redactSecretsHandlesVeryLongKey() {
        String key = String.join("", java.util.Collections.nCopies(200_000, "a''b"));
        assertThat(NativeDB.redactSecrets("pragma key = '" + key + "'"))
                .isEqualTo("pragma key = '***'");
    }

    @Test
    public void redactSecretsKeepsPragmaNameAndFollowingStatements() {
        assertThat(NativeDB.redactSecrets("pragma key = 'secret'")).isEqualTo("pragma key = '***'");
        assertThat(NativeDB.redactSecrets("pragma key('secret')")).isEqualTo("pragma key('***')");
        assertThat(NativeDB.redactSecrets("pragma hexrekey = 'ab'; select 1"))
                .isEqualTo("pragma hexrekey = '***'; select 1");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "select key from t",
                "pragma journal_mode = wal",
                "pragma foreign_keys = on",
                "select 1 from sqlite_schema"
            })
    public void redactSecretsLeavesOtherStatementsUnchanged(String sql) {
        assertThat(NativeDB.redactSecrets(sql)).isEqualTo(sql);
    }

    @Test
    public void redactSecretsHandlesNull() {
        assertThat(NativeDB.redactSecrets(null)).isNull();
    }

    @Test
    public void traceLogDoesNotContainPassword() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(NativeDB.class);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            Properties props = new Properties();
            props.setProperty("password", "supersecret");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:", props);
                    Statement stat = conn.createStatement()) {
                stat.execute("pragma rekey = 'othersecret'");
            }
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }

        List<String> messages =
                appender.list.stream()
                        .map(ILoggingEvent::getFormattedMessage)
                        .collect(Collectors.toList());
        assertThat(messages).noneMatch(m -> m.contains("supersecret"));
        assertThat(messages).noneMatch(m -> m.contains("othersecret"));
        assertThat(messages).anyMatch(m -> m.contains("[SQLite EXEC] pragma key = '***'"));
        assertThat(messages).anyMatch(m -> m.contains("[SQLite EXEC] pragma rekey = '***'"));
    }
}
