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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.uvm.security.Tid;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

/**
 * Settings for the Captive Portal Node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_cpd_settings", schema="settings")
public class CPDSettings implements Serializable
{
    public static enum AuthenticationType { ACTIVE_DIRECTORY, RADIUS, NONE };    
    public static enum PageType { BASIC_LOGIN, BASIC_MESSAGE, CUSTOM };

    private Long id;
    private Tid tid;

    private List<CaptureRule> captureRules = null;

    /* List of IPDBMatchers, separated by ';' */
    private String passedClients = "";
    private String passedServers = "";

    private boolean captureBypassedTraffic = true;
    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private int idleTimeout = 0;
    private int timeout = 0;
    private boolean isLogoutButtonEnabled = false;
    private boolean areConcurrentLoginsEnabled = true;
    private PageType pageType = PageType.BASIC_MESSAGE;
    private String pageParameters = "";
    private String redirectUrl = "";
    private boolean useHttpsPage= false;
    private boolean isRedirectHttpsEnabled = false; 

    public CPDSettings()
    {
    }

    public CPDSettings( Tid tid)
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
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
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

    @Column(name="passed_clients", nullable=false)
    public String getPassedClients()
    {
        return this.passedClients;
    }

    public void setPassedClients(String newValue)
    {
        this.passedClients = newValue;
    }

    @Column(name="passed_servers", nullable=false)
    public String getPassedServers()
    {
        return this.passedServers;
    }

    public void setPassedServers(String newValue)
    {
        this.passedServers = newValue;
    }

    @Column(name="capture_bypassed_traffic", nullable=false)
    public boolean getCaptureBypassedTraffic()
    {
        return this.captureBypassedTraffic;
    }

    public void setCaptureBypassedTraffic(boolean newValue)
    {
        this.captureBypassedTraffic = newValue;
    }
    
    @Column(name="authentication_type", nullable=false)
    @Type(type="com.untangle.node.cpd.AuthenticationTypeUserType")
    public AuthenticationType getAuthenticationType()
    {
    	return this.authenticationType;
    }
    
    public void setAuthenticationType( AuthenticationType newValue )
    {
    	this.authenticationType = newValue;
    }
    
    
    @Column(name="idle_timeout", nullable=false)
    public int getIdleTimeout()
    {
    	return this.idleTimeout;
    }
    
    public void setIdleTimeout( int newValue )
    {
    	this.idleTimeout = newValue;
    }

    @Column(name="timeout", nullable=false)
    public int getTimeout()
    {
    	return this.timeout;
    }
    
    public void setTimeout( int newValue )
    {
    	this.timeout = newValue;
    }
    
    @Column(name="logout_button", nullable=false)
    public boolean getLogoutButtonEnabled()
    {
    	return this.isLogoutButtonEnabled;
    }
    
    public void setLogoutButtonEnabled( boolean newValue)
    {
    	this.isLogoutButtonEnabled = newValue;
    }
    
    @Column(name="concurrent_logins", nullable=false)
    public boolean getConcurrentLoginsEnabled()
    {
    	return this.areConcurrentLoginsEnabled;
    }
    
    public void setConcurrentLoginsEnabled( boolean newValue)
    {
    	this.areConcurrentLoginsEnabled = newValue;
    }

    @Column(name="page_type", nullable=false)
    @Type(type="com.untangle.node.cpd.PageTypeUserType")
    public PageType getPageType()
    {
    	return this.pageType;
    }
    
    public void setPageType( PageType newValue)
    {
    	this.pageType = newValue;
    }
    
    @Column(name="page_parameters", nullable=false)
    public String getPageParameters()
    {
    	return this.pageParameters;
    }
    
    public void setPageParameters( String newValue)
    {
    	this.pageParameters = newValue;
    }
 
    @Column(name="redirect_url", nullable=false)
    public String getRedirectUrl()
    {
    	return this.redirectUrl;
    }
    
    public void setRedirectUrl( String newValue)
    {
    	this.redirectUrl = newValue;
    }
    
    @Column(name="https_page", nullable=false)
    public boolean getUseHttpsPage()
    {
    	return this.useHttpsPage;
    }
    
    public void setUseHttpsPage( boolean newValue)
    {
    	this.useHttpsPage = newValue;
    }

    @Column(name="redirect_https", nullable=false)
    public boolean getRedirectHttpsEnabled()
    {
    	return this.isRedirectHttpsEnabled;
    }
    
    public void setRedirectHttpsEnabled( boolean newValue)
    {
    	this.isRedirectHttpsEnabled = newValue;
    }
}
