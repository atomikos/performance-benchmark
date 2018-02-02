package com.atomikos.pool;

import static com.atomikos.icatch.config.Configuration.getConfigProperties;

import java.util.Properties;

import javax.transaction.UserTransaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.icatch.config.Configuration;
import com.atomikos.jdbc.AtomikosDataSourceBean;

public class AtomikosDataSourceBeanPerfTest extends CommonDataSourceTest {

	private static final Logger LOGGER = LoggerFactory.getLogger("Atomikos");

	private UserTransaction userTransaction;

	@Before
	public void setUp() throws Exception {
		getConfigProperties().setProperty("com.atomikos.icatch.max_actives",String.valueOf(Integer.MAX_VALUE));
		getConfigProperties().setProperty("com.atomikos.icatch.checkpoint_interval", "20000");
		getConfigProperties().setProperty("com.atomikos.icatch.registered","true");
		//getConfigProperties().setProperty("com.atomikos.icatch.default_jta_timeout","30000");
		userTransaction = new com.atomikos.icatch.jta.UserTransactionImp();
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
		ds.setUniqueResourceName(DB_NAME);
		ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");
		ds.setConcurrentConnectionValidation(true);
		Properties props = new Properties();
		props.put("ServerName", HOST);
		props.put("PortNumber", "5432");
		props.put("DatabaseName", DB_NAME);
		props.put("User", USER);
		props.put("Password", PASSWORD);
		ds.setXaProperties(props);
		ds.setPoolSize(POOL_SIZE);
		ds.setTestQuery("SELECT 1");
		ds.init();
		prepareData(ds);
		this.ds = ds;
	}

	@After
	public void tearDown() throws Exception {
		Configuration.shutdown(1000);
		((AtomikosDataSourceBean) ds).close();
	}

	@Test
	public void testHarness() throws Exception {
		
		Runnable r = new Runnable() {
			public void run() {
					for (int count = 0; count < NB_TRANSACTIONS_PER_THREAD; count++) {
						try {
						userTransaction.begin();
						performSQL();
						userTransaction.commit();  //success !
						} catch (Exception e) {
							e.printStackTrace();
							try {
								userTransaction.rollback();
							} catch (Exception e1) {
							} 
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
