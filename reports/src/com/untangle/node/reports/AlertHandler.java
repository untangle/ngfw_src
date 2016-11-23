/**
 * $Id: AlertHandler.java,v 1.00 2014/11/15 11:56:09 dmorris Exp $
 */
package com.untangle.node.reports;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import java.util.LinkedList;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This contains the logic for handling the alert rules.
 * This is called by the EventWriter
 */
@SuppressWarnings("serial")
public class AlertHandler
{
    private static final Logger logger = Logger.getLogger( AlertHandler.class );

    public static void runAlertRules( LinkedList<AlertRule> alertRules, LinkedList<LogEvent> events, ReportsApp reports )
    {
        for ( LogEvent event : events ) {
            runAlertRules( alertRules, event, reports );
        }
    }
    
    public static void runAlertRules( LinkedList<AlertRule> alertRules, LogEvent event, ReportsApp reports )
    {
            try {
                JSONObject jsonObject = event.toJSONObject();
                if ( ! ( event instanceof AlertEvent ) ) {
                    for ( AlertRule rule : alertRules ) {
                        if ( ! rule.getEnabled() )
                            continue;
                
                        if ( rule.isMatch( jsonObject ) ) {
                            logger.info( "alert match: " + rule.getDescription() + " matches " + jsonObject.toString() );

                            if ( rule.getLog() )
                                UvmContextFactory.context().logEvent( new AlertEvent( rule.getDescription(), event.toSummaryString(), jsonObject, event ) );
                            if ( rule.getAlert() ) 
                                sendAlertForEvent( rule, event, reports );
                        } 
                    }
                }
            } catch ( Exception e ) {
                logger.warn("Failed to evaluate alert rules.", e);
            }
    }

    private static void sendAlertForEvent( AlertRule rule, LogEvent event, ReportsApp reports )
    {
        if ( rule.getAlertLimitFrequency() && rule.getAlertLimitFrequencyMinutes() > 0 ) {
            long currentTime = System.currentTimeMillis();
            long lastAlertTime = rule.lastAlertTime();
            long secondsSinceLastAlert = ( currentTime - lastAlertTime ) / 1000;
            // if not enough time has elapsed, just return
            if ( secondsSinceLastAlert < ( rule.getAlertLimitFrequencyMinutes() * 60 ) )
                return;
        }

        rule.updateAlertTime();

        String companyName = UvmContextFactory.context().brandingManager().getCompanyName();
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String fullName = hostName + (  domainName == null ? "" : ("."+domainName));
        String serverName = companyName + " " + I18nUtil.marktr("Server");
        JSONObject jsonObject = event.toJSONObject();
        String jsonEvent;

        cleanupJsonObject( jsonObject );
        
        try {
            jsonEvent = jsonObject.toString(4);
        } catch (org.json.JSONException e) {
            logger.warn("Failed to pretty print.",e);
            jsonEvent = jsonObject.toString();
        }
        
        String subject = serverName + " " +
            I18nUtil.marktr("Alert!") +
            " [" + fullName + "] ";

        String messageBody = I18nUtil.marktr("The following event occurred on the") + " " + serverName + " @ " + event.getTimeStamp() +
            "\r\n\r\n" +
            rule.getDescription() + ":" + "\r\n" +
            event.toSummaryString() +
            "\r\n\r\n" +
            I18nUtil.marktr("Causal Event:") + " " + event.getClass().getSimpleName() + 
            "\r\n" +
            jsonEvent + 
            "\r\n\r\n" +
            I18nUtil.marktr("This is an automated message sent because the event matched the configured Alert Rules.");
                              
        if ( reports.getSettings().getReportsUsers() != null ) {
            for ( ReportsUser user : reports.getSettings().getReportsUsers() ) {
                if ( user.getEmailAddress() == null || "".equals( user.getEmailAddress() ) )
                    continue;
                if ( ! user.getEmailAlerts() )
                    continue;
                try {
                    String[] recipients = new String[]{ user.getEmailAddress() };
                    logger.warn("Sending alert to " + user.getEmailAddress());
                    UvmContextFactory.context().mailSender().sendMessage( recipients, subject, messageBody);
                } catch ( Exception e) {
                    logger.warn("Failed to send mail.",e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void cleanupJsonObject( JSONObject jsonObject )
    {
        if ( jsonObject == null )
            return;

        java.util.Iterator<String> keys = (java.util.Iterator<String>)jsonObject.keys();
        while ( keys.hasNext() ) {
            String key = keys.next();

            if ("class".equals(key)) {
                keys.remove();
                continue;
            }
            if ("tag".equals(key)) {
                keys.remove();
                continue;
            }
            if ("partitionTablePostfix".equals(key)) {
                keys.remove();
                continue;
            }

            /**
             * Recursively clean json objects
             */
            try {
                JSONObject subObject = jsonObject.getJSONObject(key);
                if (subObject != null) {
                    cleanupJsonObject( subObject );
                }
            } catch (Exception e) {
                /* ignore */
            }

            /**
             * If the object implements JSONString, then its probably a jsonObject
             * Convert to JSON Object, recursively clean that, then replace it
             */
            try {
                if ( jsonObject.get(key) != null ) {
                    Object o = jsonObject.get(key);
                    if ( o instanceof org.json.JSONString ) {
                        JSONObject newObj = new JSONObject( o );
                        cleanupJsonObject( newObj );
                        jsonObject.put( key, newObj );
                    }
                }
            } catch (Exception e) {
                /* ignore */
            }
            
        }
    }
}

