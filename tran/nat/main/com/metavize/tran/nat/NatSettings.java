/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterSettings.java,v 1.8 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.tran.protofilter;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the ProtoFilter transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_FIREWALL_SETTINGS"
 */
public class FirewallSettings implements java.io.Serializable
{
    /* XXX Must be updated */
    private static final long serialVersionUID = 2664348127860496780L;

    private List rules = null;

    private boolean quickExit;
    private boolean rejectSilently;    

    /**
     * Hibernate constructor.
     */
    private FirewallSettings() {}

    /**
     * Real constructor
     */
    public FirewallSettings(Tid tid)
    {
        this.tid = tid;
        this.patterns = new LinkedList();
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid() 
    {
        return tid;
    }
    
    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * If true, exit on the first positive or negative match.  Otherwise, exit
     * on the first negative match.
     *
     * @hibernate.property
     * column="QUICKEXIT"
     */
    public boolean isQuickExit()
    {
        return this.quickExit;
    }

    public void setQuickExit( boolean b ) 
    {
        this.quickExit = b;
    }

    /**
     *  If true, the session is rejected quietly (default), otherwise the connection
     *  is rejected silently.
     *
     * @hibernate.property
     * column="REJECTSILENT"
     */
    public boolean isRejectSilently()
    {
        return this.rejectSilently;
    }

    public void setRejectSilently( boolean b ) 
    {
        this.rejectSilently = b;
    }

    /**
     * Firewall rules.
     *
     * @return the list of Firewall Rules
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.firewall.FirewallRule"
     */
    public List getRules()
    {
        return rules;
    }

    public void setRules(List s ) 
    { 
        this.rules = s;
    }
}
