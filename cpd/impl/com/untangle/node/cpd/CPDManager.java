package com.untangle.node.cpd;

import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.firewall.intf.IntfSimpleMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;

class CPDManager {
    public static final long CACHE_DELAY_MS = 10l * 60l * 1000l;
    private static String CPD_CONFIG_FILE = "/etc/untangle-cpd/config.js";
    
    private static final String START_SCRIPT =  System.getProperty( "bunnicula.home" ) + "/cpd/start";
    private static final String STOP_SCRIPT = System.getProperty( "bunnicula.home" ) + "/cpd/stop";

    
    private final WorkerRunner worker = new WorkerRunner(new CacheMonitor(), null);

    private final CPDImpl cpd;

    CPDManager(CPDImpl cpd)
    {
        this.cpd = cpd;
    }
        
    void setConfig(CPDSettings settings) throws JSONException, IOException
    {
        /* Convert the settings to JSON */
        JSONObject json = serializeCPDSettings(settings);
        
        /* Save the configuration into the captive portal */
        FileWriter fw = new FileWriter(CPD_CONFIG_FILE);
        
        /* Write out the configuration */
        fw.write( json.toString());
        fw.write("\n");
        fw.close();        
    }
    
    void start()
    {
        worker.start();
    }
    
    void stop()
    {
        worker.stop();
    }
    

    private JSONObject serializeCPDSettings( CPDSettings settings ) throws JSONException
    {
        JSONObject json = new JSONObject();
        
        CPDBaseSettings baseSettings = settings.getBaseSettings();  
                
        /* Save the values from the base settings */
        json.put("captureBypassedTraffic", baseSettings.getCaptureBypassedTraffic());
        json.put("concurrentLoginsEnabled", baseSettings.getConcurrentLoginsEnabled());
        json.put("authenticationType", baseSettings.getAuthenticationType().toString());
        json.put("idleTimeout", baseSettings.getIdleTimeout());
        json.put("timeout", baseSettings.getTimeout());
        json.put("logoutButtonEnabled", baseSettings.getLogoutButtonEnabled());
        json.put("pageType", baseSettings.getPageType().toString());
        json.put("pageParameters", new JSONObject(baseSettings.getPageParameters()));
        json.put("redirectHttpsEnabled", baseSettings.getRedirectHttpsEnabled());
        json.put("redirectUrl", baseSettings.getRedirectUrl());
        json.put("useHttpPage", baseSettings.getUseHttpsPage());
        
        JSONArray captureRules = new JSONArray();
        
        /* Add the passed clients and addresses first, and then add the capture rules */
        for ( PassedClient client : settings.getPassedClients()) {
            captureRules.put(serializePassedAddress(client.getAddress(), IPSimpleMatcher.getAllMatcher()));
        }
        
        for ( PassedServer server : settings.getPassedServers()) {
            captureRules.put(serializePassedAddress(IPSimpleMatcher.getAllMatcher(), server.getAddress()));
        }
        
        for ( CaptureRule captureRule : settings.getCaptureRules()) {
            captureRules.put(serializeCaptureRule(captureRule));
        }
        
        return json;
    }

    private JSONObject serializeCaptureRule(CaptureRule captureRule) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("live", captureRule.isLive());
        json.put("description",captureRule.getDescription());
        json.put("capture", captureRule.getCapture());
        json.put("clientAddress", captureRule.getClientAddress().toDatabaseString());
        json.put("serverAddress",captureRule.getServerAddress());
        json.put("clientInterface", captureRule.getClientInterface().toDatabaseString());
        json.put("days",captureRule.getDays());
        json.put("startTime", captureRule.getStartTime());
        json.put("endTime", captureRule.getEndTime());
        return json;
    }
    
    private JSONObject serializePassedAddress(IPDBMatcher client, IPDBMatcher server)
            throws JSONException {
        return serializeCaptureRule(new CaptureRule(true, false,
                "passed client", IntfSimpleMatcher.getAllMatcher(), client,
                server, "00:00", "23:59",
                "mon,tue,wed,thu,fri,sat,sun"));
    }
    
    private class CacheMonitor implements Worker
    {
        @Override
        public void start() {
            try {
                ScriptRunner.getInstance().exec( START_SCRIPT );
            } catch ( NodeException e ) {
                throw new IllegalStateException("Unable to start Captive Portal Daemon", e);
            }
        }

        @Override
        public void stop() {
            try {
            ScriptRunner.getInstance().exec( STOP_SCRIPT );
            } catch ( NodeException e ){
                throw new IllegalStateException("Unable to stop Captive Portal Daemon", e);
            }
        }

        @Override
        public void work() throws InterruptedException {
            Thread.sleep( CACHE_DELAY_MS );
            
            CPDManager.this.cpd.getPhoneBookAssistant().clearExpiredData();
        }
    }
}
