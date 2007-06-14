/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
