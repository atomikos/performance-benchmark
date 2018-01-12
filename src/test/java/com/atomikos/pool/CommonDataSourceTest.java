package com.atomikos.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

abstract class CommonDataSourceTest {

	protected static final int NB_THREADS = 50;
	protected static final int NB_TRANSACTIONS_PER_THREAD = 500;
	protected DataSource ds;

	protected void prepareData(DataSource ds) throws SQLException {
		Connection conn = ds.getConnection();
		Statement s = conn.createStatement();
		try {
			s.executeQuery("select * from Accounts");
		} catch (SQLException ex) {
			// table not there => create it
			System.err.println("Creating Accounts table...");
			s.executeUpdate("create table Accounts (account VARCHAR ( 200 ), owner VARCHAR(300), balance DECIMAL (19,0) )");
		}
		s.close();
		conn.close();
	}

	protected void performSQL() throws SQLException {
		String  account = UUID.randomUUID().toString();
		Connection c = ds.getConnection();
		PreparedStatement insertStmt = c.prepareStatement("insert into Accounts values ( ? ,  ?, 10000 )");
		insertStmt.setString(1, account);
		insertStmt.setString(2, account);
		insertStmt.execute();
		insertStmt.close();
		PreparedStatement deleteStmt = c
				.prepareStatement("delete from Accounts where account = ?");
		deleteStmt.setString(1, account);
		deleteStmt.execute();
		deleteStmt.close();
		c.close();
	}

}