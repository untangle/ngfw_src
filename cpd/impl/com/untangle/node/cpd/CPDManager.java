package com.untangle.node.cpd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.node.cpd.CPD.BlingerType;
import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.JsonClient.ConnectionException;
import com.untangle.uvm.node.DirectoryConnector;

public class CPDManager
{
    private final Logger logger = Logger.getLogger(CPDManager.class);

    public static final long CACHE_DELAY_MS = 10l * 60l * 1000l;
    private static String CPD_CONFIG_FILE = "/etc/untangle-cpd/config.js";
    private static String CPD_CONFIG_DIR = "/etc/untangle-cpd";
    
    private static final String START_SCRIPT =  System.getProperty( "uvm.home" ) + "/cpd/start";
    private static final String STOP_SCRIPT = System.getProperty( "uvm.home" ) + "/cpd/stop";

    private static final String CPD_WEB_CONFIG = System.getProperty( "uvm.home" ) + "/web/cpd/config.php";

    private static final String LOAD_CUSTOM_SCRIPT = System.getProperty( "uvm.home" ) + "/cpd/load_custom";

    private static final String CPD_URL = System.getProperty( "uvm.node.cpd.url", "http://localhost:3005");
        
    private final CPDImpl cpd;

    CPDManager(CPDImpl cpd)
    {
        this.cpd = cpd;
    }
       
    void setConfig(CPDSettings settings, boolean isEnabled ) throws JSONException, IOException
    {
        /* Convert the settings to JSON */
        JSONObject json = serializeCPDSettings(settings, isEnabled );
        
        (new File( CPD_CONFIG_DIR)).mkdir();
        
        /* Save the configuration into the captive portal */
        FileWriter fw = new FileWriter(CPD_CONFIG_FILE);
        
        /* Write out the configuration */
        fw.write( json.toString());
        fw.write("\n");
        fw.close();

        fw = new FileWriter(CPD_WEB_CONFIG);
        fw.write(String.format( "<?php $https_redirect = %s;?>", settings.getBaseSettings().getUseHttpsPage() ? "TRUE" : "FALSE" ));
        fw.close();
    }
    
    boolean clearHostDatabase() throws JSONException, ConnectionException
    {
        JSONObject jsonObject = new JSONObject();
        
        jsonObject.put( "function", "clear_host_database" );
        
        JSONObject response = JsonClient.getInstance().call(CPD_URL, jsonObject);
        if ( logger.isDebugEnabled()) {
            logger.debug( "Server Returned: " + response.toString());
        }
        
        int status = response.getInt( JsonClient.RESPONSE_STATUS);
        String message = response.getString( JsonClient.RESPONSE_MESSAGE);
        
        if (  status != JsonClient.STATUS_SUCCESS ) {
            logger.info( "CPD could clear host database. [" + message + "]");
            return false;
        }
        
        return true;
    }
    
    void start() throws Exception
    {
        ScriptRunner.getInstance().exec( START_SCRIPT );
    }
    
    void stop()
    {
        try {
            ScriptRunner.getInstance().exec( STOP_SCRIPT );
        } catch (Exception e) {
            logger.debug( "Unable to stop untangle-cpd.", e);
        }
    }
    
    void loadCustomPage( String fileName ) throws Exception
    {
        ScriptRunner.getInstance().exec( LOAD_CUSTOM_SCRIPT, fileName );
    }
    
    boolean authenticate( String addressString, String username, String password, String credentials )
    {
        InetAddress address = null;
        try {
            address = InetAddress.getByName( addressString );
        } catch ( UnknownHostException e ) {
            logger.info( "Unable to resolve the host:" + addressString );
            return false;
        }
        
        boolean isAuthenticated = false;
        CPDBaseSettings baseSettings = this.cpd.getBaseSettings();
        AuthenticationType method = baseSettings.getAuthenticationType(); 
        /**
         * bug #7951
         * Try an alternative username that removes domain
         * domain*backslash*user -> user
         * user@domain -> user
         */
        String strippedUsername = username;
        strippedUsername = strippedUsername.replaceAll(".*\\\\","");
        strippedUsername = strippedUsername.replaceAll("@.*","");

        /**
         * try first with normal username
         * then try with stripped username
         */
        for (int i = 0; i< 2 ; i++) {
            String u;
            if (i == 0)
                u = username;
            else 
                u = strippedUsername;
                
            switch( method ) {
            case NONE:
                isAuthenticated = true;
                break;
            
            case ACTIVE_DIRECTORY:
                try {
                    DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                    if (adconnector != null)
                        isAuthenticated = adconnector.activeDirectoryAuthenticate( u, password );
                } catch (Exception e) {
                    logger.warn( "Unable to authenticate users.", e );
                    isAuthenticated = false;
                }
                break;
            
            case LOCAL_DIRECTORY:
                try {
                    isAuthenticated = UvmContextFactory.context().localDirectory().authenticate( u, password );
                } catch (Exception e) {
                    logger.warn( "Unable to authenticate users.", e );
                    isAuthenticated = false;
                }
                break;
            
            case RADIUS:
                try {
                    DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                    if (adconnector != null)
                        isAuthenticated = adconnector.radiusAuthenticate( u, password );
                }
                catch (Exception e) {
                    logger.warn( "Unable to authenticate users.", e );
                    isAuthenticated = false;
                }
                break;
            }

            if (isAuthenticated)
                break;
        }

        if ( !isAuthenticated ) {
            return false;
        }
        
        /* Tell the Captive Portal daemon about the new success */
        try {
            if ( !replaceHost(address, username)) {
                return false;
            }
        } catch ( Exception e ) {
            logger.info( "Unable to replace host", e );
            return false;
        }
        
        /* Expire the cache on the phonebook */
        /* This will force adconnector to relookup the address and log any associated events */
        DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getIpUsernameMap().expireUser( address );
        }
        
        CPDLoginEvent.EventType eventType = isAuthenticated ? 
                CPDLoginEvent.EventType.LOGIN : CPDLoginEvent.EventType.FAILED; 
        CPDLoginEvent event = new CPDLoginEvent( address, username, method, eventType );

        this.cpd.getLoginEventManager().log(event);        
        
        if ( isAuthenticated ) {
            this.cpd.incrementCount(BlingerType.AUTHORIZE, 1);
        }
        
        return isAuthenticated;
    }
    
    boolean logout( String addressString )
    {
        InetAddress address = null;
        try {
            address = InetAddress.getByName( addressString );
        } catch ( UnknownHostException e ) {
            logger.info( "Unable to resolve the host:" + addressString );
            return false;
        }
        
        try {
            if ( !removeHost(address)) {
                return false;
            }
        } catch (JSONException e) {
            logger.warn( "Unable to remove host", e);
            return false;
        } catch (ConnectionException e) {
            logger.warn( "Unable to remove host", e);
            return false;
        }
        
        /* Expire the cache on the phonebook */
        /* This will force adconnector to relookup the address and log any associated events */
        DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getIpUsernameMap().expireUser( address );
        }

        CPDBaseSettings baseSettings = this.cpd.getBaseSettings();
        
        AuthenticationType method = baseSettings.getAuthenticationType(); 

        CPDLoginEvent event = new CPDLoginEvent( address, "", method, CPDLoginEvent.EventType.LOGOUT );
        this.cpd.getLoginEventManager().log(event);
        
        return true;
    }

    private JSONObject serializeCPDSettings( CPDSettings settings, boolean isEnabled ) throws JSONException
    {
        JSONObject json = new JSONObject();
        
        CPDBaseSettings baseSettings = settings.getBaseSettings();  
                
        /* Save the values from the base settings */
        json.put("enabled", isEnabled );
        json.put("capture_bypassed_traffic", baseSettings.getCaptureBypassedTraffic());
        json.put("concurrent_logins", baseSettings.getConcurrentLoginsEnabled());
        json.put("authentication_type", baseSettings.getAuthenticationType().toString());
        json.put("idle_timeout_s", baseSettings.getIdleTimeout());
        json.put("timeout_s", baseSettings.getTimeout());
        json.put("page_type", baseSettings.getPageType().toString());
        json.put("page_parameters", new JSONObject(baseSettings.getPageParameters()));
        json.put("redirect_https_enabled", baseSettings.getRedirectHttpsEnabled());
        json.put("redirect_url", baseSettings.getRedirectUrl());
        json.put("use_https_page", baseSettings.getUseHttpsPage());
        
        /* This setting is not configurable through the UI */
        json.put("expiration_frequency_s", 60);
        
        JSONArray captureRules = new JSONArray();
        
        /* Add the passed clients and addresses first, and then add the capture rules */
        for ( PassedClient client : settings.getPassedClients()) {
            if (client.isLive()) {
                serializePassedAddress(captureRules, client.getAddress(), IPMatcher.getAnyMatcher());
            }
        }
        
        for ( PassedServer server : settings.getPassedServers()) {
            if (server.isLive()) {
                serializePassedAddress(captureRules, IPMatcher.getAnyMatcher(), server.getAddress());
            }
        }
        
        for ( CaptureRule captureRule : settings.getCaptureRules()) {
            serializeCaptureRule(captureRules, captureRule);
        }
        
        json.put( "capture_rules",  captureRules );
        
        return json;
    }

    private void serializeCaptureRule(JSONArray rules, CaptureRule captureRule) throws JSONException
    {
        List<Integer> interfaceList = splitInterfaceList(captureRule.getClientInterface());
        if ( interfaceList == null ) {
            return;
        }
        
        List<String> clientAddressList = splitAddressList(captureRule.getClientAddress());
        if ( clientAddressList == null ) {
            return;
        }
        
        List<String> serverAddressList = splitAddressList(captureRule.getServerAddress());
        if ( serverAddressList == null ) {
            return;
        }
        
        for ( Integer intf : interfaceList ) {
            for ( String clientAddress : clientAddressList ) {
                for ( String serverAddress : serverAddressList ) {
                    JSONObject json = new JSONObject();
                    json.put("enabled", captureRule.isLive());
                    json.put("description",captureRule.getDescription());
                    json.put("capture", captureRule.getCapture());
                    json.put("client_address", clientAddress );
                    json.put("server_address",serverAddress);
                    json.put("client_interface", intf );
                    json.put("days",captureRule.getDays());
                    json.put("start_time", captureRule.getStartTime());
                    json.put("end_time", captureRule.getEndTime());
                    
                    rules.put(json);
                }
            }
        }
    }
    
    /* CPD doesn't understand the set syntax, have to break rules into their individual parts */
    private List<Integer> splitInterfaceList(IntfMatcher matcher)
    {
        List<Integer> clientInterfaceList = new ArrayList<Integer>(1);

        int index = 0;
        String matcherStr = matcher.toString();

        switch (matcher.getType()) {

        case SINGLE:
            try {
                index = Integer.parseInt(matcherStr);
                clientInterfaceList.add(index);
            } catch (NumberFormatException e) {
                logger.warn("Unknown interface format: " + matcherStr);
            }
            return clientInterfaceList;

        case LIST:
            String[] i = matcherStr.split(",");
            for (index = 1 ; index < 256 ; index++) {
                if (matcher.isMatch(index)) {
                    clientInterfaceList.add(index);
                }
            }
            return clientInterfaceList;

        case ANY:
            clientInterfaceList.add(-1);
            return clientInterfaceList;

        default:
            /* all other cases return null*/
            return null;

        }
    }
    
    private List<String> splitAddressList(IPMatcher matcher)
    {
        List<String> addressList = new ArrayList<String>();
        addressList.add("any");

        IPMatcher.IPMatcherType type = matcher.getType();
        
        if ( ( type == IPMatcher.IPMatcherType.SINGLE ) ||
             ( type == IPMatcher.IPMatcherType.SUBNET ) ||
             ( type == IPMatcher.IPMatcherType.RANGE )  ||
             ( type == IPMatcher.IPMatcherType.LIST )) {
            
            addressList = new ArrayList<String>(1);
            addressList.add(matcher.toDatabaseString());

        } else if ( type == IPMatcher.IPMatcherType.NONE ) {

            logger.info( "Capture rule with nil address matcher, returning.");
            return null;
        } else if ( type == IPMatcher.IPMatcherType.ANY ) {

            /* Nothing to do here */
        } else {

            logger.info( "Capture rule with invalid interface matcher, returning.");
            return null;
            /* Other matcher types are ignored */
        }
        
        return addressList;
    }
    
    private void serializePassedAddress(JSONArray captureRules, IPMatcher client, IPMatcher server)
            throws JSONException
    {
        serializeCaptureRule(captureRules, new CaptureRule(true, false, "passed client", IntfMatcher.getAnyMatcher(), client, server, "00:00", "23:59", "mon,tue,wed,thu,fri,sat,sun"));
    }
    
    private boolean replaceHost( InetAddress clientAddress, String username ) throws JSONException, ConnectionException
    {
        JSONObject jsonObject = new JSONObject();
        
        jsonObject.put( "function", "replace_host" );
        jsonObject.put( "username", username );
        jsonObject.put( "update_session_start", true);
        jsonObject.put( "ipv4_addr", clientAddress.getHostAddress());
        
        JSONObject response = JsonClient.getInstance().call(CPD_URL, jsonObject);
        if ( logger.isDebugEnabled()) {
            logger.debug( "Server Returned: " + response.toString());
        }
        
        int status = response.getInt( JsonClient.RESPONSE_STATUS);
        String message = response.getString( JsonClient.RESPONSE_MESSAGE);
        
        if (  status != JsonClient.STATUS_SUCCESS ) {
            logger.info( "CPD could not replace host. [" + message + "]");
            return false;
        }
        
        return true;   
    }
    
    private boolean removeHost( InetAddress clientAddress ) throws JSONException, ConnectionException
    {
        JSONObject jsonObject = new JSONObject();
        
        jsonObject.put( "function", "remove_ipv4_addr" );
        jsonObject.put( "ipv4_addr", clientAddress.getHostAddress());
        
        JSONObject response = JsonClient.getInstance().call(CPD_URL, jsonObject);
        if ( logger.isDebugEnabled()) {
            logger.debug( "Server Returned: " + response.toString());
        }
        
        int status = response.getInt( JsonClient.RESPONSE_STATUS);
        String message = response.getString( JsonClient.RESPONSE_MESSAGE);
        
        if (  status != JsonClient.STATUS_SUCCESS ) {
            logger.info( "CPD could not remove host. [" + message + "]");
            return false;
        }
        
        return true;   
    }
    
}
