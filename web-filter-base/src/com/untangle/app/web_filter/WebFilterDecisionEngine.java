/** $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.web_filter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.GlobMatcher;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.vnet.AppTCPSession;

import com.untangle.uvm.util.Pulse;

/**
 * Does blacklist lookups from zvelo API.
 */
public class WebFilterDecisionEngine extends DecisionEngine
{
    public static char DOMAIN_PORT = ':';
    public static char DOMAIN_DOT = '.';
    public static String DOMAIN_WILDCARD = "*.";
    public static String BCTI_QUERY_URLINFO_PREFIX = "{\"url/getinfo\":{\"urls\":[\"";
    public static String BCTI_QUERY_URLINFO_SUFFIX = "\"]}}\r\n";
    public static String BCTI_QUERY_URLINFO_CATEGORY_LIST_KEY="cats";
    public static String BCTI_QUERY_URLINFO_ERROR_KEY="ERROR";
    public static String BCTI_QUERY_URLINFO_CATEGORY_ID_KEY="catid";
    public static String BCTI_QUERY_URLCLEARCACHE = "{\"url/setcacheclear\":{}}\r\n";
    public static String BCTI_QUERY_STATUS = "{\"status\":{}}\r\n";

    private static final String QUIC_COOKIE_FIELD_NAME = "alt-svc";
    private static final String QUIC_COOKIE_FIELD_START = "quic=";

    private static final String YOUTUBE_HEADER_FIELD_FIND_NAME = "youtube";
    private static final String YOUTUBE_RESTRICT_COOKIE_NAME = "PREF=";
    private static final String YOUTUBE_RESTIRCT_COOKIE_VALUE = "&f2=8000000";
    private static final int YOUTUBE_RESTRICT_COOKIE_TIMEOUT = 60 * 1000;
    private static final DateFormat COOKIE_DATE_FORMATTER = new SimpleDateFormat("E, MM-dd-yyyy HH:mm:ss z");
    private static final Pattern SEPARATORS_REGEX = Pattern.compile("\\.");

    private static Integer UNCATEGORIZED_CATEGORY = 0;

    public static String BCTID_CONFIG_FILE = "/etc/bctid/bcti.cfg";
    public static String BCTID_CONFIG_DEVICE_KEY = "Device=";
    public static String BCTID_CONFIG_DEVICE_VALUE = "NGFirewall";
    public static String BCTID_CONFIG_DEVICE_DEVELOPER_VALUE = "_Internal";
    public static String BCTID_CONFIG_UID_KEY = "UID=";

    static private InetSocketAddress BCTID_SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", 8484);

    private static AtomicInteger DecisionEngineCount = new AtomicInteger();

    private static Boolean BctidReady = false;
    private static final long BCTID_CONNECT_WAIT = 1 * 1000;
    private long BctidSocketConnectWait = 0L;

    private static Integer BctidMaxSocketPoolSize = 30;

    private static ArrayBlockingQueue<Socket> BctidSocketPool = new ArrayBlockingQueue<>(BctidMaxSocketPoolSize);
    private static AtomicInteger BctidSocketRunnersCount = new AtomicInteger();
    private static long BctidSocketPoolMaxWaitSeconds = 5L;

    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase ourApp = null;
    private int failures = 0;

    /**
     * Pulse thread to read btci daemon statistics.
     */
    private static long DEFAULT_GET_STATISTICS_INTERVAL_MS = (long) 30 * 1000; /* every 30 seconds */
    private static long DEFAULT_GET_STATISTICS_RUN_TIMEOUT_MS = (long) 60 * 60 * 1000; /* Kill process after 60 minutes.  */

    private Pulse pulseGetStatistics = new Pulse("decision-get-statistics", new GetStatistics(), DEFAULT_GET_STATISTICS_INTERVAL_MS, true, DEFAULT_GET_STATISTICS_RUN_TIMEOUT_MS);

    /**
     * Constructor
     * 
     * @param app
     *        The base application
     */
    public WebFilterDecisionEngine(WebFilterBase app)
    {
        super(app);
        ourApp = app;
    }

    /**
     * Check the request
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        The client address
     * @param port
     *        The port
     * @param requestLine
     *        The request line
     * @param header
     *        The header
     * @return The result
     */
    @Override
    public String checkRequest(AppTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        Boolean isFlagged = false;
        if (!isLicenseValid()) {
            return null;
        } else {
            String result = super.checkRequest(sess, clientIp, port, requestLine, header);
            if (result == null) {
                String term = SearchEngine.getQueryTerm(clientIp, requestLine, header);

                if (term != null) {
                    GenericRule matchingRule = null;
                    for (GenericRule rule : ourApp.getSettings().getSearchTerms()) {
                        if (rule.getEnabled() != null && !rule.getEnabled()) continue;

                        Object matcherO = rule.attachment();
                        GlobMatcher matcher = null;

                        /**
                         * If the matcher is not attached to the rule, initialize a new one
                         * and attach it. Otherwise just use the matcher already initialized
                         * and attached to the rule
                         */
                        if (matcherO == null || !(matcherO instanceof GlobMatcher)) {
                            matcher = GlobMatcher.getMatcher("*\\b" + rule.getString() + "\\b*");
                            rule.attach(matcher);
                        } else {
                            matcher = (GlobMatcher) matcherO;
                        }

                        if (matcher.isMatch(term)) {
                            logger.warn("LOG: " + term + " matches " + rule.getString());
                            matchingRule = rule;
                            break;
                        }
                    }

                    WebFilterQueryEvent hbe = new WebFilterQueryEvent(requestLine.getRequestLine(), header.getValue("host"), term, matchingRule != null ? matchingRule.getBlocked() : Boolean.FALSE, matchingRule != null ? matchingRule.getFlagged() : Boolean.FALSE, getApp().getName());
                    getApp().logEvent(hbe);

                    if(matchingRule != null){
                        URI uri = null;

                        try {
                            uri = new URI(CONSECUTIVE_SLASHES_URI_PATTERN.matcher(requestLine.getRequestUri().normalize().toString()).replaceAll("/"));
                        } catch (URISyntaxException e) {
                            logger.error("Could not parse URI '" + uri + "'", e);
                        }

                        String host = uri.getHost();
                        if (null == host) {
                            host = header.getValue("host");
                            if (null == host) {
                                host = clientIp.getHostAddress();
                            }
                        }

                        host = UrlMatchingUtil.normalizeHostname(host);

                        if (matchingRule.getBlocked()) {
                            ourApp.incrementFlagCount();

                            WebFilterBlockDetails bd = new WebFilterBlockDetails(ourApp.getSettings(), host, uri.toString(), matchingRule.getDescription(), clientIp, ourApp.getAppTitle());
                            return ourApp.generateNonce(bd);
                        } else {
                            if (matchingRule.getFlagged()) isFlagged = true;

                            if (isFlagged) ourApp.incrementFlagCount();
                            return null;
                        }
                    }
                }

                if(ourApp.getSettings().getRestrictYoutube()){
                    addYoutubeRestrictCookie(sess, header);
                }
           }
            return result;
        }
    }

    /**
     * Check the response
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        The client address
     * @param requestLine
     *        The request line
     * @param header
     *        The header
     * @return The result
     */
    @Override
    public String checkResponse(AppTCPSession sess, InetAddress clientIp, RequestLineToken requestLine, HeaderToken header)
    {
        if (!isLicenseValid()) {
            return null;
        } else {
            String result = super.checkResponse(sess, clientIp, requestLine, header);
            if(result == null){
                if(ourApp.getSettings().getBlockQuic()){
                    removeQuicCookie(header);
                }
            }
            return result;
       }
    }


    /**
     * Categorize a site
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The URI
     * @return The category list
     */
    @Override
    protected List<Integer> categorizeSite(String domain, String uri)
    {
        List<Integer> categories = null;
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
            String url = domain + uri;

            String bctidAnswer = null;
            try{
                bctidAnswer = bctidQuery(BCTI_QUERY_URLINFO_PREFIX + url + BCTI_QUERY_URLINFO_SUFFIX);
                if(bctidAnswer != null){
                    JSONObject urlAnswer = new JSONArray(bctidAnswer).getJSONObject(0);

                    if(urlAnswer.has(BCTI_QUERY_URLINFO_ERROR_KEY)){
                        logger.warn("categorizeSite: answer contains error: " + bctidAnswer + ", defaulting to UNCATEGORIZED_CATEGORY");
                    }else{
                        JSONArray catids = urlAnswer.getJSONArray(BCTI_QUERY_URLINFO_CATEGORY_LIST_KEY);
                        categories = new ArrayList<Integer>(catids.length());
                        for(i = 0; i < catids.length(); i++){
                            categories.add(catids.getJSONObject(i).getInt(BCTI_QUERY_URLINFO_CATEGORY_ID_KEY));
                        }
                    }

                }
            }catch(Exception e){
                if(bctidAnswer != null){
                    logger.warn("bctidAnswer: "+ bctidAnswer);
                }
                logger.warn(e);
            }
        }

        if (categories == null || categories.size() == 0){
            categories = new ArrayList<Integer>(1);
            categories.add(UNCATEGORIZED_CATEGORY);
        }

        return categories;
    }

    /**
     * Clear the host cache
     */
    public void clearCache()
    {
        bctidQuery(BCTI_QUERY_URLCLEARCACHE);
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
        String[] urlSplit = splitUrl(url);
        return categorizeSite(urlSplit[0], urlSplit[2]);
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
     * Start the statistics gatherer.
     */
    public void start()
    {
        reconfigure(null);
        boolean firstIn = DecisionEngineCount.get() == 0;
        DecisionEngineCount.incrementAndGet();

        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-bctid");

        if(firstIn){
            BctidReady = true;
        }
        pulseGetStatistics.start();
    }

    /**
     * Close the bcti sockets.
     */
    public void stop()
    {
        boolean lastOut = DecisionEngineCount.decrementAndGet() == 0;
        try{
            if(pulseGetStatistics.getState() == Pulse.PulseState.RUNNING){
                pulseGetStatistics.stop();
            }
            if( lastOut ){
                BctidReady = false;
                Socket socket = null;
                Thread.sleep(2 * 1000);
                while((socket = BctidSocketPool.poll(BctidSocketPoolMaxWaitSeconds, TimeUnit.SECONDS)) != null){
                    socket.close();
                }
                BctidSocketPool.clear();
                BctidSocketRunnersCount.set(0);
            }
        }catch(Exception e){
            logger.warn("Unable to properly stop", e);
        }
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-bctid");
    }

    /**
     * Reconfigure the decision engine.
     * @param settings WebFilter settings.
     */
    public void reconfigure(WebFilterSettings settings){

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
     * Query daemon.
     * @param  query        String of bcti query to send
     * @return              String of json response.
     */
    String bctidQuery(String query)
    {
        return bctidQuery(query, false);
    }

    /**
     * Query daemon.
     * @param  query        String of bcti query to send
     * @param  retry        Boolean to retry again.
     * @return              String of json response.
     */
    String bctidQuery(String query, Boolean retry)
    {
        String answer = null;

        if(BctidReady == false){
            return answer;
        }
        boolean failed = false;
        StringBuilder responseBuilder = new StringBuilder(1024);
        Socket bctidSocket = null;
        try{
            synchronized(BctidSocketRunnersCount){
                if((BctidSocketRunnersCount.get() + BctidSocketPool.size()) < BctidMaxSocketPoolSize){
                    BctidSocketRunnersCount.incrementAndGet();
                    bctidSocket = new Socket();
                    bctidSocket.connect(BCTID_SOCKET_ADDRESS, 1000);
                    bctidSocket.setKeepAlive(true);
                    bctidSocket.setSoTimeout(5000);
                }
            }
            if(bctidSocket == null){
                bctidSocket = BctidSocketPool.poll(BctidSocketPoolMaxWaitSeconds, TimeUnit.SECONDS);
                if(bctidSocket == null){
                    logger.warn("bctidQuery: timed out getting socket from pool!" + BctidSocketRunnersCount.get() + ":" + BctidSocketPool.size());
                    return answer;
                }
                BctidSocketRunnersCount.incrementAndGet();
            }

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
                answer = new String(payloadBuffer, 0, payloadRead);
                if(nFound && responseBuilder.length() == 0){
                    break;
                }
                responseBuilder.append(answer);
                answer = responseBuilder.toString();
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
            if(retry == false){
                logger.warn("problem with query ("+query+"), attemping retry");
                failed = true;
            }else{
                logger.warn("problem with query ("+query+"), was in retry", e);
            }
        }

        if(bctidSocket != null){
            BctidSocketRunnersCount.decrementAndGet();
            BctidSocketPool.offer(bctidSocket);
        }

        if(failed && retry == false){
            return bctidQuery(query, true);
        }

        return answer;
    }

    /**
     * Split a URL into parts
     * 
     * @param url
     *        The URL
     * @return The parts
     */
    private String[] splitUrl(String url)
    {
        String[] urlFields = new String[3];
        String domain = url;
        String uri = "/";
        String port = "80";
        int pos;

        /*
         * Strip protocol
         */
        pos = domain.indexOf("://");
        if (pos > 0) {
            domain = domain.substring(pos + 3);
        }

        /*
         * Split domain and URI
         */
        pos = domain.indexOf('/');
        if (pos > 0) {
            uri = domain.substring(pos);
            domain = domain.substring(0, pos);
        }

        /*
         * Split domain and port
         */
        pos = domain.indexOf(':');
        if (pos > 0) {
            port = domain.substring(pos + 1);
            domain = domain.substring(0, pos);
        }
        urlFields[0] = domain;
        urlFields[1] = port;
        urlFields[2] = uri;

        return urlFields;
    }

    /**
     * Check the license status
     * 
     * @return True if valid, otherwise false
     */
    private boolean isLicenseValid()
    {
        if (ourApp.getAppName().equals("web-monitor")) return true;

        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_FILTER)) return true;
        return false;
    }

    /**
     * Look for presence of quic cookie and if found remove.
     *
     * @param  header HeadetToken to parse.
     */
    private void removeQuicCookie(HeaderToken header){
        List<String> altSvcs = header.getValues(QUIC_COOKIE_FIELD_NAME);
        if(altSvcs != null){
            List<String> newAltSvcs = new ArrayList<>();
            for(String altSvc : altSvcs){
                if(!altSvc.toLowerCase().startsWith(QUIC_COOKIE_FIELD_START)){
                    newAltSvcs.add(altSvc);
                }
            }
            header.removeField(QUIC_COOKIE_FIELD_NAME);
            if(newAltSvcs.size() > 0){
                header.setValues(QUIC_COOKIE_FIELD_NAME, newAltSvcs);
            }
        }
    }


    /**
     * If host is youtube, replace the inline PREF (if found) or append the PREF cookie that
     * enables restricted mode.
     * @param sess
     *        AppTCPSession for this session.
     * @param header HeaterToken to parse.
     */
    private void addYoutubeRestrictCookie(AppTCPSession sess, HeaderToken header){
        String host = header.getValue("host");

        if(host != null && SEPARATORS_REGEX.matcher(host.toLowerCase()).replaceAll("").contains(YOUTUBE_HEADER_FIELD_FIND_NAME) ) {

            List<String> cookies = header.getValues("cookie");
            long cookieExpiration = System.currentTimeMillis() + YOUTUBE_RESTRICT_COOKIE_TIMEOUT;

            if (cookies == null) {
                cookies = new ArrayList<>();
            }
            boolean found = false;
            for(int i = 0; i < cookies.size(); i++){
                String setCookie = cookies.get(i);
                if(setCookie.toUpperCase().startsWith(YOUTUBE_RESTRICT_COOKIE_NAME)){
                    found = true;
                    int firstSemiColon=setCookie.indexOf(';');
                    cookies.set(i, setCookie.substring(0, firstSemiColon) + YOUTUBE_RESTIRCT_COOKIE_VALUE + setCookie.substring(firstSemiColon));
                }
            }
            if(!found){
                cookies.add(YOUTUBE_RESTRICT_COOKIE_NAME + YOUTUBE_RESTIRCT_COOKIE_VALUE + "; expires="+COOKIE_DATE_FORMATTER.format(cookieExpiration)+"; path=/; domain=.youtube.com");
            }
        }
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
                String statusResult = bctidQuery(BCTI_QUERY_STATUS);
                if(statusResult != null){
                    JSONObject status = new JSONObject(statusResult);
                    ourApp.setCacheCount(new Long(status.getJSONObject("url_db").getInt("url_cache_current_size")));
                    ourApp.setNetworkErrorCount(new Long(status.getJSONObject("stats").getJSONObject("errors").getInt("network")));
                    ourApp.setIpErrorCount(new Long(status.getJSONObject("stats").getJSONObject("errors").getInt("ip")));
                }
            }catch(Exception e){
                logger.warn("Unable to query cache size",e);
            }
        }
    }

}