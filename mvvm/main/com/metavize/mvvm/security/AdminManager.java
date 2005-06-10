/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import com.metavize.mvvm.MailSettings;
import javax.transaction.TransactionRolledbackException;

public interface AdminManager
{
    AdminSettings getAdminSettings();

    void setAdminSettings(AdminSettings settings)
        throws TransactionRolledbackException;

    MailSettings getMailSettings();
    
    void setMailSettings(MailSettings settings)
        throws TransactionRolledbackException;

    LoginSession[] loggedInUsers();
}
