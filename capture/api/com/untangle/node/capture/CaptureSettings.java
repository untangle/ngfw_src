/**
 * $Id: CaptureSettings.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class CaptureSettings implements Serializable
{
    public static enum AuthenticationType { ACTIVE_DIRECTORY, RADIUS, LOCAL_DIRECTORY, NONE };
    public static enum PageType { BASIC_LOGIN, BASIC_MESSAGE, CUSTOM };

    private List<CaptureRule> captureRules = new LinkedList<CaptureRule>();
    private List<PassedAddress> passedClients = new LinkedList<PassedAddress>();
    private List<PassedAddress> passedServers = new LinkedList<PassedAddress>();

    private AuthenticationType authenticationType = AuthenticationType.NONE;
    private int idleTimeout = 0;
    private int userTimeout = 3600;
    private boolean areConcurrentLoginsEnabled = true;
    private PageType pageType = PageType.BASIC_MESSAGE;
    private String customFileName = "";
    private String redirectUrl = "";

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

    public CaptureSettings()
    {
    }

    public List<CaptureRule> getCaptureRules()
    {
    	if ( this.captureRules == null )
    	{
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

    public AuthenticationType getAuthenticationType()
    {
        return this.authenticationType;
    }

    public void setAuthenticationType( AuthenticationType newValue )
    {
        this.authenticationType = newValue;
    }

    public int getIdleTimeout()
    {
        return this.idleTimeout;
    }

    public void setIdleTimeout( int newValue )
    {
        this.idleTimeout = newValue;
    }

    public int getUserTimeout()
    {
        return this.userTimeout;
    }

    public void setUserTimeout( int newValue )
    {
        this.userTimeout = newValue;
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

    public String getCustomFilename()
    {
        return this.customFileName;
    }

    public void setCustomFilename( String newValue )
    {
        this.customFileName = newValue;
    }

    public String getBasicLoginPageTitle()      { return this.basicLoginPageTitle; }
    public String getBasicLoginPageWelcome()    { return this.basicLoginPageWelcome; }
    public String getBasicLoginUsername()       { return this.basicLoginUsername; }
    public String getBasicLoginPassword()       { return this.basicLoginPassword; }
    public String getBasicLoginMessageText()    { return this.basicLoginMessageText; }
    public String getBasicLoginFooter()         { return this.basicLoginFooter; }
    public String getBasicMessagePageTitle()    { return this.basicMessagePageTitle; }
    public String getBasicMessagePageWelcome()  { return this.basicMessagePageWelcome; }
    public String getBasicMessageMessageText()  { return this.basicMessageMessageText; }
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
    public void setBasicMessageMessageText( String newValue )   { this.basicMessageMessageText = newValue; }
    public void setBasicMessageAgreeBox( boolean newValue )     { this.basicMessageAgreeBox = newValue; }
    public void setBasicMessageAgreeText( String newValue )     { this.basicMessageAgreeText = newValue; }
    public void setBasicMessageFooter( String newValue )        { this.basicMessageFooter = newValue; }
}
