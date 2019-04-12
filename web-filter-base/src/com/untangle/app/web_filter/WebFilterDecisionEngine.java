/** $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.web_filter;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.License;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Does blacklist lookups from zvelo API.
 */
public class WebFilterDecisionEngine extends DecisionEngine
{
    private static final String QUIC_COOKIE_FIELD_NAME = "alt-svc";
    private static final String QUIC_COOKIE_FIELD_START = "quic=";

    private static final String YOUTUBE_HEADER_FIELD_FIND_NAME = "youtube";
    private static final String YOUTUBE_RESTRICT_COOKIE_NAME = "PREF=";
    private static final String YOUTUBE_RESTIRCT_COOKIE_VALUE = "&f2=8000000";
    private static final int YOUTUBE_RESTRICT_COOKIE_TIMEOUT = 60 * 1000;
    private static final DateFormat COOKIE_DATE_FORMATTER = new SimpleDateFormat("E, MM-dd-yyyy HH:mm:ss z");
    private static final Pattern SEPARATORS_REGEX = Pattern.compile("\\.");

    private static Integer UNCATEGORIZED_CATEGORY = 0;

    static private InetSocketAddress SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", 8484);
    private static String URL_QUERY_PREFIX = "{\"url/getinfo\":{\"catids\": true,\"urls\":[\"";
    private static String URL_QUERY_SUFFIX = "\"]}}\r\n";
    private static byte PAYLOAD_SIZE_BEGIN = (byte) '<';
    private static byte PAYLOAD_SIZE_END = (byte) '>';

    private Socket lookupDaemonSocket = null;

    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase ourApp = null;
    private int failures = 0;

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
        if (!isLicenseValid()) {
            return null;
        } else {
            String result = super.checkRequest(sess, clientIp, port, requestLine, header);
            if (result == null) {
                String term = SearchEngine.getQueryTerm(clientIp, requestLine, header);
                if (term != null) {
                    WebFilterQueryEvent hbe = new WebFilterQueryEvent(requestLine.getRequestLine(), header.getValue("host"), term, getApp().getName());
                    getApp().logEvent(hbe);
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
        if (logger.isDebugEnabled()) {
            logger.debug("start-checkDecisionEngineDatabase");
        }

        /**
         * While Brightcloud can handle domains with ports its very expensive, around 100 times slower.
         */
        int i = domain.indexOf(':');
        if (i > 0) {
            domain = domain.substring(0, i);
        }

        List<Integer> categories = lookupUrl(domain + uri);

        if (categories == null) {
            categories = new ArrayList<Integer>(1);
            categories.add(UNCATEGORIZED_CATEGORY);
        }

        return categories;
    }

    /**
     * Clear the host cache
     * 
     * @param expireAll
     *        Flag to clear all or expired only
     */
    // public void clearCache(boolean expireAll)
    // {
    //     // HostCache.cleanCache(expireAll);
    // }

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
     * Perform lookup of URL.
     *
     * @param  url URL to lookup.
     * @return     List of integers of category ids.
     */
    List<Integer> lookupUrl(String url)
    {
        return lookupUrl(url, false);
    }


    /**
     * Perform lookup of URL.
     *
     * @param  url URL to lookup.
     * @param reopenSocket If true, re-open socket.  Otherwise, use attempt to use cached socket.
     * @return     List of integers of category ids.
     */
    List<Integer> lookupUrl(String url, Boolean reopenSocket)
    {
        List<Integer> answers = null;
        try{
            synchronized(this){
                if(lookupDaemonSocket == null || reopenSocket){
                    lookupDaemonSocket = new Socket();
                    lookupDaemonSocket.connect(SOCKET_ADDRESS);
                    lookupDaemonSocket.setKeepAlive(true);
                }
                lookupDaemonSocket.getOutputStream().write((URL_QUERY_PREFIX + url + URL_QUERY_SUFFIX).getBytes());
                lookupDaemonSocket.getOutputStream().flush();

                InputStream is = lookupDaemonSocket.getInputStream();

                boolean inSize = false;
                int payloadSize = 0;
                int c;
                do{
                    c = is.read();
                    if(c == PAYLOAD_SIZE_BEGIN){
                        inSize = true;
                        continue;
                    }
                    if(inSize){
                        if(c == PAYLOAD_SIZE_END){
                            break;
                        }
                        payloadSize = (char) (c - 48)+ (payloadSize * 10);
                    }
                }while(is.available()>0);
                byte payloadBuffer[] = new byte[payloadSize];
                is.read(payloadBuffer,0,payloadSize);

                JSONObject jo = new JSONObject(new String(payloadBuffer, 0, payloadSize));
                JSONArray catids = jo.getJSONObject(url).getJSONArray("catids");
                answers = new ArrayList<Integer>(catids.length());
                for(int i = 0; i < catids.length(); i++){
                    answers.add(catids.getInt(i));
                }
            }
        }catch(Exception e){
            logger.warn(e);
            // logger.warn("what", e);
            try{
                lookupDaemonSocket.close();
                lookupDaemonSocket = null;
            }catch(Exception e2){}

            if(reopenSocket == false){
                return lookupUrl(url, true);
            }
        }

        return answers;
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

}
