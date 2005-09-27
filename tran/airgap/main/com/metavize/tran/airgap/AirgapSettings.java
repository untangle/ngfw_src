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

package com.metavize.tran.airgap;

import java.io.Serializable;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.shield.ShieldNodeRule;

/**
 * Settings for the Airgap Transform.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_AIRGAP_SETTINGS"
 */
public class AirgapSettings implements Serializable
{
    private static final long serialVersionUID = -4330737456182204381L;

    private Long id;
    private Tid tid;

    private List shieldNodeRuleList = new LinkedList();
    
    public AirgapSettings() { }

    public AirgapSettings(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Shield node configuration rules.
     *
     * @return the list of user settings
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.shield.ShieldNodeRule"
     */
    public List getShieldNodeRuleList()
    {
        if ( null == this.shieldNodeRuleList ) {
            this.shieldNodeRuleList = new LinkedList();
        }

        return this.shieldNodeRuleList;
    }
    
    public void setShieldNodeRuleList( List shieldNodeRuleList )
    {
        if ( null == shieldNodeRuleList  ) {
            shieldNodeRuleList = new LinkedList();
        }

        this.shieldNodeRuleList = shieldNodeRuleList;
    }
}
