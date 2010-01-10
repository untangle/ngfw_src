package com.untangle.node.cpd;

import java.io.Serializable;

import javax.persistence.Column;

import org.hibernate.annotations.Type;

import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.node.cpd.CPDSettings.PageType;

public class CPDBaseSettings implements Serializable {
    private static final long serialVersionUID = 601889164122486170L;
    private boolean captureBypassedTraffic = true;
    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private int idleTimeout = 0;
    private int timeout = 0;
    private boolean isLogoutButtonEnabled = false;
    private boolean areConcurrentLoginsEnabled = true;
    private PageType pageType = PageType.BASIC_MESSAGE;
    private String pageParameters = "{}";
    private String redirectUrl = "";
    private boolean useHttpsPage= false;
    private boolean isRedirectHttpsEnabled = false; 

    public CPDBaseSettings()
    {
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
    
    /**
     * @return Idle timeout in seconds.
     */
    @Column(name="idle_timeout", nullable=false)
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
    @Column(name="timeout", nullable=false)
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
