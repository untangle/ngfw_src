/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.sql.Connection;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;

/**
 * Executes {@link TransactionWork} within a Hibernate transaction.
 */
public class TransactionRunner
{
    private static final int[] SLEEP_TIMES = new int[] { 0, 500, 1000, 2000 };

    private final Logger logger = Logger.getLogger(getClass());

    private final SessionFactory sessionFactory;

    public TransactionRunner(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Runs a transaction block in the approved manner.
     *
     * @param tw the work to be done in the TransactionRunner.
     * @return boolean if the transaction was completed, false if
     * rolled back.
     */
    @SuppressWarnings("deprecation")
	public boolean runTransaction(TransactionWork<?> tw)
    {
        TransactionException transExn = null;

        for (int i = 0; i < SLEEP_TIMES.length; i++) {
            Session s = null;
            Transaction tx = null;
            try {
                s = sessionFactory.openSession();
                Connection c = s.connection();
                c.setTransactionIsolation(tw.getTransactionIsolation());
                tx = s.beginTransaction();

                if (tw.doWork(s)) {
                    tx.commit();
                    return true;
                } else {
                    tx.rollback();
                    return false;
                }
            } catch (TransactionException exn) {
                transExn = exn;
                try {
                    Thread.currentThread();
					Thread.sleep(SLEEP_TIMES[i]);
                } catch (InterruptedException e) { /* keep going */ }
            } catch (Exception exn) {
                logger.error("Hibernate error, see nested exception below", exn);
                if (null != tx) {
                    tx.rollback();
                }
                return false;
            } finally {
                if (null != s) {
                    try {
                        s.close();
                    } catch (HibernateException exn) {
                        logger.warn("could not close session", exn);
                    }
                }
            }
        }

        logger.error("could not commit after " + SLEEP_TIMES.length + "tries.",
                     transExn);

        return false;
    }
}
