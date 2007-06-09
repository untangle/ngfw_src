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

package com.untangle.uvm.engine;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

/**
 * Internal state for NodeManagerImpl.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="node_manager_state", schema="settings")
class NodeManagerState
{
    private Long id;
    private Long lastTid = 0L;

    NodeManagerState() { }

    @Id
    @Column(name="id")
    @GeneratedValue
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
     */
    @Column(name="last_tid", nullable=false)
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
    Tid nextTid(Policy policy, String nodeName)
    {
        return new Tid(++lastTid, policy, nodeName);
    }
}
