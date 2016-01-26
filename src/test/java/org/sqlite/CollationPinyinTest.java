package org.sqlite;

import static org.junit.Assert.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CollationPinyinTest {
	
	//NAME, REMARK, TYPE
	private static final String[][] VALUES = {
			{"大型仓库","这是个仓库", "仓库"},	//1
			{"小型仓库","仓库啊", "仓库"},		//2
			{"仓库A","小仓库", "仓库"},		//3
			{"仓库B","小仓库", "仓库"},		//4
			{"办公室_1","办公大楼", "办公室"},	//5
			{"办公室_2","办公大楼", "办公室"},	//6
			{"会议室","办公大楼", "会议室"},	//7
			{"X会议室","办公大楼", "会议室"},	//8
			{"一会议室","办公大楼", "会议室"},	//9
			{"一仓库","办公大楼", "仓库"},		//10
			{"三实验室","办公大楼", "实验室"},	//11
	};
	private static final Integer[] NAME_ASC ={
		8,5,6,3,4,1,7,11,2,10,9
	};
	private static final Integer[] NAME_DESC ={
		9,10,2,11,7,1,4,3,6,5,8
	};
	private static final Integer[] TYPE_ASC_NAME_DESC ={
		6,5,10,2,1,4,3,9,7,8,11
	};
	
	
	String dbName;
	
	@Before
	public void setup() throws Exception
	{
		File tmpFile = File.createTempFile("pinyin-sqlite", ".db");
		tmpFile.deleteOnExit();
		dbName = tmpFile.getAbsolutePath();
		createDB();
	}

	@After
	public void tearDown() throws Exception {

	}
	
	private Connection getConnect() throws SQLException {
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName+"?foreign_keys=true&pinyin=true");
		//Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName);
		return conn;
	}
	
	private void createDB() throws SQLException{
		
		Connection conn = getConnect();
		try{
			Statement st = conn.createStatement();
			st.executeUpdate("CREATE TABLE `tlocation` ("
				+ "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT ,"
				+ "`name` varchar(200) NOT NULL UNIQUE,"
				+ "`remark` varchar(1000) DEFAULT NULL,"
				+ "`type` varchar(40) NOT NULL);");
			st.close();
			
			conn.setAutoCommit(false);
			PreparedStatement preSt = conn.prepareStatement(
				"INSERT INTO tlocation (name, remark, type) values (?, ?, ?)");
			
			for(String[] v : VALUES){
				preSt.setString(1, v[0]);
				preSt.setString(2, v[1]);
				preSt.setString(3, v[2]);
				preSt.execute();
			}
			conn.setAutoCommit(true);
			preSt.close();
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			conn.close();
		}
	}
	/**
	 * 
	 * @param colum:  order by colum
	 * @param order:  asc or desc
	 * @return record's id list
	 * @throws Exception
	 */
	private List<Integer> orderby(String colum, String order) throws Exception{
		return orderby(colum, order, null, null);
	}
	
	/**
	 * 
	 * @param colum: order by first colum
	 * @param order: first asc or desc
	 * @param colum2: 
	 * @param order2:
	 * @return record's id list
	 * @throws Exception
	 */
	private List<Integer> orderby(String colum, String order, String colum2, String order2) throws Exception{
		Connection  conn = getConnect();
		List<Integer> ret = new ArrayList<Integer>();
		try{
			String sql = "SELECT id,name,type,remark FROM tlocation ORDER BY "+colum+" COLLATE pinyin "+order;
			if(colum2!=null && order2!=null){
				sql += ", "+colum2+" COLLATE pinyin "+order2;
			}
			PreparedStatement stat = conn.prepareStatement(sql);
			ResultSet rs = stat.executeQuery();
			System.out.println("-------------------------------------------------------");
			System.out.println("|id\t|\tname\t|\ttype\t|\tremark|");
			while(rs.next()){
				ret.add(rs.getInt(1));
				System.out.printf("|%d\t|\t%s\t|\t%s\t|\t%s|\r\n", rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			conn.close();
		}
		return ret;
	}
	@Test
	public void orderby() throws Exception{
		List<Integer> ret = orderby("name", "asc");
		assertArrayEquals(ret.toArray(), NAME_ASC);
		ret = orderby("name", "desc");
		assertArrayEquals(ret.toArray(), NAME_DESC);
		ret = orderby("type", "asc", "name", "desc");
		assertArrayEquals(ret.toArray(), TYPE_ASC_NAME_DESC);
	}
}
