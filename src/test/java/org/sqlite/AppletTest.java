package org.sqlite;

import java.awt.Graphics;
import java.awt.Image;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.JApplet;

/**
 * <p>
 * Title: UnsignedApplet
 * </p>
 * <p>
 * Description: Demo of an Unsigned applet's unsuccessfull attempt to retrieve
 * an image stored on a remote server
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003 Raditha Dissanayake
 * </p>
 * <p>
 * Company: Raditha Dissanayake
 * </p>
 * 
 * @author Raditha Dissanayake
 * @version 1.0
 */
public class AppletTest extends JApplet
{
    Image img;

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

            //g.drawImage(img, 10, 10, 50, 50, this);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}