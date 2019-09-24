/** $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.webroot;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.log4j.Logger;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * Queries for webroot
 */
public class WebrootQuery
{
    private static final Logger logger = Logger.getLogger(WebrootQuery.class);
    /**
     * The singleton instance
     */
    private static final WebrootQuery INSTANCE = new WebrootQuery();

    private static WebrootDaemon webrootDaemon = WebrootDaemon.getInstance();

    public static final char DOMAIN_PORT = ':';
    public static final char DOMAIN_DOT = '.';
    public static final String DOMAIN_WILDCARD = "*.";

    private static final DateFormat BCTI_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
    private static final String BCTI_DATE_TIME = "T00:00:00";
    private static final int BCTI_DATE_MAX_START_DAYS = 180;

    public static final String BCTI_API_ERROR_KEY="ERROR";

    public static final String BCTI_API_DIRECT_REQUEST_IPS_PARAMETER = "%ips%";
    public static final String BCTI_API_DIRECT_REQUEST_URLS_PARAMETER = "%urls%";
    public static final String BCTI_API_DIRECT_REQUEST_STARTDATE_PARAMETER = "%startdate%";
    public static final String BCTI_API_DIRECT_REQUEST_ENDDATE_PARAMETER = "%enddate%";

    public static final String BCTI_API_DIRECT_RESPONSE_QUERIES_KEY = "queries";

    public static final String BCTI_API_DIRECT_URL_PREFIX_TEMPLATE = "%protocol%://%server%/1.0/";
    public static final String BCTI_API_DIRECT_REQUEST_PREFIX_TEMPLATE = "{\"requestid\": \"%requestid%\", \"oemid\": \"%oemid%\",\"deviceid\": \"%deviceid%\",\"uid\": \"%uid%\",";
    public static final String BCTI_API_DIRECT_REQUEST_SUFFIX_TEMPLATE = ",\"xml\": 0}";

    public static final String BCTI_API_DIRECT_REQUEST_URL = "url";
    public static final String BCTI_API_DIRECT_REQUEST_URL_GETREPINFO = "\"queries\": [\"getrepinfo\",],\"urls\": [\"%urls%\"]";

    public static final String BCTI_API_DIRECT_REQUEST_URLTHREATINSIGHT = "urlthreatinsight";
    public static final String BCTI_API_DIRECT_REQUEST_URLTHREATINSIGHT_GETURLINFO = "\"queries\": [\"geturlhistory\",], \"startdate\": \"%startdate%\",\"enddate\": \"%enddate%\",\"values\": [\"%urls%\"]";

    public static final String BCTI_API_DIRECT_REQUEST_IP = "ip";
    public static final String BCTI_API_DIRECT_REQUEST_IP_GETREPHISTORY = "\"queries\": [\"getrephistory\",],\"ips\": [\"%ips%\"]";
    public static final String BCTI_API_DIRECT_REQUEST_IP_GETTHREATHISTORY = "\"queries\": [\"getthreathistory\",],\"ips\": [\"%ips%\"]";

    public static final String BCTI_API_DIRECT_REQUEST_IPTHREATINSIGHT = "ipthreatinsight";
    public static final String BCTI_API_DIRECT_REQUEST_IPTHREATINSIGHT_GETIPEVIDENCE = "\"queries\": [\"getipevidence\",],\"values\": [\"%ips%\"]";

    public static final String BCTI_API_DAEMON_REQUEST_URLINFO_PREFIX = "{\"url/getinfo\":{\"urls\":[\"";
    public static final String BCTI_API_DAEMON_REQUEST_URLINFO_SUFFIX = "\"],\"a1cat\":1, \"reputation\":1}}\r\n";

    public static final String BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_LIST_KEY="cats";
    public static final String BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_ID_KEY="catid";
    public static final String BCTI_API_DAEMON_RESPONSE_URLINFO_A1CAT_KEY="a1cat";

    public static final String BCTI_API_DAEMON_RESPONSE_URLINFO_URL_KEY="url";
    public static final String BCTI_API_DAEMON_RESPONSE_URLINFO_REPUTATION_KEY="reputation";

    public static final String BCTI_API_DAEMON_REQUEST_IPINFO_PREFIX = "{\"ip/getinfo\":{\"ips\":[\"";
    public static final String BCTI_API_DAEMON_REQUEST_IPINFO_SUFFIX = "\"]}}\r\n";

    public static final String BCTI_API_DAEMON_RESPONSE_IPINFO_IP_KEY="ip";
    public static final String BCTI_API_DAEMON_RESPONSE_IPINFO_THREATMASK_KEY="threat_mask";
    public static final String BCTI_API_DAEMON_RESPONSE_IPINFO_REPUTATION_KEY="reputation";

    public static final String BCTI_API_DAEMON_REQUEST_URLCLEARCACHE = "{\"url/setcacheclear\":{}}\r\n";
    public static final String BCTI_API_DAEMON_REQUEST_STATUS = "{\"status\":{}}\r\n";

    public static String BctiApiDirectUrlPrefix = "";
    public static String BctiApiDirectRequestPrefix = "";
    public static String BctiApiDirectRequestSuffix = "";
    public static Integer requestId = 0;

    private static Integer UNCATEGORIZED_CATEGORY = 0;

    static private InetSocketAddress BCTID_SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", 8484);

    private static final long BCTID_CONNECT_WAIT = 1 * 1000;
    private long BctidSocketConnectWait = 0L;

    private static final long BCTID_RETRY_INTERVAL = 60 * 1000;
    private static final int BCTID_RETRY_MAX_COUNT = 5;
    private static AtomicInteger BctidRetryCount = new AtomicInteger(0);
    private static AtomicLong BctidRetryIntervalExpire = new AtomicLong(0L);

    // private static Integer BctidMaxSocketPoolSize = 30;
    private static Integer BctidMaxSocketPoolSize = 2;

    private static ArrayBlockingQueue<Socket> BctidSocketPool = new ArrayBlockingQueue<>(BctidMaxSocketPoolSize);
    private static AtomicInteger BctidSocketRunnersCount = new AtomicInteger();
    private static long BctidSocketPoolMaxWaitSeconds = 5L;

    private static int BctidClientConnectTimeout = 250;
    private static int BctidClientReadTimeout = 2500;

    private int failures = 0;

    private static AtomicInteger ipCacheSync = new AtomicInteger(0);
    private static WebrootCache ipCache = null;
    private static AtomicInteger urlCacheSync = new AtomicInteger(0);
    private static WebrootCache urlCache = null;
    private static WebrootCache urlA1Cache = null;
    private static AtomicInteger ipHistoryCacheSync = new AtomicInteger(0);
    private static WebrootCache ipHistoryCache = null;
    private static AtomicInteger urlHistoryCacheSync = new AtomicInteger(0);
    private static WebrootCache urlHistoryCache = null;

    static {
        ipCache = new WebrootCache();
        urlCache = new WebrootCache();
        urlA1Cache = new WebrootCache();
        ipHistoryCache = new WebrootCache();
        urlHistoryCache = new WebrootCache();
    }

    /**
     * Get our singleton instance
     * 
     * @return The instance
     */
    public synchronized static WebrootQuery getInstance()
    {
        return INSTANCE;
    }

    /**
     * Build up direct parameters.
     */
    public static void start(){
        Map<String,Map<String,String>> config = WebrootDaemon.getInstance().getConfig();
        BctiApiDirectUrlPrefix = BCTI_API_DIRECT_URL_PREFIX_TEMPLATE
            .replaceAll("%protocol%", config.get("Global").get("IsisPort").equals("443") ? "https" : "http")
            .replaceAll("%server%", config.get("Global").get("IsisServer"));
        BctiApiDirectRequestPrefix = BCTI_API_DIRECT_REQUEST_PREFIX_TEMPLATE
            .replaceAll("%oemid%", config.get("Global").get("OEM"))
            .replaceAll("%deviceid%", config.get("Global").get("Device"))
            .replaceAll("%uid%", config.get("Global").get("UID"));
        BctiApiDirectRequestSuffix = BCTI_API_DIRECT_REQUEST_SUFFIX_TEMPLATE;
    }

    /**
     * Stop queries, flush pool.
     */
    public static void stop(){
        Socket socket = null;
        try{
            Thread.sleep(2 * 1000);
            while((socket = BctidSocketPool.poll(BctidSocketPoolMaxWaitSeconds, TimeUnit.SECONDS)) != null){
                socket.close();
            }
        }catch(Exception e){
            logger.warn("Unable to properly stop", e);
        }
        BctidSocketPool.clear();
        BctidSocketRunnersCount.set(0);        
    }

    /**
     * Clear the host cache
     */
    public void clearCache()
    {
        apiDaemon(BCTI_API_DAEMON_REQUEST_URLCLEARCACHE);
    }

    /**
     * Lookup a site
     * 
     * @param url
     *        The URL
     * @return The category list
     */
    public List<Integer> lookupSite(String url)
    {
        // String[] urlSplit = splitUrl(url);
        // return categorizeSite(urlSplit[0], urlSplit[2]);
        return null;
    }

    /**
     * Re-categorize a site
     * 
     * @param url
     *        The URL
     * @param category
     *        The category
     * @return The category
     */
    public int recategorizeSite(String url, int category)
    {
        // String[] urlSplit = splitUrl(url);
        // String question = encodeDnsQuery(urlSplit[0], urlSplit[2], Integer.toString(category) + "_a");
        // String[] answers = lookupDns(question);
        // int answerCategory = -1;
        // if (answers.length > 0) {
        //     /**
        //      * The only way this can fail is if zvelo removes the submit
        //      * functionality.
        //      */
        //     String[] split = answers[0].split("\\s+");
        //     if (-1 != url.indexOf(split[1])) {
        //         answerCategory = category;
        //     }
        // }
        // return (category == answerCategory ? category : -1);
        return -1;
    }

    /**
     * Shutdown the static pool.
     */
    static void closeBctidSockets(){
        try{
            Socket socket = null;
            while((socket = BctidSocketPool.poll(BctidSocketPoolMaxWaitSeconds, TimeUnit.SECONDS)) != null){
                socket.close();
            }
            BctidSocketPool.clear();
        }catch(Exception e){
            logger.warn("closeBctidSockets:", e);
        }
    }

    /**
     * Query daemon.
     * @param  query        String of bcti query to send
     * @return              String of json response.
     */
    public JSONArray apiDaemon(String query)
    {
        return apiDaemon(query, false);
    }

    /**
     * Query daemon.
     * @param  query        String of bcti query to send
     * @param  retry        Boolean to retry again.
     * @return              String of json response.
     */
    public JSONArray apiDaemon(String query, Boolean retry)
    {
        JSONArray answer = null;
        String rawAnswer = null;

        if(webrootDaemon.isReady() == false){
            return answer;
        }
        boolean failed = false;
        Socket bctidSocket = null;
        try{
            synchronized(BctidSocketRunnersCount){
                if((BctidSocketRunnersCount.get() + BctidSocketPool.size()) < BctidMaxSocketPoolSize){
                    BctidSocketRunnersCount.incrementAndGet();
                    bctidSocket = new Socket();
                    bctidSocket.connect(BCTID_SOCKET_ADDRESS, BctidClientConnectTimeout);
                    bctidSocket.setKeepAlive(true);
                }
            }
            if(bctidSocket == null){
                bctidSocket = BctidSocketPool.poll(BctidSocketPoolMaxWaitSeconds, TimeUnit.SECONDS);
                if(bctidSocket == null){
                    logger.warn("apiDaemon: timed out getting socket from pool!" + BctidSocketRunnersCount.get() + ":" + BctidSocketPool.size());
                    return answer;
                }
                BctidSocketRunnersCount.incrementAndGet();
            }

            StringBuilder responseBuilder = new StringBuilder(1024);
            bctidSocket.setSoTimeout(BctidClientReadTimeout);
            InputStream is = null;
            bctidSocket.getOutputStream().write(query.getBytes());
            bctidSocket.getOutputStream().flush();
            is = bctidSocket.getInputStream();

            // !!! look for \r\n not just \n
            boolean rFound = false;
            boolean nFound = false;
            byte payloadBuffer[] = new byte[1024];
            int c = 0;
            int payloadRead = 0;
            int sbLength = 0;
            do{
                payloadRead = is.read(payloadBuffer);
                if((char) payloadBuffer[payloadRead - 1] == '\n'){
                    nFound = true;
                }
                rawAnswer = new String(payloadBuffer, 0, payloadRead);
                if(nFound && responseBuilder.length() == 0){
                    break;
                }
                responseBuilder.append(rawAnswer);
                rawAnswer = responseBuilder.toString();
            }while(!nFound);
        }catch(ConnectException ce){
            logger.warn("Unable to connect.");
            BctidSocketRunnersCount.decrementAndGet();
            bctidSocket = null;
        }catch(Exception e){
            try{
                bctidSocket.close();
            }catch(Exception e2){
                logger.warn("close socket", e2);
            }
            bctidSocket = null;
            BctidSocketRunnersCount.decrementAndGet();
            failed = true;
            if(retry == false){
                logger.warn("problem with query ("+query+"), attemping retry");
            }else{
                logger.warn("problem with query ("+query+"), was in retry", e);
            }
        }

        if(bctidSocket != null){
            BctidSocketRunnersCount.decrementAndGet();
            BctidSocketPool.offer(bctidSocket);
        }

        if(failed){
            if(retry == false){
                return apiDaemon(query, true);
            }else{
                BctidRetryCount.incrementAndGet();
                if(BctidRetryIntervalExpire.get() == 0){
                    BctidRetryIntervalExpire.set(System.currentTimeMillis() + BCTID_RETRY_INTERVAL);
                }else if(BctidRetryIntervalExpire.get() < System.currentTimeMillis() ){
                    BctidRetryIntervalExpire.set(0);
                    if(BctidRetryCount.get() >= BCTID_RETRY_MAX_COUNT){
                        closeBctidSockets();
                        BctidRetryCount.set(0);
                        webrootDaemon.restart();
                    }
                    BctidRetryCount.set(0);
                }
            }
        }

        // logger.warn("rawAnswer=" + rawAnswer);

        try{
            answer = new JSONArray(rawAnswer);
        }catch(JSONException e){
            try{
                JSONObject jsonObjectAnswer = new JSONObject(rawAnswer);
                answer = new JSONArray();
                answer.put(0, jsonObjectAnswer);
            }catch(Exception eo){
                logger.warn("Unable to decode as an object", eo);
            }
        }catch(Exception e){
            logger.warn("Unable to decode answer", e);
        }

        if(answer != null){
            try{
                JSONObject urlAnswer = answer.getJSONObject(0);
                if(urlAnswer.has(BCTI_API_ERROR_KEY)){
                    logger.warn("apiDaemon: answer contains error: " + rawAnswer);
                    answer = null;
                }
            }catch(Exception e){
                logger.warn("Unable to detect error", e);
            }
        }

        return answer;
    }

    /**
     * Perform url getinfo query for single url.
     *
     * NOTE: This is optimized for webfilter/webmonitor queries which arrive with the
     *       domain and uri already split.
     *
     * @param  domain domain of url
     * @param  uri url path of url
     * @return     JSONArray of webroot response.
     */
    public JSONArray urlGetInfo(String domain, String uri)
    {
        /**
         * While Brightcloud can handle domains with ports its very expensive, around 100 times slower.
         */
        int i = domain.indexOf(DOMAIN_PORT);
        if (i > -1) {
            domain = domain.substring(0, i);
        }
        /**
         * If we see "*.domain.com", strip the wildcard and do lookup on remaining.
         */
        i = domain.indexOf(DOMAIN_WILDCARD);
        if (i > -1) {
            domain = domain.substring(i + 2);
        }
        if(domain.indexOf(DOMAIN_DOT) > -1){
            return urlGetInfo(domain + uri);
        }
        return null;
    }

    /**
     * Perform url getinfo query for address.
     *
     * @param  urls Strings of urls
     * @return      JSONArray of webroot response.
     */
    public JSONArray urlGetInfo(String... urls)
    {
        JSONArray answer = null;
        for(int i = 0; i < urls.length; i++){
            int strippedSlashesLength = urls[i].replace("/", "").length();
            boolean rootUrl = false;
            if(strippedSlashesLength == urls[i].length() || ( strippedSlashesLength == ( urls[i].length() - 1)) ){
                rootUrl = true;
            }
            JSONArray queryAnswer = null;
            synchronized(urlCacheSync){
                queryAnswer = urlCache.get(urls[i]);
                if(queryAnswer == null){
                    queryAnswer = urlA1Cache.get(urls[i]);
                }
            }
            if(queryAnswer == null){
                queryAnswer = apiDaemon(BCTI_API_DAEMON_REQUEST_URLINFO_PREFIX + urls[i] + BCTI_API_DAEMON_REQUEST_URLINFO_SUFFIX);
                if(queryAnswer != null){
                    boolean a1 = false;
                    try{
                        a1 = (urls.length == 1) && queryAnswer.getJSONObject(0).getBoolean(BCTI_API_DAEMON_RESPONSE_URLINFO_A1CAT_KEY);
                    }catch(Exception e){
                        logger.warn("Unable to determine a1cat: "+ queryAnswer);
                    }
                    // logger.warn("a1:"+ urls[0] + " -> " + urls[i].replace("/", "") + ":" + rootUrl + " && " + a1);
                    synchronized(urlCacheSync){
                        if(rootUrl && a1){
                            urlA1Cache.put(urls[i], queryAnswer);
                        }else{
                            urlCache.put(urls[i], queryAnswer);
                        }
                        // logger.warn("urlA1Cache=" + urlA1Cache.size() + ", urlCache="+urlCache.size() );
                    }
                }
            }
            if(queryAnswer != null){
                if( i == ( urls.length + 1 ) ){
                    answer = queryAnswer;
                }else{
                    if(answer == null){
                        answer = new JSONArray();
                    }
                    try{
                        answer.put(queryAnswer.get(0));
                    }catch(Exception e){
                        logger.warn("Unable to add queryAnswer: "+ queryAnswer);
                    }
                }
            }
        }
        return answer;
    }

    /**
     * Perform ip getinfo query for address.
     *
     * NOTE: This is optimized for webfilter/webmonitor queries which arrive with the
     *       domain and uri already split.
     *
     * @param  ips Strings of IP addresss
     * @return     JSONArray of webroot response.
     */
    public JSONArray ipGetInfo(String... ips)
    {
        JSONArray answer = null;
        String key = String.join("\",\"", ips);
        synchronized(ipCacheSync){
            answer = ipCache.get(key);
        }
        if(answer == null){
            answer = apiDaemon(BCTI_API_DAEMON_REQUEST_IPINFO_PREFIX + key + BCTI_API_DAEMON_REQUEST_IPINFO_SUFFIX);
            if(answer != null){
                synchronized(ipCacheSync){
                    ipCache.put(key, answer);
                }
            }
        }
        return answer;
    }


    /**
     * Perform direct queries to webroot servers.
     *
     * @param api Major API name.
     * @param  query Minor API name
     * @return       JSONArray of answer.
     */
    public JSONArray apiDirect(String api, String query){
        JSONArray answer = null;
        URL url = null;

        try{
            url = new URL(BctiApiDirectUrlPrefix + api);
        }catch(Exception e){
            logger.warn("apiDirect: ", e);
            return answer;
        }
        HttpsURLConnection connection = null;

        try{
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setDoOutput(true);
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            // encode query?
            logger.warn("apiDirect: query=" + BctiApiDirectRequestPrefix.replaceAll("%requestid%", Integer.toString(requestId)) + query + BctiApiDirectRequestSuffix);
            dos.writeBytes(BctiApiDirectRequestPrefix.replaceAll("%requestid%", Integer.toString(requestId)) + query + BctiApiDirectRequestSuffix);
            requestId++;
            dos.flush();
            dos.close();

            int responseCode = connection.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            logger.warn(response.toString());
            JSONObject jsonObject = new JSONObject(response.toString());
            logger.warn(jsonObject);
            answer = jsonObject.getJSONArray("results");
            // close buffered reader?
            // close connection
        }catch(Exception e){
            logger.warn("apiDirect: ", e);
            return answer;
        }

        return answer;
    }

    /**
     * Diret query for historical information about incoming IP addresses.
     *
     * @param  addresses String array of IP addresses.
     * @return           JSONArray of each query for each address.
     */
    public JSONArray getIpHistory(String... addresses)
    {
        JSONArray answer = null;
        String key = String.join("\",\"", addresses);
        synchronized(ipHistoryCacheSync){
            answer = ipHistoryCache.get(key);
        }
        if(answer == null){
            answer = new JSONArray();
            int index = 0;
            Calendar startDate = Calendar.getInstance();
            startDate.add(Calendar.DATE, -BCTI_DATE_MAX_START_DAYS);
            Calendar endDate = Calendar.getInstance();
            JSONArray directAnswer = null;
            try{
                directAnswer = apiDirect(BCTI_API_DIRECT_REQUEST_IP,
                    BCTI_API_DIRECT_REQUEST_IP_GETREPHISTORY
                        .replaceAll(BCTI_API_DIRECT_REQUEST_IPS_PARAMETER, key)
                );
                answer.put(index++, ((JSONObject) directAnswer.get(0)).getJSONObject(BCTI_API_DIRECT_RESPONSE_QUERIES_KEY));
                directAnswer = apiDirect(BCTI_API_DIRECT_REQUEST_IP,
                    BCTI_API_DIRECT_REQUEST_IP_GETTHREATHISTORY
                        .replaceAll(BCTI_API_DIRECT_REQUEST_IPS_PARAMETER, key)
                );
                answer.put(index++, ((JSONObject) directAnswer.get(0)).getJSONObject(BCTI_API_DIRECT_RESPONSE_QUERIES_KEY));
                directAnswer = apiDirect(BCTI_API_DIRECT_REQUEST_IPTHREATINSIGHT,
                    BCTI_API_DIRECT_REQUEST_IPTHREATINSIGHT_GETIPEVIDENCE
                        .replaceAll(BCTI_API_DIRECT_REQUEST_IPS_PARAMETER, key)
                        .replaceAll(BCTI_API_DIRECT_REQUEST_STARTDATE_PARAMETER, BCTI_DATE_FORMATTER.format(startDate.getTime()) + BCTI_DATE_TIME)
                        .replaceAll(BCTI_API_DIRECT_REQUEST_ENDDATE_PARAMETER, BCTI_DATE_FORMATTER.format(endDate.getTime()) + BCTI_DATE_TIME)
                );
                answer.put(index++, ((JSONObject) directAnswer.get(0)).getJSONObject(BCTI_API_DIRECT_RESPONSE_QUERIES_KEY));
            }catch(Exception e){
                logger.warn("getIpHistory: ", e);
            }

            if(answer != null){
                synchronized(ipHistoryCacheSync){
                    ipHistoryCache.put(key, answer);
                }
            }
        }
        return answer;
    }

    /**
     * Diret query for historical information about ourgoing IP addresses or urls.
     *
     * @param  addresses String array of IP addresses.
     * @return           JSONArray of each query for each address.
     */
    public JSONArray getUrlHistory(String... addresses)
    {
        JSONArray answer = null;
        String key = String.join("\",\"", addresses);
        synchronized(urlHistoryCacheSync){
            answer = urlHistoryCache.get(key);
        }
        if(answer == null){
            answer = new JSONArray();

            int index = 0;
            Calendar startDate = Calendar.getInstance();
            startDate.add(Calendar.DATE, -BCTI_DATE_MAX_START_DAYS);
            Calendar endDate = Calendar.getInstance();
            JSONArray directAnswer = null;
            try{
                directAnswer = apiDirect(BCTI_API_DIRECT_REQUEST_URL,
                    BCTI_API_DIRECT_REQUEST_URL_GETREPINFO
                        .replaceAll(BCTI_API_DIRECT_REQUEST_URLS_PARAMETER, key)
                );
                answer.put(index++, ((JSONObject) directAnswer.get(0)).getJSONObject(BCTI_API_DIRECT_RESPONSE_QUERIES_KEY));
                directAnswer = apiDirect(BCTI_API_DIRECT_REQUEST_URLTHREATINSIGHT, 
                    BCTI_API_DIRECT_REQUEST_URLTHREATINSIGHT_GETURLINFO
                        .replaceAll(BCTI_API_DIRECT_REQUEST_URLS_PARAMETER, key)
                        .replaceAll(BCTI_API_DIRECT_REQUEST_STARTDATE_PARAMETER, BCTI_DATE_FORMATTER.format(startDate.getTime()) + BCTI_DATE_TIME)
                        .replaceAll(BCTI_API_DIRECT_REQUEST_ENDDATE_PARAMETER, BCTI_DATE_FORMATTER.format(endDate.getTime()) + BCTI_DATE_TIME)
                );
                answer.put(index++, ((JSONObject) directAnswer.get(0)).getJSONObject(BCTI_API_DIRECT_RESPONSE_QUERIES_KEY));
            }catch(Exception e){
                logger.warn("getUrlHistory: ", e);
            }

            if(answer != null){
                synchronized(urlHistoryCacheSync){
                    urlHistoryCache.put(key, answer);
                }
            }
        }
        return answer;
    }

    /**
     * Get daemon status
     * @return JSONArray of status
     */
    public JSONArray status()
    {
        return apiDaemon(BCTI_API_DAEMON_REQUEST_STATUS);
    }

    // ipGetInfo(ips...)
    // status()

    /**
     *  Generic cache class
     */
    @SuppressWarnings("serial")
    static private class WebrootCache extends LinkedHashMap<String,JSONArray>
    {
        private static final int MAX_ENTRIES = 1000;

        /**
         * Extend so that aging is performed on the oldest accessed entry.
         * @param  eldest Map element of String/JSONArray
         * @return        Whether the size of the cache is greater than maximum number of entries.
         */
        protected boolean removeEldestEntry(Map.Entry<String,JSONArray> eldest) {
            return size() > MAX_ENTRIES;
        }
    }

}