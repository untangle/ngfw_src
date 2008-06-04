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
package com.untangle.node.shield;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.node.script.ScriptRunner;

import com.untangle.uvm.LocalUvmContextFactory;

import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;

import com.untangle.uvm.node.NodeException;



class ShieldManager
{
    private static final int SLEEP_DELAY_MS = 60000;
    private static final String START_SCRIPT = System.getProperty( "bunnicula.home" ) + "/shield/start";
    private static final String STOP_SCRIPT = System.getProperty( "bunnicula.home" ) + "/shield/stop";
    private static final String SHIELD_URL = 
        System.getProperty( "uvm.shield.url", "http://localhost:3001" );

    private static final String FUNCTION_BLESS_USERS = "bless_users";
    private static final String FUNCTION_GET_LOGS = "get_logs";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";

    private static final int STATUS_SUCCESS = 104;    

    private final Logger logger = Logger.getLogger( this.getClass());

    private final WorkerRunner monitor = new WorkerRunner( new Monitor(), LocalUvmContextFactory.context());

    ShieldManager()
    {
    }

    void start()
    {
        this.monitor.start();
    }
    
    void stop()
    {
        this.monitor.stop();
    }

    void blessUsers( ShieldSettings settings ) throws JsonClient.ConnectionException, JSONException
    {
        /* Convert the settings to a JSON Object */
        JSONObject json = new JSONObject();
        json.put( "function", FUNCTION_BLESS_USERS );
        /* No need to save the config */
        json.put( "write_config", false );

        JSONArray users = new JSONArray();
        
        for ( ShieldNodeRule rule : settings.getShieldNodeRuleList()) {
            JSONObject rule_json = new JSONObject();
            String temp = rule.getAddressString();
            if ( temp.length() == 0 ) continue;

            rule_json.put( "ip", temp );
            temp = rule.getNetmaskString();
            if ( temp.length() == 0 ) temp = "255.255.255.255";
            rule_json.put( "netmask", temp );

            rule_json.put( "divider", rule.getDivider());
            
            users.put( rule_json );
        }

        json.put( "users", users );
        
        JSONObject response = JsonClient.getInstance().call( SHIELD_URL, json );
        if ( logger.isDebugEnabled()) logger.debug( "Server returned:\n" + response.toString() + "\n" );

        int status = response.getInt( RESPONSE_STATUS );
        String message = response.getString( RESPONSE_MESSAGE );
        
        if ( status != STATUS_SUCCESS ) {
            logger.warn( "There was an error[" + status +"] blessing the users: '" + message + "'" );
        }
    }

    private final class Monitor implements Worker
    {
        public void work() throws InterruptedException
        {
            Thread.sleep( SLEEP_DELAY_MS );
        }

        public void start()
        {
            try {
                /* Start shield daemon */
                ScriptRunner.getInstance().exec( START_SCRIPT );
            } catch ( NodeException e ) {
                logger.error( "Unable to start the shield.", e );
            }
        }
    
        public void stop()
        {
            try {
                /* Stop the shield daemon */
                ScriptRunner.getInstance().exec( STOP_SCRIPT );
            } catch ( NodeException e ) {
                logger.error( "Unable to stop the shield.", e );
            }
        }
    }
}
    
