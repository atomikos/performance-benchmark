package com.atomikos.pool;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariDataSource;

public class HikariCPPerfTest extends CommonDataSourceTest {

	private static final Logger LOGGER = LoggerFactory.getLogger("Hikari");
	@Before
	public void setUp() throws Exception {
		HikariDataSource ds = new HikariDataSource();
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setJdbcUrl("jdbc:postgresql://"+HOST+"/"+DB_NAME);
		ds.setUsername(USER);
		ds.setPassword(PASSWORD);
		ds.setMinimumIdle(POOL_SIZE);
		ds.setMaximumPoolSize(POOL_SIZE);
		ds.setConnectionTestQuery("SELECT 1");
		prepareData(ds);
		this.ds = ds;
	}

	@After
	public void tearDown() throws Exception {
		((HikariDataSource)ds).close();
	}

	@Test
	public void testHarness() throws Exception {
		Runnable r = new Runnable() {
			public void run() {
				for (int count = 0; count < NB_TRANSACTIONS_PER_THREAD; count++) {
					try {
						performSQL(true);
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
		long start = System.currentTimeMillis();
		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}
		for (int j = 0; j < threads.length; j++) {
			threads[j].join();
		}
		LOGGER.info(""+(NB_THREADS*NB_TRANSACTIONS_PER_THREAD)*1000/((System.currentTimeMillis() - start)));
	}
}
