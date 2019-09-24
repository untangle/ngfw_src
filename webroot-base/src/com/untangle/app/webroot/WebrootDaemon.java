/**
 * $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */
package com.untangle.app.webroot;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.uvm.util.Pulse;

/**
 * Queries for webroot daemon
 */
public class WebrootDaemon
{
    private final Logger logger = Logger.getLogger(WebrootDaemon.class);

    /**
     * The singleton instance
     */
    private static final WebrootDaemon INSTANCE = new WebrootDaemon();

    private static AtomicInteger AppCount = new AtomicInteger();

    private final String BCTID_CONFIG_FILE = "/etc/bctid/bcti.cfg";
    private final String BCTID_CONFIG_DEVICE_KEY = "Device=";
    private final String BCTID_CONFIG_DEVICE_VALUE = "NGFirewall";
    private final String BCTID_CONFIG_DEVICE_DEVELOPER_VALUE = "_Internal";
    private final String BCTID_CONFIG_UID_KEY = "UID=";

    /**
     * Pulse thread to read btci daemon statistics.
     */
    private final long DEFAULT_GET_STATISTICS_INTERVAL_MS = (long) 30 * 1000; /* every 30 seconds */
    private final long DEFAULT_GET_STATISTICS_RUN_TIMEOUT_MS = (long) 60 * 60 * 1000; /* Kill process after 60 minutes.  */

    /**
     * These store this app's metrics (for display in the UI) The hash map is
     * for fast lookups The list is to maintain order for the UI
     */
    public static final String STAT_CACHE_COUNT = "webroot_cache_count";
    public static final String STAT_NETWORK_ERROR_COUNT = "webroot_network_error_count";
    public static final String STAT_IP_ERROR_COUNT = "webroot_ip_error_count";
    // ??? atomic?
    private Map<String, AppMetric> metrics = new ConcurrentHashMap<>();
    private List<AppMetric> metricList = new ArrayList<>();

    private Pulse pulseGetStatistics = null;

    /**
     * Constructor
     */
    private WebrootDaemon()
    {
        pulseGetStatistics = new Pulse("decision-get-statistics", new GetStatistics(), DEFAULT_GET_STATISTICS_INTERVAL_MS, false, DEFAULT_GET_STATISTICS_RUN_TIMEOUT_MS);
        addMetric(new AppMetric(STAT_CACHE_COUNT, I18nUtil.marktr("Cache count")));
        addMetric(new AppMetric(STAT_NETWORK_ERROR_COUNT, I18nUtil.marktr("Network error count")));
        addMetric(new AppMetric(STAT_IP_ERROR_COUNT, I18nUtil.marktr("IP error count")));
    }

    /**
     * Get our singleton instance
     * 
     * @return The instance
     */
    public synchronized static WebrootDaemon getInstance()
    {
        return INSTANCE;
    }

    /**
     * Determine if daemon is ready (running)
     * @return boolea nwhere if true means daemon is ready, false it is not.
     */
    public boolean isReady()
    {
        return AppCount.get() > 0;
    }

    /**
     * Start the statistics gatherer.
     */
    public void start()
    {
        boolean firstIn = AppCount.get() == 0;
        AppCount.incrementAndGet();
        if(firstIn){
            reconfigure();
        }

        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-bctid");

        if(firstIn){
            restart();
            try{
                WebrootQuery.start();
            }catch(Exception e){
                logger.warn(e);
            }
            pulseGetStatistics.start();
        }
    }

    /**
     * Close the bcti sockets.
     */
    public void stop()
    {
        boolean lastOut = AppCount.decrementAndGet() == 0;
        if( lastOut ){
            if(pulseGetStatistics.getState() == Pulse.PulseState.RUNNING){
                pulseGetStatistics.stop();
            }
            WebrootQuery.stop();
        }
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-bctid");
    }

    /**
     * Restart the daemon.
     */
    public void restart()
    {
        UvmContextFactory.context().daemonManager().restart("untangle-bctid");
    }

    /**
     * Return current brightcloud configuration as a map of section/key/value.
     * @return Hash map of section/key/value.
     */
    public Map<String,Map<String,String>> getConfig()
    {
        Map<String,Map<String,String>> configMap = new HashMap<>();
        File f = new File(BCTID_CONFIG_FILE);
        if( f.exists() == false ){
            logger.info("getConfig: bctid configuration not found: " + BCTID_CONFIG_FILE);
        }else{
            String[] config = null;
            FileInputStream is = null;
            try{
                is = new FileInputStream(BCTID_CONFIG_FILE);
                config = IOUtils.toString(is, "UTF-8").split("\n");
            }catch (Exception e){
                logger.error("getConfig: read config",e);
            }finally{
                try{
                    if(is != null){
                        is.close();
                    }
                }catch( IOException e){
                    logger.error("getConfig: failed to close file");
                }
            }
            if(config != null){
                int splitIndex = 0;
                String currentSectionName = null;
                Map<String,String> currentSection = null;
                for(int i = 0; i < config.length; i++){
                    if(config[i].startsWith("[")){
                        if(currentSectionName != null && currentSection != null){
                            configMap.put(currentSectionName, currentSection);
                        }
                        currentSectionName = config[i].substring(config[i].indexOf('[') + 1, config[i].indexOf(']') );
                        currentSection = new HashMap<>();
                    }else if(
                        config[i].trim().isEmpty() == false &&
                        config[i].trim().startsWith("#") == false){
                        String[] keyValuePair = config[i].split("=", 2);
                        if(keyValuePair.length == 2){
                            currentSection.put(keyValuePair[0], keyValuePair[1]);
                        }
                    }
                }
                if(currentSectionName != null && currentSection != null){
                    configMap.put(currentSectionName, currentSection);
                }
            }
        }
        return configMap;
    }

    /**
     * Reconfigure the decision engine.
     */
    private void reconfigure(){
        // Update bctid configuration.
        File f = new File(BCTID_CONFIG_FILE);
        if( f.exists() == false ){
            logger.info("reconfigure: bctid configuration not found: " + BCTID_CONFIG_FILE);
        }else{
            String[] config = null;
            FileInputStream is = null;
            try{
                is = new FileInputStream(BCTID_CONFIG_FILE);
                config = IOUtils.toString(is, "UTF-8").split("\n");
            }catch (Exception e){
                logger.error("reconfigure: read config",e);
            }finally{
                try{
                    if(is != null){
                        is.close();
                    }
                }catch( IOException e){
                    logger.error("reconfigure: failed to close file");
                }
            }
            if(config != null){
                boolean changed = false;
                String deviceValue = BCTID_CONFIG_DEVICE_VALUE;
                if(UvmContextFactory.context().isDevel()){
                    deviceValue += BCTID_CONFIG_DEVICE_DEVELOPER_VALUE;
                }
                String uidValue = UvmContextFactory.context().getServerUID();
                for(int i = 0; i < config.length; i++){
                    if(config[i].startsWith(BCTID_CONFIG_DEVICE_KEY) && 
                       !config[i].equals(BCTID_CONFIG_DEVICE_KEY + deviceValue)){
                        config[i] = BCTID_CONFIG_DEVICE_KEY + deviceValue;
                        changed = true;
                    }else if(config[i].startsWith(BCTID_CONFIG_UID_KEY) &&
                        !config[i].equals(BCTID_CONFIG_UID_KEY + uidValue)){
                        config[i] = BCTID_CONFIG_UID_KEY + uidValue;
                        changed = true;
                    }
                }
                if(changed){
                    try(FileOutputStream fos = new FileOutputStream(f)){
                        fos.write(String.join("\n", config).getBytes());
                    }catch(Exception e){
                        logger.warn("reconfigure: write file failed with ", e);
                    }
                }
            }
        }
    }

    /**
     * Add daemon metric
     * @param metric AppMetric to add.
     */
    private void addMetric(AppMetric metric)
    {
        if (metrics.get(metric.getName()) != null) {
            //logger.warn("addMetric(): Metric already exists: \"" + metric.getName() + "\" - ignoring");
            return;
        }
        metrics.put(metric.getName(), metric);
        metricList.add(metric);
    }

    /**
     * Renew the doman/group cache across all available domains.
     */
    private class GetStatistics implements Runnable
    {
        /**
         * Cache update process.
         */
        public void run()
        {
            try{
                JSONArray statusResult = WebrootQuery.getInstance().status();
                if(statusResult != null){
                    JSONObject status = statusResult.getJSONObject(0);
                    // !!! KEYS FROM QUERY
                    metrics.get(STAT_CACHE_COUNT).setValue(new Long(status.getJSONObject("url_db").getInt("url_cache_current_size")));
                    metrics.get(STAT_NETWORK_ERROR_COUNT).setValue(new Long(status.getJSONObject("counters").getJSONObject("errors").getInt("network")));
                    metrics.get(STAT_IP_ERROR_COUNT).setValue(new Long(status.getJSONObject("counters").getJSONObject("errors").getInt("ip")));
                }
            }catch(Exception e){
                logger.warn("Unable to query status",e);
            }
        }
    }
}