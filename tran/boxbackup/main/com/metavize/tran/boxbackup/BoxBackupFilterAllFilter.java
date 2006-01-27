/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.boxbackup;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class BoxBackupFilterAllFilter implements SimpleEventFilter<BoxBackupEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Protocol Events");

    private static final String WARM_QUERY
        = "FROM BoxBackupEvent evt ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(BoxBackupEvent e)
    {
        return true;
    }
}
