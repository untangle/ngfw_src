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

package com.untangle.node.firewall;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Validatable;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings for the Firewall node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_firewall_settings", schema="settings")
public class FirewallSettings implements Serializable, Validatable
{
    private Long id;
    private Tid tid;

    /* XXX Must be updated */
    private static final long serialVersionUID = 1629094295874759581L;

    private List<FirewallRule> firewallRuleList = null;

    private boolean quickExit = true;
    private boolean rejectSilently = true;
    private boolean isDefaultAccept = true;

    private FirewallSettings() {}

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

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
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
     */
    @Column(name="is_quickexit", nullable=false)
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
     */
    @Column(name="is_reject_silent", nullable=false)
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
     */
    @Column(name="is_default_accept", nullable=false)
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<FirewallRule> getFirewallRuleList()
    {
        return firewallRuleList;
    }

    public void setFirewallRuleList(List<FirewallRule> s)
    {
        this.firewallRuleList = s;
    }
}
