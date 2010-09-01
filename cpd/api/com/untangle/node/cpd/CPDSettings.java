/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.cpd;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;

import com.untangle.uvm.security.NodeId;

/**
 * Settings for the Captive Portal Node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_cpd_settings", schema="settings")
@SuppressWarnings("serial")
public class CPDSettings implements Serializable
{

    public static enum AuthenticationType { ACTIVE_DIRECTORY, RADIUS, LOCAL_DIRECTORY, NONE };    
    public static enum PageType { BASIC_LOGIN, BASIC_MESSAGE, CUSTOM };

    private Long id;
    private NodeId tid;

    private List<CaptureRule> captureRules = new LinkedList<CaptureRule>();
    
    private List<PassedClient> passedClients = new LinkedList<PassedClient>();
    private List<PassedServer> passedServers = new LinkedList<PassedServer>();
    
    private CPDBaseSettings baseSettings = new CPDBaseSettings();
    
    public CPDSettings()
    {
    }

    public CPDSettings( NodeId tid)
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
     * Node id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public NodeId getTid()
    {
        return tid;
    }

    public void setTid(NodeId tid)
    {
        this.tid = tid;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<CaptureRule> getCaptureRules()
    {
    	if ( this.captureRules == null ) {
    		this.captureRules = new LinkedList<CaptureRule>();
    	}
        return this.captureRules;
    }

    public void setCaptureRules(List<CaptureRule> newValue)
    {
        this.captureRules = newValue;
    }
    
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PassedClient> getPassedClients()
    {
        return this.passedClients;
    }

    public void setPassedClients(List<PassedClient> newValue)
    {
        this.passedClients = newValue;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<PassedServer> getPassedServers()
    {
        return this.passedServers;
    }

    public void setPassedServers(List<PassedServer> newValue)
    {
        this.passedServers = newValue;
    }

    
    @Embedded
    public CPDBaseSettings getBaseSettings() {
        return baseSettings;
    }

    public void setBaseSettings(CPDBaseSettings baseSettings) {
        this.baseSettings = baseSettings;
    }

}
