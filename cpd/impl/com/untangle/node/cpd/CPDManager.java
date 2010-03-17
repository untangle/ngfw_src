package com.untangle.node.cpd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.ServiceUnavailableException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.node.cpd.CPD.BlingerType;
import com.untangle.node.cpd.CPDSettings.AuthenticationType;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.addrbook.RemoteAddressBook.Backend;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.ParsingConstants;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.intf.IntfSetMatcher;
import com.untangle.uvm.node.firewall.intf.IntfSimpleMatcher;
import com.untangle.uvm.node.firewall.intf.IntfSingleMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPRangeMatcher;
import com.untangle.uvm.node.firewall.ip.IPSetMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;
import com.untangle.uvm.node.firewall.ip.IPSingleMatcher;
import com.untangle.uvm.node.firewall.ip.IPSubnetMatcher;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.user.UserInfo;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.JsonClient.ConnectionException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.ADConnector;
import com.untangle.uvm.user.UserInfo;

class CPDManager {
    private final Logger logger = Logger.getLogger(CPDManager.class);

    public static final long CACHE_DELAY_MS = 10l * 60l * 1000l;
    private static String CPD_CONFIG_FILE = "/etc/untangle-cpd/config.js";
    private static String CPD_CONFIG_DIR = "/etc/untangle-cpd";
    
    private static final String START_SCRIPT =  System.getProperty( "bunnicula.home" ) + "/cpd/start";
    private static final String STOP_SCRIPT = System.getProperty( "bunnicula.home" ) + "/cpd/stop";

    private static final String CPD_WEB_CONFIG = System.getProperty( "bunnicula.home" ) + "/web/cpd/config.php";

    private static final String LOAD_CUSTOM_SCRIPT = System.getProperty( "bunnicula.home" ) + "/cpd/load_custom";

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
    
    void start() throws NodeException
    {
        ScriptRunner.getInstance().exec( START_SCRIPT );
    }
    
    void stop()
    {
        try {
            ScriptRunner.getInstance().exec( STOP_SCRIPT );
        } catch (NodeException e) {
            logger.debug( "Unable to stop untangle-cpd.", e);
        }
    }
    
    void loadCustomPage( String fileName ) throws NodeException
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
        
        /* Just verify that username is a valid string XXX */
//         try {
//             Username.parse(username);
//         } catch (ParseException e1) {
//             logger.info( "Invalid username: '" + username + "'");
//             return false;
//         }
            
        boolean isAuthenticated = false;
        CPDBaseSettings baseSettings = this.cpd.getBaseSettings();
        
        AuthenticationType method = baseSettings.getAuthenticationType(); 
        
        switch( method ) {
        case NONE:
            isAuthenticated = true;
            break;
            
        case ACTIVE_DIRECTORY:
            try {
                isAuthenticated = LocalUvmContextFactory.context().appAddressBook().authenticate(username, password, Backend.ACTIVE_DIRECTORY);
            } catch (ServiceUnavailableException e) {
                logger.warn( "Unable to authenticate users.", e );
                isAuthenticated = false;
            }
            break;
            
        case LOCAL_DIRECTORY:
            try {
                isAuthenticated = LocalUvmContextFactory.context().appAddressBook().authenticate(username, password, Backend.LOCAL_DIRECTORY);
            } catch (ServiceUnavailableException e) {
                logger.warn( "Unable to authenticate users.", e );
                isAuthenticated = false;
            }
            break;
            
        case RADIUS:
            try {
                isAuthenticated = LocalUvmContextFactory.context().appAddressBook().authenticate(username, password, Backend.RADIUS);
            } catch (ServiceUnavailableException e) {
                logger.warn( "Unable to authenticate users.", e );
                isAuthenticated = false;
            }
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
        ADConnector adconnector = (ADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getPhoneBook().expireUser( address );
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
        ADConnector adconnector = (ADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getPhoneBook().expireUser( address );
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
        json.put("logout_button_enabled", baseSettings.getLogoutButtonEnabled());
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
            serializePassedAddress(captureRules,client.getAddress(), IPSimpleMatcher.getAllMatcher());
        }
        
        for ( PassedServer server : settings.getPassedServers()) {
            serializePassedAddress(captureRules,IPSimpleMatcher.getAllMatcher(), server.getAddress());
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
    private List<Integer> splitInterfaceList(IntfDBMatcher matcher)
    {
        List<Integer> clientInterfaceList = new ArrayList<Integer>(1);
        clientInterfaceList.add(-1);

        if ( matcher instanceof IntfSingleMatcher) {
            clientInterfaceList = new ArrayList<Integer>(1);
            clientInterfaceList.add(Integer.parseInt(matcher.toDatabaseString()));
        } else if ( matcher instanceof IntfSetMatcher ) {
            String[] i = matcher.toDatabaseString().split(ParsingConstants.MARKER_SEPERATOR);
            clientInterfaceList = new ArrayList<Integer>(i.length);
            
            for ( String intf : i ) {
                clientInterfaceList.add(Integer.parseInt(intf));
            }
        } else if ( matcher == IntfSimpleMatcher.getNilMatcher()) {
            logger.info( "Capture rule with nil interface matcher, returning.");
            return null;
        } else if ( matcher == IntfSimpleMatcher.getAllMatcher()) {
            /* Nothing to do here */
        } else {
            logger.info( "Capture rule with invalid interface matcher, returning.");
            return null;
            /* Other matcher types are ignored */
        }
        
        return clientInterfaceList;
    }
    
    private List<String> splitAddressList(IPDBMatcher matcher)
    {
        List<String> addressList = new ArrayList<String>();
        addressList.add("any");

        if ( ( matcher instanceof IPSingleMatcher) || ( matcher instanceof IPSubnetMatcher) ||
                ( matcher instanceof IPRangeMatcher ) || ( matcher instanceof IPSetMatcher)) {
            addressList = new ArrayList<String>(1);
            addressList.add(matcher.toDatabaseString());
        } else if ( matcher == IPSimpleMatcher.getNilMatcher()) {
            logger.info( "Capture rule with nil address matcher, returning.");
            return null;
        } else if ( matcher == IPSimpleMatcher.getAllMatcher()) {
            /* Nothing to do here */
        } else {
            logger.info( "Capture rule with invalid interface matcher, returning.");
            return null;
            /* Other matcher types are ignored */
        }
        
        return addressList;
    }
    
    private void serializePassedAddress(JSONArray captureRules,IPDBMatcher client, IPDBMatcher server)
            throws JSONException {
        serializeCaptureRule(captureRules,new CaptureRule(true, false,
                "passed client", IntfSimpleMatcher.getAllMatcher(), client,
                    server, "00:00", "23:59",
                "mon,tue,wed,thu,fri,sat,sun"));
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
