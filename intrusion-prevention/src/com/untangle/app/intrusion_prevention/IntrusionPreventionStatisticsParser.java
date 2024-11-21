/**
 * $Id: IntrusionPreventionSnortStatisticsParser.java 31685 2014-11-24 15:50:30Z cblaise $
 */
package com.untangle.app.intrusion_prevention;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;

import com.untangle.uvm.ExecManager;
import com.untangle.uvm.UvmContextFactory;

/**
 * Process the snort text log to derive information for metrics.
 */
public class IntrusionPreventionStatisticsParser
{
    private static final String STATISTICS_COMMAND = "/usr/bin/suricatasc -c 'dump-counters'";

    private static final String STATISTICS_RETURN_KEY = "return";
    private static final String STATISTICS_RETURN_VALID = "OK";
    private static final String STATISTICS_RETURN_INVALID = "NOK";

    private static final String STATISTICS_MESSAGE_KEY = "message";

    // These keys are below the MESSAGE tree.
    private static final String STATISTICS_DETECT_KEY = "detect";
    private static final String STATISTICS_DETECT_LOG_KEY = "alert";

    private static final String STATISTICS_IPS_KEY = "ips";
    private static final String STATISTICS_IPS_REJECTED_KEY = "rejected";
    private static final String STATISTICS_IPS_BLOCKED_KEY = "blocked";
    private static final String STATISTICS_IPS_ACCEPTED_KEY = "accepted";

    private final Logger logger = LogManager.getLogger(getClass());

    protected static ExecManager execManager = null;

    /**
     * Iniitialize statistics parser.
     */
    public IntrusionPreventionStatisticsParser()
    {
        if ( IntrusionPreventionStatisticsParser.execManager == null) {
            IntrusionPreventionStatisticsParser.execManager = UvmContextFactory.context().createExecManager();
            IntrusionPreventionStatisticsParser.execManager.setLevel( Level.DEBUG );    
        }
    }

    /**
     * Read the log file.
     *
     * @param ipsApp
     *  Intrusion Prevention application.
     */
	public void parse( IntrusionPreventionApp ipsApp )
    {

        String result = IntrusionPreventionStatisticsParser.execManager.execOutput( STATISTICS_COMMAND );
        if(result.substring(0,1).equals("{")){
            try{
                JSONObject resultJson = new JSONObject(result);
                if(resultJson.get(STATISTICS_RETURN_KEY).toString().equals(STATISTICS_RETURN_INVALID)){
                    ipsApp.setDaemonReady(false);
                    return;
                }
                ipsApp.setDaemonReady(true);
                resultJson = resultJson.getJSONObject(STATISTICS_MESSAGE_KEY);
                ipsApp.setMetricsDetectCount( resultJson.getJSONObject(STATISTICS_DETECT_KEY).getInt(STATISTICS_DETECT_LOG_KEY) );
                /**
                 * If block action is reject (default), use the ips/rejected counter.
                 * Otherwise for drop, use ips/blocked.
                 * In reject mode, ips/blocked has values that seem to be wrong and not actually reflect reject actions.
                 */
                long blocked = resultJson.getJSONObject(STATISTICS_IPS_KEY).getInt(ipsApp.getSettings().getBlockAction().equals("drop") ? STATISTICS_IPS_BLOCKED_KEY : STATISTICS_IPS_REJECTED_KEY);
                ipsApp.setMetricsBlockCount( blocked);
                ipsApp.setMetricsScanCount( resultJson.getJSONObject(STATISTICS_IPS_KEY).getInt(STATISTICS_IPS_ACCEPTED_KEY) + blocked);
                ipsApp.updateMetricsMemory();
            }catch(Exception e){
                logger.warn("IntrusionPreventionStatisticsParser, parse", e);
            }
        }
    }
}
