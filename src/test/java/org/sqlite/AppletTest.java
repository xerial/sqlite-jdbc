package org.sqlite;

import java.awt.Color;
import java.awt.Graphics;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JApplet;

/**
 * A demo for using SQLite JDBC inside a Java Applet.
 * 
 * @author leo
 * 
 */
public class AppletTest extends JApplet
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public AppletTest()
    {
        this.setSize(200, 200);
    }

    @SuppressWarnings("unchecked")
    public void init()
    {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run()
            {
                try
                {
                    Class.forName("org.sqlite.JDBC");
                }
                catch (ClassNotFoundException e)
                {
                    e.printStackTrace();
                }

                return null;
            }
        });
    }

    public void paint(Graphics g)
    {
        try
        {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("create table sample(id, name)");
            stmt.executeUpdate("insert into sample values(1, \"leo\")");
            stmt.executeUpdate("insert into sample values(2, \"yui\")");

            int yOffset = 50;
            ResultSet rs = stmt.executeQuery("select * from sample");
            while (rs.next())
            {
                int id = rs.getInt(1);
                String name = rs.getString(2);
                g.drawString(String.format("id=%d, name=%s", id, name), 10, yOffset);
                yOffset += 50;
            }

            stmt.close();
            conn.close();

            g.setColor(Color.DARK_GRAY);
            g.drawRect(5, 5, 180, 180);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}