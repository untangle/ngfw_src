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

package com.untangle.tran.boxbackup;

import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class BoxBackupFilterAllFilter implements SimpleEventFilter<BoxBackupEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Backup Events");

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
