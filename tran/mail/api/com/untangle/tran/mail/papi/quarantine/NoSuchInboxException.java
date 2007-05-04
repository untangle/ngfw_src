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

package com.untangle.tran.mail.papi.quarantine;
import java.io.Serializable;

/**
 * ...name says it all...
 */
public class NoSuchInboxException
    extends Exception
    implements Serializable {

    private final String m_accountName;

    public NoSuchInboxException(String accountName) {
        super("No such account \"" + accountName + "\"");
        m_accountName = accountName;
    }

    public String getAccountName() {
        return m_accountName;
    }

}
