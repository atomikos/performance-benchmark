package com.atomikos.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

import javax.sql.DataSource;

abstract class CommonDataSourceTest {

	protected static final int NB_THREADS = 50;
	protected static final int NB_TRANSACTIONS_PER_THREAD = 100;
	protected static final int POOL_SIZE = NB_THREADS/2;
	
	
	protected static final String DB_NAME = "atomikos";
	protected static final String USER = "atomikos";
	protected static final String PASSWORD = "atomikos";
	protected static final String HOST = "192.168.0.12";

	protected DataSource ds;

	protected void prepareData(DataSource ds) throws SQLException {
		Connection conn = ds.getConnection();
		Statement s = conn.createStatement();
			s.executeUpdate("drop table if exists accounts");
			System.err.println("Creating Accounts table...");
			s.executeUpdate("create table accounts ( "
					+ " account INTEGER, owner VARCHAR(300), balance BIGINT )");
			for (int i = 0; i < NB_TRANSACTIONS_PER_THREAD; i++) {
				s.executeUpdate("insert into Accounts values ( " + i + " , 'owner" + i + "', 10000 )");
			}

		s.close();
		conn.close();
	}

	protected void performSQL() throws SQLException {
		Random rand = new Random();
		Connection c = ds.getConnection();
		PreparedStatement s = c
				.prepareStatement("update Accounts set balance = balance + ? where account = ?");
		s.setInt(1, rand.nextInt());
		s.setInt(2, rand.nextInt(NB_TRANSACTIONS_PER_THREAD));
		s.execute();
		s.close();
		c.close();
	}

}