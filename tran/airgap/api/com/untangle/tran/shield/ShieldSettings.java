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

package com.untangle.tran.airgap;

import java.io.Serializable;
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
import javax.persistence.Transient;

import com.untangle.mvvm.security.Tid;
import com.untangle.tran.airgap.ShieldNodeRule;
import org.hibernate.annotations.IndexColumn;

/**
 * Settings for the Airgap Transform.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_airgap_settings", schema="settings")
public class AirgapSettings implements Serializable
{
    private static final long serialVersionUID = -4330737456182204381L;

    private Long id;
    private Tid tid;

    private List<ShieldNodeRule> shieldNodeRuleList = new LinkedList<ShieldNodeRule>();

    public AirgapSettings() { }

    public AirgapSettings(Tid tid)
    {
        this.tid = tid;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
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
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
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
     */
    @OneToMany(targetEntity=ShieldNodeRule.class, cascade=CascadeType.ALL,
               fetch=FetchType.EAGER)
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List getShieldNodeRuleList()
    {
        if (null == this.shieldNodeRuleList) {
            this.shieldNodeRuleList = new LinkedList();
        }

        return this.shieldNodeRuleList;
    }

    public void setShieldNodeRuleList(List shieldNodeRuleList)
    {
        if (null == shieldNodeRuleList) {
            shieldNodeRuleList = new LinkedList();
        }

        this.shieldNodeRuleList = shieldNodeRuleList;
    }
}
