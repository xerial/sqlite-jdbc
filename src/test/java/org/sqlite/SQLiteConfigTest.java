package org.sqlite;

import static org.assertj.core.api.Assertions.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteConfig.Pragma;

public class SQLiteConfigTest {

    @Test
    public void toProperties() {
        SQLiteConfig config = new SQLiteConfig();

        config.setReadOnly(true);
        config.setDateStringFormat("yyyy/mm/dd");
        config.setDatePrecision("seconds");
        config.setDateClass("real");

        Properties properties = config.toProperties();

        assertThat(properties.getProperty(SQLiteConfig.Pragma.DATE_STRING_FORMAT.getPragmaName()))
                .isEqualTo("yyyy/mm/dd");
        assertThat(properties.getProperty(SQLiteConfig.Pragma.DATE_PRECISION.getPragmaName()))
                .isEqualTo(SQLiteConfig.DatePrecision.SECONDS.name());
        assertThat(properties.getProperty(SQLiteConfig.Pragma.DATE_CLASS.getPragmaName()))
                .isEqualTo(SQLiteConfig.DateClass.REAL.name());
    }

    @Test
    public void setBusyTimeout() {
        SQLiteConfig config = new SQLiteConfig();

        // verify the default is set in the pragma table and the cached value
        assertThat(config.toProperties().getProperty(SQLiteConfig.Pragma.BUSY_TIMEOUT.pragmaName))
                .isEqualTo("3000");
        assertThat(config.getBusyTimeout()).isEqualTo(3000);

        // verify that the default is updated in both places
        config.setBusyTimeout(1234);
        assertThat(config.toProperties().getProperty(SQLiteConfig.Pragma.BUSY_TIMEOUT.pragmaName))
                .isEqualTo("1234");
        assertThat(config.getBusyTimeout()).isEqualTo(1234);

        Properties properties = new Properties();
        properties.setProperty(SQLiteConfig.Pragma.BUSY_TIMEOUT.pragmaName, "100");
        config = new SQLiteConfig(properties);

        // verify that we can set an initial value other than the default
        assertThat(config.toProperties().getProperty(SQLiteConfig.Pragma.BUSY_TIMEOUT.pragmaName))
                .isEqualTo("100");
        assertThat(config.getBusyTimeout()).isEqualTo(100);
    }

    @Test
    public void pragmaSet() {
        Set<String> expectedPragmaSet = new HashSet<>();
        for (Pragma v : SQLiteConfig.Pragma.values()) {
            expectedPragmaSet.add(v.pragmaName);
        }

        assertThat(SQLiteConfig.pragmaSet).isEqualTo(expectedPragmaSet);
    }
}
