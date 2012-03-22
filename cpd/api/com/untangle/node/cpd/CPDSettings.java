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

/**
 * Settings for the Captive Portal Node.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class CPDSettings implements Serializable
{

    public static enum AuthenticationType { ACTIVE_DIRECTORY, RADIUS, LOCAL_DIRECTORY, NONE };
    public static enum PageType { BASIC_LOGIN, BASIC_MESSAGE, CUSTOM };

    private Long id;

    private List<CaptureRule> captureRules = new LinkedList<CaptureRule>();

    private List<PassedAddress> passedClients = new LinkedList<PassedAddress>();
    private List<PassedAddress> passedServers = new LinkedList<PassedAddress>();

    private boolean captureBypassedTraffic = false;
    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private int idleTimeout = 0;
    private int timeout = 3600;
    private boolean areConcurrentLoginsEnabled = true;
    private PageType pageType = PageType.BASIC_MESSAGE;
    private String redirectUrl = "";
    private boolean useHttpsPage= false;
    private boolean isRedirectHttpsEnabled = false;

    private String basicLoginPageTitle = "";
    private String basicLoginPageWelcome = "";
    private String basicLoginUsername = "";
    private String basicLoginPassword = "";
    private String basicLoginMessageText = "";
    private String basicLoginFooter = "";
    private String basicMessagePageTitle = "";
    private String basicMessagePageWelcome = "";
    private String basicMessageMessageText = "";
    private boolean basicMessageAgreeBox = false;
    private String basicMessageAgreeText = "";
    private String basicMessageFooter = "";

    public CPDSettings()
    {
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

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

    public List<PassedAddress> getPassedClients()
    {
        return this.passedClients;
    }

    public void setPassedClients(List<PassedAddress> newValue)
    {
        this.passedClients = newValue;
    }

    public List<PassedAddress> getPassedServers()
    {
        return this.passedServers;
    }

    public void setPassedServers(List<PassedAddress> newValue)
    {
        this.passedServers = newValue;
    }

    public boolean getCaptureBypassedTraffic()
    {
        return this.captureBypassedTraffic;
    }

    public void setCaptureBypassedTraffic(boolean newValue)
    {
        this.captureBypassedTraffic = newValue;
    }

    public AuthenticationType getAuthenticationType()
    {
        return this.authenticationType;
    }

    public void setAuthenticationType( AuthenticationType newValue )
    {
        this.authenticationType = newValue;
    }

    /**
     * @return Idle timeout in seconds.
     */
    public int getIdleTimeout()
    {
        return this.idleTimeout;
    }

    /**
     * Set the idle timeout.
     * @param newValue The new idle timeout in seconds.
     */
    public void setIdleTimeout( int newValue )
    {
        this.idleTimeout = newValue;
    }

    /**
     * Retrieve the session timeout.
     * @return The session timeout in seconds
     */
    public int getTimeout()
    {
        return this.timeout;
    }

    /**
     * Set the session timeout.
     * @param newValue The new session timeout in seconds.
     */
    public void setTimeout( int newValue )
    {
        this.timeout = newValue;
    }

    public boolean getConcurrentLoginsEnabled()
    {
        return this.areConcurrentLoginsEnabled;
    }

    public void setConcurrentLoginsEnabled( boolean newValue)
    {
        this.areConcurrentLoginsEnabled = newValue;
    }

    public PageType getPageType()
    {
        return this.pageType;
    }

    public void setPageType( PageType newValue)
    {
        this.pageType = newValue;
    }

    public String getRedirectUrl()
    {
        return this.redirectUrl;
    }

    public void setRedirectUrl( String newValue)
    {
        this.redirectUrl = newValue;
    }

    public boolean getUseHttpsPage()
    {
        return this.useHttpsPage;
    }

    public void setUseHttpsPage( boolean newValue)
    {
        this.useHttpsPage = newValue;
    }

    public boolean getRedirectHttpsEnabled()
    {
        return this.isRedirectHttpsEnabled;
    }

    public void setRedirectHttpsEnabled( boolean newValue)
    {
        this.isRedirectHttpsEnabled = newValue;
    }

    public String getBasicLoginPageTitle()      { return this.basicLoginPageTitle; }
    public String getBasicLoginPageWelcome()    { return this.basicLoginPageWelcome; }
    public String getBasicLoginUsername()       { return this.basicLoginUsername; }
    public String getBasicLoginPassword()       { return this.basicLoginPassword; }
    public String getBasicLoginMessageText()    { return this.basicLoginMessageText; }
    public String getBasicLoginFooter()         { return this.basicLoginFooter; }
    public String getBasicMessagePageTitle()    { return this.basicMessagePageTitle; }
    public String getBasicMessagePageWelcome()  { return this.basicMessagePageWelcome; }
    public String getBasicMessageMessageText()  { return this.basicMessageAgreeText; }
    public boolean getBasicMessageAgreeBox()    { return this.basicMessageAgreeBox; }
    public String getBasicMessageAgreeText()    { return this.basicMessageAgreeText; }
    public String getBasicMessageFooter()       { return this.basicMessageFooter; }

    public void setBasicLoginPageTitle( String newValue )       { this.basicLoginPageTitle = newValue; }
    public void setBasicLoginPageWelcome( String newValue )     { this.basicLoginPageWelcome = newValue; }
    public void setBasicLoginUsername( String newValue )        { this.basicLoginUsername = newValue; }
    public void setBasicLoginPassword( String newValue )        { this.basicLoginPassword = newValue; }
    public void setBasicLoginMessageText( String newValue )     { this.basicLoginMessageText = newValue; }
    public void setBasicLoginFooter( String newValue )          { this.basicLoginFooter = newValue; }
    public void setBasicMessagePageTitle( String newValue )     { this.basicMessagePageTitle = newValue; }
    public void setBasicMessagePageWelcome( String newValue )   { this.basicMessagePageWelcome = newValue; }
    public void setBasicMessageMessageText( String newValue )   { this.basicMessageAgreeText = newValue; }
    public void setBasicMessageAgreeBox( boolean newValue )     { this.basicMessageAgreeBox = newValue; }
    public void setBasicMessageAgreeText( String newValue )     { this.basicMessageAgreeText = newValue; }
    public void setBasicMessageFooter( String newValue )        { this.basicMessageFooter = newValue; }
}
