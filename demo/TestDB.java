import java.sql.*;
import java.io.*;
import java.util.*;

class TestDB
{
  public static void main(String args[]){
    try
    {
      runTest();
    }
    catch(SQLException ex)
    {
       while(ex!= null)
       {
         ex.printStackTrace();
         ex = ex.getNextException();
         
       }
      
    }
    catch(IOException ex)
    {
        ex.printStackTrace();
      
    }
  }
}

public static void runTest()throws SQLException ,IOException
{
  Connection conn = getConnection();
  try{
  Statement stat = conn.createStatement();
  stat.execute("CREATE TABLE Greetings (Message CHAR(20))");
  stat.execute("INSERT INTO Greetings VALUES ('Hello,World!')");
  ResultSet result = stat.executeQuerey("SELECT*FROM Greetings");
  result.next();
  System.out.println(result.getString(1));
  stat.execute("DROP TABLE Greetings");
  }
  finally{
  conn.close();
  }
}


public static Connection getConnection()throws SQLException,IOException
{
 Properties props = new Properties();
 FileInputStream in = new FileInputStream("database.properties");
 props.load(in);
 in.close();
 
 String drivers = props.getProperty("jdbc.drivers");
    if(drivers!=null)
    System.setProperty("jdbc.drivers",drivers);
    String url = props.getProperty("jdbc.url");
    String username = props.getProperty("jdbc.username ");
    String password = props.getProperty("jdbc.password");
    
    return DriverManager.getConnection(url , username , password);
    }
    
    
}
