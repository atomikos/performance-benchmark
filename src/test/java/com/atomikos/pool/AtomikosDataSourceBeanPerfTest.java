package com.atomikos.pool;

import static com.atomikos.icatch.config.Configuration.getConfigProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.atomikos.icatch.config.Configuration;
import com.atomikos.jdbc.AtomikosDataSourceBean;

public class AtomikosDataSourceBeanPerfTest {

	private static final int NB_THREADS = 50;
	private static final int NB_TRANSACTIONS_PER_THREAD = 100;

	private UserTransaction userTransaction;

	@Before
	public void setUp() throws Exception {
		getConfigProperties().setProperty("com.atomikos.icatch.max_actives",String.valueOf(Integer.MAX_VALUE));
		getConfigProperties().setProperty("com.atomikos.icatch.checkpoint_interval", "10000");
		getConfigProperties().setProperty("com.atomikos.icatch.registered","true");
		getConfigProperties().setProperty("com.atomikos.icatch.default_jta_timeout","30000");
		userTransaction = new com.atomikos.icatch.jta.UserTransactionImp();
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
		ds.setUniqueResourceName("resourceName");
		ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");
		ds.setConcurrentConnectionValidation(true);
		Properties props = new Properties();
		props.put("ServerName", "192.168.0.12");
		props.put("PortNumber", "5432");
		props.put("DatabaseName", "atomikos");
		props.put("User", "atomikos");
		props.put("Password", "atomikos");
		ds.setXaProperties(props);
		ds.setPoolSize(20);
		ds.setTestQuery("SELECT 1");
		ds.init();
		prepareData(ds);
		this.ds = ds;
	}

	@After
	public void tearDown() throws Exception {
		Configuration.shutdown(2000);
		((AtomikosDataSourceBean) ds).close();
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
		
		Runnable r = new Runnable() {
			public void run() {
				
					for (int count = 0; count < NB_TRANSACTIONS_PER_THREAD; count++) {
						try {
						userTransaction.begin();
						UUID uuid = UUID.randomUUID();
						Connection c = ds.getConnection();
						PreparedStatement stmt = c.prepareStatement("insert into Accounts values ( ? ,  ?, 10000 )");
						stmt.setString(1, uuid.toString());
						stmt.setString(2, uuid.toString());
						stmt.execute();
						stmt.close();
//						for (int i = 0; i < 2; i++) {
//							PreparedStatement update = c
//									.prepareStatement("update Accounts set balance = balance - ? where account = ?");
//							update.setDouble(1, random.nextDouble());
//							update.setString(2, name);
//							update.execute();
//							update.close();
//						}
						PreparedStatement deleteStmt = c
								.prepareStatement("delete from Accounts where account = ?");
						deleteStmt.setString(1, uuid.toString());
						deleteStmt.execute();
						deleteStmt.close();
						//Thread.sleep(10); //simulate latency
						c.close();
						userTransaction.commit();  //success !
						} catch (Exception e) {
							e.printStackTrace();
							try {
								userTransaction.rollback();
							} catch (Exception e1) {
							//	e1.printStackTrace();
							} 
						} finally {
						}
					}
				
			}
		};

		Thread[] threads = new Thread[NB_THREADS];
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread(r);
		}
		
		long start = System.currentTimeMillis();
		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}
		for (int j = 0; j < threads.length; j++) {
			threads[j].join();
		}
		System.out.println("NB transactions per seconds "+(NB_THREADS*NB_TRANSACTIONS_PER_THREAD)*1000/((System.currentTimeMillis() - start)));
		
		

	}
}
