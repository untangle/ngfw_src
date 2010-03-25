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

import java.util.Date;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.shield.ShieldRejectionEvent;
import com.untangle.uvm.shield.ShieldStatisticEvent;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ShieldManager
{
    private static final int SLEEP_DELAY_MS = 5000;
    /* Currently set to 20 minutes */
    private static final int STATISTIC_DELAY_MS = 1200000;

    private static final String START_SCRIPT = System.getProperty( "uvm.home" ) + "/shield/start";
    private static final String STOP_SCRIPT = System.getProperty( "uvm.home" ) + "/shield/stop";
    private static final String SHIELD_URL =
        System.getProperty( "uvm.shield.url", "http://localhost:3001" );

    private static final String FUNCTION_BLESS_USERS = "bless_users";
    private static final String FUNCTION_GET_LOGS = "get_logs";
    private static final String RESPONSE_STATUS = "status";
    private static final String RESPONSE_MESSAGE = "message";

    private static final int STATUS_SUCCESS = 104;

    private final Logger logger = Logger.getLogger( this.getClass());

    EventLogger<ShieldStatisticEvent> statisticLogger;
    EventLogger<ShieldRejectionEvent> rejectionLogger;

    private final WorkerRunner monitor = new WorkerRunner( new Monitor(), LocalUvmContextFactory.context());

    ShieldManager( EventLogger<ShieldStatisticEvent> statisticLogger,
                   EventLogger<ShieldRejectionEvent> rejectionLogger )
    {
        this.statisticLogger = statisticLogger;
        this.rejectionLogger = rejectionLogger;
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

        for ( ShieldNodeRule rule : settings.getShieldNodeRules()) {
            if ( !rule.isLive()) continue;

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
        json.put( "write_users", true );

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

        private JSONObject last_js = null;
        private Date last = new Date( 0 );

        /* This is the last time it logged a statistic event */
        private long nextStatisticEvent = System.currentTimeMillis() + STATISTIC_DELAY_MS;
        private ShieldStatisticEvent statisticEvent = new ShieldStatisticEvent();

        public void work() throws InterruptedException
        {
            Thread.sleep( SLEEP_DELAY_MS );

            logger.debug( "Fetching the logs." );

            /* xxx This should use the key to determine the data
             * mappping, right now it is hardcoded. */
            try {
                JSONObject logs = getLogs();

                JSONArray data = logs.getJSONArray( "data" );

                for ( int c = 0 ; c < data.length(); c++ ){
                    JSONObject iteration_js = data.getJSONObject( c );
                    if ( iteration_js == null ) continue;

                    try {
                        LogIteration iteration = LogIteration.parse( iteration_js );

                        /* Combine in the statistics from the other logs */
                        add( this.statisticEvent, iteration.getStatisticEvent());
                        long now = System.currentTimeMillis();

                        if ( nextStatisticEvent > ( now + ( 2 * STATISTIC_DELAY_MS ))) {
                            logger.warn( "Statistic time is too far in the future, logging now." );
                            nextStatisticEvent = 0;
                        }

                        if ( nextStatisticEvent < now ) {
                            statisticLogger.log( this.statisticEvent );
                            statisticEvent = new ShieldStatisticEvent();
                            nextStatisticEvent = now + STATISTIC_DELAY_MS;
                        }

                        for ( ShieldRejectionEvent r : iteration.getRejectionEvents()) {
                            rejectionLogger.log( r );
                        }

                        if ( last.before( iteration.getStartTime())) {
                            last = iteration.getStartTime();
                            last_js = iteration.getStart();
                        }
                    } catch ( JSONException e ) {
                        logger.info( "Unable to parse iteration: " + e.getMessage());
                    }
                }

            } catch ( Exception e ) {
                logger.warn( "Unable to retrieve the logs: '" + e.getMessage() + "'", e );
            }
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

        private JSONObject getLogs() throws JsonClient.ConnectionException, JSONException
        {
            JSONObject request = new JSONObject();
            request.put( "function", FUNCTION_GET_LOGS );
            request.put( "ignore_accept_only", true );
            if ( this.last_js != null ) request.put( "start_time", this.last_js );

            JSONObject response = JsonClient.getInstance().call( SHIELD_URL, request );

            if ( logger.isDebugEnabled()) logger.debug( "Server returned:\n" + response.toString() + "\n" );
            int status = response.getInt( RESPONSE_STATUS );
            String message = response.getString( RESPONSE_MESSAGE );

            if ( status != STATUS_SUCCESS ) {
                logger.warn( "There was an error[" + status +"] retrieving the logs: '" + message + "'" );
                return null;
            }

            return response.getJSONObject( "logs" );
        }

        private void add( ShieldStatisticEvent dest, ShieldStatisticEvent source )
        {
            dest.setAccepted( dest.getAccepted() + source.getAccepted());
            dest.setDropped( dest.getDropped() + source.getDropped());
            dest.setLimited( dest.getLimited() + source.getLimited());
            dest.setRejected( dest.getRejected() + source.getRejected());
            dest.setRelaxed( dest.getRelaxed() + source.getRelaxed());
            dest.setLax( dest.getLax() + source.getLax());
            dest.setTight( dest.getTight() + source.getTight());
            dest.setClosed( dest.getClosed() + source.getClosed());
        }
    }
}
