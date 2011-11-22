/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
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
