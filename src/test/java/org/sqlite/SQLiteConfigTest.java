package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;

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
}
