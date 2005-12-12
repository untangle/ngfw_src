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

package com.metavize.tran.spam;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class SpamSmtpFilter implements SimpleEventFilter<SpamEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("SMTP Events");
    private static final String WARM_QUERY
        = "FROM SpamSmtpEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // EventCache methods -----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(SpamEvent e)
    {
        return e instanceof SpamSmtpEvent;
    }
}
