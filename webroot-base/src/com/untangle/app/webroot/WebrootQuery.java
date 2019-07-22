/** $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.webroot;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.log4j.Logger;

/**
 * Queries for webroot daemon
 */
public class WebrootQuery
{
    private static final Logger logger = Logger.getLogger(WebrootQuery.class);
    /**
     * The singleton instance
     */
    private static final WebrootQuery INSTANCE = new WebrootQuery();

    private static WebrootDaemon webrootDaemon = WebrootDaemon.getInstance();

    public static char DOMAIN_PORT = ':';
    public static char DOMAIN_DOT = '.';
    public static String DOMAIN_WILDCARD = "*.";

    public static String BCTI_API_ERROR_KEY="ERROR";

    public static String BCTI_API_QUERY_URLINFO_PREFIX = "{\"url/getinfo\":{\"urls\":[\"";
    public static String BCTI_API_QUERY_URLINFO_SUFFIX = "\"]}}\r\n";

    public static String BCTI_API_RESPONSE_URLINFO_CATEGORY_LIST_KEY="cats";
    public static String BCTI_API_RESPONSE_URLINFO_CATEGORY_ID_KEY="catid";

    public static String BCTI_API_QUERY_URLCLEARCACHE = "{\"url/setcacheclear\":{}}\r\n";
    public static String BCTI_API_QUERY_STATUS = "{\"status\":{}}\r\n";

    private static Integer UNCATEGORIZED_CATEGORY = 0;

    static private InetSocketAddress BCTID_SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", 8484);

    private static final long BCTID_CONNECT_WAIT = 1 * 1000;
    private long BctidSocketConnectWait = 0L;

    private static Integer BctidMaxSocketPoolSize = 30;

    private static ArrayBlockingQueue<Socket> BctidSocketPool = new ArrayBlockingQueue<>(BctidMaxSocketPoolSize);
    private static AtomicInteger BctidSocketRunnersCount = new AtomicInteger();
    private static long BctidSocketPoolMaxWaitSeconds = 5L;

    private int failures = 0;

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
     * Enable queries.
     */
    public static void start(){
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
        api(BCTI_API_QUERY_URLCLEARCACHE);
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
     * Query daemon.
     * @param  query        String of bcti query to send
     * @return              String of json response.
     */
    public JSONArray api(String query)
    {
        return api(query, false);
    }

    /**
     * Query daemon.
     * @param  query        String of bcti query to send
     * @param  retry        Boolean to retry again.
     * @return              String of json response.
     */
    public JSONArray api(String query, Boolean retry)
    {
        JSONArray answer = null;
        String rawAnswer = null;

        if(webrootDaemon.isReady() == false){
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
                    logger.warn("api: timed out getting socket from pool!" + BctidSocketRunnersCount.get() + ":" + BctidSocketPool.size());
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
            return api(query, true);
        }

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
                    logger.warn("api: answer contains error: " + rawAnswer);
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
            return api(BCTI_API_QUERY_URLINFO_PREFIX + domain + uri + BCTI_API_QUERY_URLINFO_SUFFIX);
        }
        return null;
    }

    /**
     * Get daemon status
     * @return JSONArray of status
     */
    public JSONArray status()
    {
        return api(BCTI_API_QUERY_STATUS);
    }

    // ipGetInfo(ips...)
    // status()


}