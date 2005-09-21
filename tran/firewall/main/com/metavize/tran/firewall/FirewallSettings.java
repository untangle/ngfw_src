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

package com.metavize.tran.firewall;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.Validatable;

/**
 * Settings for the Firewall transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_FIREWALL_SETTINGS"
 */
public class FirewallSettings implements Serializable, Validatable
{
    private Long id;
    private Tid tid;

    /* XXX Must be updated */
    private static final long serialVersionUID = 1629094295874759581L;

    private List firewallRuleList = null;

    private boolean quickExit = true;
    private boolean rejectSilently = true;
    private boolean isDefaultAccept = true;

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
        this.firewallRuleList = new LinkedList();
    }

    /* Validation method */
    public void validate() throws ParseException
    {
        for ( Iterator iter = this.firewallRuleList.iterator(); iter.hasNext() ; ) {
            ((FirewallRule)iter.next()).fixPing();
        }
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
     * column="IS_QUICKEXIT"
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
     * column="IS_REJECT_SILENT"
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
     *  If true, the session is accepted if it doesn't match any other rules.
     *
     * @hibernate.property
     * column="IS_DEFAULT_ACCEPT"
     */
    public boolean isDefaultAccept()
    {
        return this.isDefaultAccept;
    }

    public void setDefaultAccept( boolean b )
    {
        this.isDefaultAccept = b;
    }

    /**
     * List of the redirect rules.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all"
     * table="TR_FIREWALL_RULES"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.firewall.FirewallRule"
     * column="RULE_ID"
     */
    public List getFirewallRuleList()
    {
        return firewallRuleList;
    }

    public void setFirewallRuleList(List s )
    {
        this.firewallRuleList = s;
    }
}
