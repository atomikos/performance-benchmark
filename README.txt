This repository contains performance benchmarking code to test the relative overhead of JTA/XA transactions versus regular JDBC transactions. We do this by comparing a similar application scenario with a JTA/XA pool versus with a regular JDBC pool. Both are connected to a PostgreSQL database.

To make sure that the comparison makes sense, we use only one datasource so we are comparing similar things: it would not make sense to compare the full two-phase commit to regular JDBC. However, it does make sense to compare the XA overhead of a 1-phase commit scenario to the performance of a non-XA application with a state-of-the art JDBC pool.

What we found is that the overhead of JTA/XA is negligible in case of network delays to the backend resource (i.e., not using a collacated DBMS). When collocated, JTA/XA results are a bit slower - probably because connection reuse is delayed until the transaction completes (which takes slightly longer than with plain JDBC, thereby making the pool less efficient) and because our open source product includes some logging overhead that is reduced in our commercial products.

Overall, this suggests that for realistic scenarios (where the backend is separate from the application) there is virtually NO overhead in using JTA/XA. So you basically get reliability for free!

If you want even more performance improvements, check out our commercial offering: https://www.atomikos.com/Main/ExtremeTransactions
