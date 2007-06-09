/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;

public abstract class TransactionWork<T>
{
    // abstract methods -------------------------------------------------------

    public abstract boolean doWork(Session s) throws SQLException;

    // public methods ---------------------------------------------------------

    public T getResult()
    {
        return null;
    }

    public int getTransactionIsolation()
    {
        return Connection.TRANSACTION_READ_COMMITTED;
    }
}
