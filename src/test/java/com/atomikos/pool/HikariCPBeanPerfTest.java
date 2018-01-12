package com.atomikos.pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

public class HikariCPBeanPerfTest {

	private static final int NB_THREADS = 50;
	private static final int NB_TRANSACTIONS_PER_THREAD = 100;

	@Before
	public void setUp() throws Exception {
		HikariDataSource ds = new HikariDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setJdbcUrl("jdbc:postgresql://192.168.0.12/atomikos");
		ds.setUsername("atomikos");
		ds.setPassword("atomikos");
		ds.setMinimumIdle(20);
		ds.setMaximumPoolSize(20);
		ds.setConnectionTestQuery("SELECT 1");
		prepareData(ds);
		this.ds = ds;
	}

	@After
	public void tearDown() throws Exception {
	}

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

	@Test
	public void testHarness() throws Exception {

		long start = System.currentTimeMillis();
		Runnable r = new Runnable() {
			public void run() {
				
					for (int count = 0; count < NB_TRANSACTIONS_PER_THREAD; count++) {
						try {
						UUID uuid = UUID.randomUUID();
						Connection c = ds.getConnection();
						PreparedStatement stmt = c.prepareStatement("insert into Accounts values ( ? ,  ?, 10000 )");
						stmt.setString(1, uuid.toString());
						stmt.setString(2, uuid.toString());
						stmt.execute();
						stmt.close();
						PreparedStatement deleteStmt = c
								.prepareStatement("delete from Accounts where account = ?");
						deleteStmt.setString(1, uuid.toString());
						deleteStmt.execute();
						deleteStmt.close();
						//Thread.sleep(10); //simulate latency
						c.close();
						} catch (Exception e) {
							e.printStackTrace();
							
						}
					}
				
			}
		};

		Thread[] threads = new Thread[NB_THREADS];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(r);
		}
		
		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}
		for (int j = 0; j < threads.length; j++) {
			threads[j].join();
		}
		System.out.println("NB transactions per seconds "+(NB_THREADS*NB_TRANSACTIONS_PER_THREAD*1000)/((System.currentTimeMillis() - start)));
		
		

	}
}
