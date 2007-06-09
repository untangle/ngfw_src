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

package com.untangle.tran.spam;

import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class SpamSmtpFilter implements SimpleEventFilter<SpamEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("SMTP Events");
    private final String warmQuery;

    public SpamSmtpFilter(String vendor)
    {
        warmQuery = "FROM SpamSmtpEvent evt WHERE evt.vendorName = '" + vendor + "' AND evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    }

    // EventCache methods -----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { warmQuery };
    }

    public boolean accept(SpamEvent e)
    {
        return e instanceof SpamSmtpEvent;
    }
}
