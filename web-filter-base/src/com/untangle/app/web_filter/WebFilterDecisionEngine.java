/** $Id: WebFilterDecisionEngine.java 43139 2016-04-28 18:10:05Z dmorris $
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import org.apache.log4j.Logger;

import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.app.webroot.WebrootQuery;
import com.untangle.app.webroot.WebrootDaemon;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.GlobMatcher;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * Does blacklist lookups from zvelo API.
 */
public class WebFilterDecisionEngine extends DecisionEngine
{
    private WebrootQuery webrootQuery = null;

    private static final String QUIC_COOKIE_FIELD_NAME = "alt-svc";
    private static final String QUIC_COOKIE_FIELD_START = "quic=";

    private static final String YOUTUBE_HEADER_FIELD_FIND_NAME = "youtube";
    private static final String YOUTUBE_RESTRICT_COOKIE_NAME = "PREF=";
    private static final String YOUTUBE_RESTIRCT_COOKIE_VALUE = "&f2=8000000";
    private static final int YOUTUBE_RESTRICT_COOKIE_TIMEOUT = 60 * 1000;
    private static final DateFormat COOKIE_DATE_FORMATTER = new SimpleDateFormat("E, MM-dd-yyyy HH:mm:ss z");
    private static final Pattern SEPARATORS_REGEX = Pattern.compile("\\.");

    private static Integer UNCATEGORIZED_CATEGORY = 0;

    private final Logger logger = Logger.getLogger(getClass());
    private WebFilterBase ourApp = null;

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
     * @param sess
     *        The session
     * @param domain
     *        The domain
     * @param uri
     *        The URI
     * @return The category list
     */
    @Override
    protected List<Integer> categorizeSite(AppTCPSession sess, String domain, String uri)
    {
        List<Integer> categories = null;
        try{
            JSONArray answer = webrootQuery.urlGetInfo(domain, uri);
            if(answer != null){
                JSONObject urlAnswer = answer.getJSONObject(0);

                JSONArray catids = urlAnswer.getJSONArray(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_LIST_KEY);
                categories = new ArrayList<Integer>(catids.length());
                for(int i = 0; i < catids.length(); i++){
                    categories.add(catids.getJSONObject(i).getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_ID_KEY));
                }

                if( (sess != null) &&
                    UvmContextFactory.context().hookManager().hooksExist(HookManager.WEBFILTER_BASE_CATEGORIZE_SITE)){
                    UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.WEBFILTER_BASE_CATEGORIZE_SITE, sess, urlAnswer.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_REPUTATION_KEY), categories );
                }
            }
        }catch(Exception e){
            logger.warn(e);
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
        // bctidQuery(BCTI_QUERY_URLCLEARCACHE);
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
        return categorizeSite(null, urlSplit[0], urlSplit[2]);
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
        WebrootDaemon.getInstance().start();
        webrootQuery = WebrootQuery.getInstance();
    }

    /**
     * Close the bcti sockets.
     */
    public void stop()
    {
        webrootQuery = null;
        WebrootDaemon.getInstance().stop();
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