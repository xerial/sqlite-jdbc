package org.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        ResultSet rs = stat.executeQuery("select acos(0.5)");
        assertTrue(rs.next());
        assertEquals(1.0471975511966, rs.getDouble(1), 0.00000000000001);
        rs.close();
    }

    @Test
    public void acosh() throws Exception {
        ResultSet rs = stat.executeQuery("select acosh(10)");
        assertTrue(rs.next());
        assertEquals(2.99322284612638, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void asin() throws Exception {
        ResultSet rs = stat.executeQuery("select asin(0.5)");
        assertTrue(rs.next());
        assertEquals(0.523598775598299, rs.getDouble(1), 0.00000000000001);
        rs.close();
    }

    @Test
    public void asinh() throws Exception {
        ResultSet rs = stat.executeQuery("select asinh(10)");
        assertTrue(rs.next());
        assertEquals(2.99822295029797, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void atan() throws Exception {
        ResultSet rs = stat.executeQuery("select atan(1)");
        assertTrue(rs.next());
        assertEquals(0.785398163397448, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void atan2() throws Exception {
        ResultSet rs = stat.executeQuery("select atan2(1,5)");
        assertTrue(rs.next());
        assertEquals(0.197395559849881, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void atn2() throws Exception {
        ResultSet rs = stat.executeQuery("select atn2(1,5)");
        assertTrue(rs.next());
        assertEquals(0.197395559849881, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void atanh() throws Exception {
        ResultSet rs = stat.executeQuery("select atanh(0.5)");
        assertTrue(rs.next());
        assertEquals(0.549306144334055, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void ceil() throws Exception {
        ResultSet rs = stat.executeQuery("select ceil(0.5)");
        assertTrue(rs.next());
        assertEquals(1.0, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void cos() throws Exception {
        ResultSet rs = stat.executeQuery("select cos(radians(45))");
        assertTrue(rs.next());
        assertEquals(0.707106781186548, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void cosh() throws Exception {
        ResultSet rs = stat.executeQuery("select cosh(0.5)");
        assertTrue(rs.next());
        assertEquals(1.12762596520638, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void cot() throws Exception {
        ResultSet rs = stat.executeQuery("select cot(0.5)");
        assertTrue(rs.next());
        assertEquals(1.830487721712452, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void coth() throws Exception {
        ResultSet rs = stat.executeQuery("select coth(0.5)");
        assertTrue(rs.next());
        assertEquals(2.163953413738653, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void degrees() throws Exception {
        ResultSet rs = stat.executeQuery("select degrees(pi()/2)");
        assertTrue(rs.next());
        assertEquals(90.0, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void exp() throws Exception {
        ResultSet rs = stat.executeQuery("select exp(1)");
        assertTrue(rs.next());
        assertEquals(2.71828182845904, rs.getDouble(1), 0.00000000000001);
        rs.close();
    }

    @Test
    public void floor() throws Exception {
        ResultSet rs = stat.executeQuery("select floor(1.5)");
        assertTrue(rs.next());
        assertEquals(1.0, rs.getDouble(1));
        rs.close();
    }

    @Test
    // this actually performs ln()
    public void log() throws Exception {
        ResultSet rs = stat.executeQuery("select log(2)");
        assertTrue(rs.next());
        assertEquals(0.693147180559945, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void log10() throws Exception {
        ResultSet rs = stat.executeQuery("select log10(10)");
        assertTrue(rs.next());
        assertEquals(1, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void pi() throws Exception {
        ResultSet rs = stat.executeQuery("select pi()");
        assertTrue(rs.next());
        assertEquals(3.141592653589793115997963468544185161590576171875, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void power() throws Exception {
        ResultSet rs = stat.executeQuery("select power(10,2)");
        assertTrue(rs.next());
        assertEquals(100, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void radians() throws Exception {
        ResultSet rs = stat.executeQuery("select radians(45)");
        assertTrue(rs.next());
        assertEquals(0.785398163397448, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void sin() throws Exception {
        ResultSet rs = stat.executeQuery("select sin(radians(30))");
        assertTrue(rs.next());
        assertEquals(0.5, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void sinh() throws Exception {
        ResultSet rs = stat.executeQuery("select sinh(0.5)");
        assertTrue(rs.next());
        assertEquals(0.521095305493747, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void sqrt() throws Exception {
        ResultSet rs = stat.executeQuery("select sqrt(4)");
        assertTrue(rs.next());
        assertEquals(2, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void square() throws Exception {
        ResultSet rs = stat.executeQuery("select square(4)");
        assertTrue(rs.next());
        assertEquals(16, rs.getDouble(1));
        rs.close();
    }

    @Test
    public void tan() throws Exception {
        ResultSet rs = stat.executeQuery("select tan(0.5)");
        assertTrue(rs.next());
        assertEquals(0.54630248984379, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }

    @Test
    public void tanh() throws Exception {
        ResultSet rs = stat.executeQuery("select tanh(0.5)");
        assertTrue(rs.next());
        assertEquals(0.46211715726001, rs.getDouble(1), 0.000000000000001);
        rs.close();
    }
}
