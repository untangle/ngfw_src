/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AdminManager.java,v 1.2 2004/12/20 08:24:20 amread Exp $
 */

package com.metavize.mvvm.security;

import javax.transaction.TransactionRolledbackException;

public interface AdminManager
{
    AdminSettings getAdminSettings();
    void setAdminSettings(AdminSettings settings)
        throws TransactionRolledbackException;

    LoginSession[] loggedInUsers();
}
