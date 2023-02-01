package org.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

@DisabledInNativeImage // assertj Assumptions do not work in native-image tests
public class MathFunctionsTest {
    private Connection conn;
    private Statement stat;

    @BeforeEach
    public void connect() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        stat = conn.createStatement();
    }

    @AfterEach
    public void close() throws SQLException {
        stat.close();
        conn.close();
    }

    @Test
    public void acos() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select acos(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1.0471975511966, offset(0.00000000000001));
        rs.close();
    }

    @Test
    public void acosh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select acosh(10)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(2.99322284612638, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void asin() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select asin(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.523598775598299, offset(0.00000000000001));
        rs.close();
    }

    @Test
    public void asinh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select asinh(10)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(2.99822295029797, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void atan() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select atan(1)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.785398163397448, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void atan2() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select atan2(1,5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.197395559849881, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void atn2() throws Exception {
        Utils.assumeJdbcExtensions(conn);
        ResultSet rs = stat.executeQuery("select atn2(1,5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.197395559849881, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void atanh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select atanh(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.549306144334055, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void ceil() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select ceil(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1.0, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void cos() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select cos(radians(45))");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.707106781186548, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void cosh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select cosh(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1.12762596520638, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void cot() throws Exception {
        Utils.assumeJdbcExtensions(conn);
        ResultSet rs = stat.executeQuery("select cot(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1.830487721712452, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void coth() throws Exception {
        Utils.assumeJdbcExtensions(conn);
        ResultSet rs = stat.executeQuery("select coth(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(2.163953413738653, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void degrees() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select degrees(pi()/2)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(90.0, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void exp() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select exp(1)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(2.71828182845904, offset(0.00000000000001));
        rs.close();
    }

    @Test
    public void floor() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select floor(1.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1.0, offset(0.000000000000001));
        rs.close();
    }

    @Test
    // with the old math extension functions, log would perform ln instead of log10
    public void logAsLn() throws Exception {
        Utils.assumeJdbcExtensionsWithoutMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select log(2)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.30102999566398114, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void ln() throws Exception {
        Utils.assumeMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select ln(2)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.693147180559945, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void logBase() throws Exception {
        Utils.assumeMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select log(3,3)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void log2() throws Exception {
        Utils.assumeMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select log2(2)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(1, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void log10() throws Exception {
        Utils.assumeMathFunctions(conn);
        {
            ResultSet rs = stat.executeQuery("select log10(10)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(1, offset(0.000000000000001));
            rs.close();
        }
        {
            ResultSet rs = stat.executeQuery("select log(10)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(1, offset(0.000000000000001));
            rs.close();
        }
    }

    @Test
    public void mod() throws Exception {
        Utils.assumeMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select mod(11,3.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isEqualTo(0.5);
        rs.close();
    }

    @Test
    public void pi() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select pi()");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1))
                .isCloseTo(
                        3.141592653589793115997963468544185161590576171875,
                        offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void power() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        {
            ResultSet rs = stat.executeQuery("select pow(10,2)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(100, offset(0.000000000000001));
            rs.close();
        }
        {
            ResultSet rs = stat.executeQuery("select power(10,2)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(100, offset(0.000000000000001));
            rs.close();
        }
    }

    @Test
    public void radians() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select radians(45)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.785398163397448, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void sin() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select sin(radians(30))");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.5, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void sinh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select sinh(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.521095305493747, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void sqrt() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select sqrt(4)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(2, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void square() throws Exception {
        Utils.assumeJdbcExtensions(conn);
        ResultSet rs = stat.executeQuery("select square(4)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(16, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void tan() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select tan(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.54630248984379, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void tanh() throws Exception {
        Utils.assumeJdbcExtensionsOrMathFunctions(conn);
        ResultSet rs = stat.executeQuery("select tanh(0.5)");
        assertThat(rs.next()).isTrue();
        assertThat(rs.getDouble(1)).isCloseTo(0.46211715726001, offset(0.000000000000001));
        rs.close();
    }

    @Test
    public void trunc() throws Exception {
        Utils.assumeMathFunctions(conn);
        {
            ResultSet rs = stat.executeQuery("select trunc(1.5)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(1, offset(0.000000000000001));
            rs.close();
        }
        {
            ResultSet rs = stat.executeQuery("select trunc(-1.5)");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getDouble(1)).isCloseTo(-1, offset(0.000000000000001));
            rs.close();
        }
    }
}
