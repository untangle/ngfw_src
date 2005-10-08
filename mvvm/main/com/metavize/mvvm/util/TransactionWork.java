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

import java.sql.SQLException;

import org.hibernate.Session;

public interface TransactionWork<T>
{
    boolean doWork(Session s) throws SQLException;
    T getResult();
}
