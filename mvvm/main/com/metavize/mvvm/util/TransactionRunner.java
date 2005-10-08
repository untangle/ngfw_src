/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class TransactionRunner
{
    private final SessionFactory sessionFactory;

    public TransactionRunner(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Runs a transaction block in the approved manner.
     *
     * XXX add retry, exceptions to indicate various failure
     * states. Hibernate doesn't seem to throw a helpful exceptions return
     * indicate anything.
     *
     * @param tw the work to be done in the TransactionRunner.
     * @return boolean if the transaction was completed, false if
     * rolled back.
     */
    public boolean runTransaction(TransactionWork tw)
    {
        Session s = null;
        Transaction tx = null;
        try {
            s = sessionFactory.openSession();
            tx = s.beginTransaction();

            if (tw.doWork(s)) {
                tx.commit();
                return true;
            } else {
                tx.rollback();
                return false;
            }
        } catch (Exception exn) {
            if (null != tx) {
                tx.rollback();
            }
            return false;
        } finally {
            if (null != s) {
                s.close();
            }
        }
    }
}
