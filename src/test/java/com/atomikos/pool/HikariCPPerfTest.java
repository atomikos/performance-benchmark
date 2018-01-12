package com.atomikos.pool;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zaxxer.hikari.HikariDataSource;

public class HikariCPPerfTest extends CommonDataSourceTest {

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
		((HikariDataSource)ds).close();
	}

	@Test
	public void testHarness() throws Exception {
		Runnable r = new Runnable() {
			public void run() {
				for (int count = 0; count < NB_TRANSACTIONS_PER_THREAD; count++) {
					try {
						performSQL();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		};
		long start = System.currentTimeMillis();
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
		System.out.println("NB transactions per seconds "
				+ (NB_THREADS * NB_TRANSACTIONS_PER_THREAD * 1000)
				/ ((System.currentTimeMillis() - start)));

	}
}
