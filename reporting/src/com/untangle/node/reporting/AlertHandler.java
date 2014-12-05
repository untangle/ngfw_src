/**
 * $Id: AlertHandler.java,v 1.00 2014/11/15 11:56:09 dmorris Exp $
 */
package com.untangle.node.reporting;

import org.json.JSONObject;

import org.apache.log4j.Logger;

import java.util.LinkedList;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.AdminUserSettings;

/**
 * This contains the logic for handling the alert rules.
 * This is called by the EventWriter
 */
@SuppressWarnings("serial")
public class AlertHandler
{
    private static final Logger logger = Logger.getLogger( AlertHandler.class );

    public static void runAlertRules( LinkedList<AlertRule> alertRules, LinkedList<LogEvent> events )
    {
        for ( LogEvent event : events ) {
            runAlertRules( alertRules, event );
        }
    }
    
    public static void runAlertRules( LinkedList<AlertRule> alertRules, LogEvent event )
    {
            try {
                JSONObject jsonObject = event.toJSONObject();
                if ( ! ( event instanceof AlertEvent ) ) {
                    for ( AlertRule rule : alertRules ) {
                        if ( ! rule.getEnabled() )
                            continue;
                
                        if ( rule.isMatch( jsonObject ) ) {
                            logger.warn( "XXX MATCH: " + rule.getDescription() + " matches " + jsonObject.toString() );

                            if ( rule.getLog() )
                                UvmContextFactory.context().logEvent( new AlertEvent( rule.getDescription(), event.toSummaryString(), jsonObject, event ) );
                            if ( rule.getAlert() ) 
                                sendAlertForEvent( rule, event );
                        } 
                    }
                }
            } catch ( Exception e ) {
                logger.warn("Failed to evaluate alert rules.", e);
            }
    }

    private static void sendAlertForEvent( AlertRule rule, LogEvent event )
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
        String serverName = companyName + " " + I18nUtil.marktr("Server");

        String subject = serverName + " " +
            I18nUtil.marktr("Alert!") +
            " [" + hostName + "] ";

        String messageBody = I18nUtil.marktr("An following event occurred on the") + " " + serverName + " @ " + event.getTimeStamp() +
            "\r\n\r\n" +
            rule.getDescription() + ":" + "\r\n" +
            event.toSummaryString() +
            "\r\n\r\n" +
            event.toJSONObject().toString() + 
            "\r\n\r\n" +
            I18nUtil.marktr("This is an automated message sent because the event matched the configured Alert Rules.");
                              

        for( AdminUserSettings admin : UvmContextFactory.context().adminManager().getSettings().getUsers() ) {
            if ( admin.getEmailAddress() == null || "".equals( admin.getEmailAddress() ) )
                continue;

            try {
                String[] recipients = new String[]{ admin.getEmailAddress() };
                logger.warn("Sending alert to " + admin.getEmailAddress());
                
                UvmContextFactory.context().mailSender().sendMessage( recipients, subject, messageBody);
            } catch ( Exception e) {
                logger.warn("Failed to send mail.",e);
            }
        }
    }

}

