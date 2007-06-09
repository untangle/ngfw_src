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

package com.untangle.node.spam;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

public class SpamSpamFilter implements SimpleEventFilter<SpamEvent>
{
    private static final RepositoryDesc SPAM_REPO_DESC = new RepositoryDesc("Spam Events");
    // XXX Hack - specify clamphish label here
    private static final RepositoryDesc CLAM_REPO_DESC = new RepositoryDesc("Identity Theft Events");

    private final String logQuery;
    private final String smtpQuery;
    private final RepositoryDesc repoDesc;

    // constructors -----------------------------------------------------------

    public SpamSpamFilter(String vendor)
    {
        logQuery = "FROM SpamLogEvent evt WHERE evt.spam = true AND evt.vendorName = '" + vendor + "' AND evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
        smtpQuery = "FROM SpamSmtpEvent evt WHERE evt.spam = true AND evt.vendorName = '" + vendor + "' AND evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

        // XXX Hack - specify clamphish vendor name here (see ClamPhishScanner)
        if (false == vendor.equals("Clam")) {
            repoDesc = SPAM_REPO_DESC;
        } else {
            repoDesc = CLAM_REPO_DESC;
        }
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return repoDesc;
    }

    public String[] getQueries()
    {
        return new String[] { logQuery, smtpQuery };
    }

    public boolean accept(SpamEvent e)
    {
        return e.isSpam();
    }
}
