/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformManagerState.java,v 1.1 2004/12/22 07:51:16 amread Exp $
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.security.Tid;

/**
 * Internal state for TransformManagerImpl.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TRANSFORM_MANAGER_STATE"
 */
class TransformManagerState
{
    private Long id;
    private Long lastTid = 0L;

    TransformManagerState() { }

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Last tid assigned.
     *
     * @return last assigned tid.
     * @hibernate.property
     * column="LAST_TID"
     */
    Long getLastTid()
    {
        return lastTid;
    }

    void setLastTid(Long lastTid)
    {
        this.lastTid = lastTid;
    }

    /**
     * Get the next Tid.
     *
     * @return a <code>Long</code> value
     */
    Tid nextTid()
    {
        return new Tid(++lastTid);
    }
}
