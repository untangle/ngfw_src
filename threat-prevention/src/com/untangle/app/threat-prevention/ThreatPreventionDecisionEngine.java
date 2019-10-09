/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.AppSession;


import com.untangle.uvm.vnet.SessionAttachments;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.app.webroot.WebrootQuery;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.GenericRule;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;

import java.util.Iterator;

/**
 * This is the core functionality of web filter It decides if a site should be
 * blocked, passed, logged, etc based on the settings and categorization.
 */
public class ThreatPreventionDecisionEngine
{
    private Map<String, String> i18nMap;
    Long i18nMapLastUpdated = 0L;

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This regex matches any URL that is IP based - http://1.2.3.4/
     */
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    /**
     * Match  slashes (except for leading protocol) with single slashes
     */
    public static Pattern CONSECUTIVE_SLASHES_URI_PATTERN = Pattern.compile("(?<!:)/+");
    public static Pattern CONSECUTIVE_SLASHES_PATH_PATTERN = Pattern.compile("/+");


    /**
     * This is the base app that owns this decision engine
     */
    private final ThreatPreventionApp app;

    /**
     * Users are able to "unblock" sites if the admin allows it. Unblocked sites
     * are temporary and only stored in memory This map stores a list of
     * unblocked sites by IP address
     */
    private final Map<InetAddress, HashSet<String>> unblockedDomains = new HashMap<InetAddress, HashSet<String>>();

    /**
     * Constructor
     * 
     * @param app
     *        The owner application
     */
    public ThreatPreventionDecisionEngine(ThreatPreventionApp app)
    {
        this.app = app;
    }


    /**
     * [addressQuery description]
     * @param  clientAddress [description]
     * @param  serverAddress [description]
     * @param  sessionAttachments   [description]
     * @return               [description]
     */
    public boolean addressQuery(InetAddress clientAddress, InetAddress serverAddress, SessionAttachments sessionAttachments)
    {
        return addressQuery(clientAddress, serverAddress, null, sessionAttachments);
    }

    /**
     * [addressQuery description]
     * @param  clientAddress      [description]
     * @param  url                [description]
     * @param  sessionAttachments [description]
     * @return                    [description]
     */
    public boolean addressQuery(InetAddress clientAddress, String url, SessionAttachments sessionAttachments)
    {
        return addressQuery(clientAddress, null, url, sessionAttachments);
    }

    /**
     * [addressQuery description]
     * @param  clientAddress      [description]
     * @param  serverAddress      [description]
     * @param  serverUrl          [description]
     * @param  sessionAttachments [description]
     * @return                    [description]
     */
    public boolean addressQuery(InetAddress clientAddress, InetAddress serverAddress, String serverUrl, SessionAttachments sessionAttachments)
    {
        boolean clientLocal = false;
        boolean serverLocal = false;
        for(IPMaskedAddress address : app.localNetworks){
            if(clientLocal == false){
                clientLocal = clientAddress != null ? address.contains(clientAddress) : false;
            }
            if(serverLocal == false){
                serverLocal = serverAddress != null ? address.contains(serverAddress) : false;
            }
        }

        long lookupTimeBegin = System.currentTimeMillis();
        JSONArray answer = null;
        if(!clientLocal && !serverLocal){
            // Also need to handle serverAddress = null
            answer = app.webrootQuery.ipGetInfo(clientAddress.getHostAddress(), serverAddress != null ? serverAddress.getHostAddress() : null);
        }else if(!clientLocal){
            answer = app.webrootQuery.ipGetInfo(clientAddress.getHostAddress());
        }else if(!serverLocal){
            answer = app.webrootQuery.urlGetInfo(serverAddress != null ? serverAddress.getHostAddress() : serverUrl);
        }
        app.adjustLookupAverage(System.currentTimeMillis() - lookupTimeBegin);

        JSONObject ipInfo = null;
        if(answer != null){
            String ip = null;
            try{
                for(int i = 0; i < answer.length(); i++){
                    ipInfo = answer.getJSONObject(i);
                    if(ipInfo.has(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_IP_KEY)){
                        ip = ipInfo.getString(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_IP_KEY);
                        if(!clientLocal && clientAddress != null && ip.equals(clientAddress.getHostAddress())){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_THREATMASK, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_THREATMASK_KEY));
                        }else if(!serverLocal && serverAddress != null && ip.equals(serverAddress.getHostAddress())){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_THREATMASK, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_THREATMASK_KEY));
                        }
                    }else if(ipInfo.has(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_URL_KEY)){
                        ip = ipInfo.getString(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_URL_KEY);
                        int threatmask = 0;

                        JSONArray catids = ipInfo.getJSONArray(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_LIST_KEY);
                        for(int j = 0; j < catids.length(); j++){
                            Integer cat = catids.getJSONObject(i).getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_CATEGORY_ID_KEY);
                            if(ThreatPreventionApp.UrlCatThreatMap.get(cat) != null){
                                threatmask += ThreatPreventionApp.UrlCatThreatMap.get(cat);
                            }
                            // !! Maybe also put into new web-filter/monitor categorized attachment to save lookup.
                        }

                        if(!clientLocal && clientAddress != null && ip.equals(clientAddress.getHostAddress())){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_THREATMASK, threatmask);
                        }else if(!serverLocal && 
                                ( ( serverAddress != null && ip.equals(serverAddress.getHostAddress()) ) ||
                                  ( serverUrl != null && ip.equals(serverUrl) )
                                ) 
                                ){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_THREATMASK, threatmask);
                        }
                    }
                }
            }catch(Exception e){
                logger.warn("Cannot pull IP information ", e);
                return false;
            }
        }

        return true;
    }

    /**
     * [getMatch description]
     * @param  sessionAttachments [description]
     * @return                    [description]
     */
    public Boolean isMatch(SessionAttachments sessionAttachments)
    {
        Integer clientReputation = (Integer) sessionAttachments.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION);
        Integer serverReputation = (Integer) sessionAttachments.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION);
        Integer threatLevel = app.getSettings().getThreatLevel();

        // time of getSettings().getValue() vs synchronized values.

        return ( ( ( serverReputation != null ) && serverReputation > 0 && serverReputation <= app.getSettings().getThreatLevel() ) )
                ||
                ( ( ( clientReputation != null ) && clientReputation > 0 && clientReputation <= app.getSettings().getThreatLevel() ) );
    }

    /**
     * Checks if the request should be blocked, giving an appropriate response
     * if it should.
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        IP That made the request.
     * @param port
     *        Port that the request was made to.
     * @param requestLine
     *        The request line token
     * @param header
     *        The header token
     * @return an HTML response (null means the site is passed and no response
     *         is given).
     */
    public String checkRequest(AppTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        /*
         * this stores whether this visit should be flagged for any reason
         */
        Boolean isFlagged = false;
        /*
         * this stores the corresponding reason for the flag/block
         */
        URI requestUri = null;

        try {
            requestUri = new URI(CONSECUTIVE_SLASHES_URI_PATTERN.matcher(requestLine.getRequestUri().normalize().toString()).replaceAll("/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + requestUri + "'", e);
        }

        String host = requestUri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }

        host = UrlMatchingUtil.normalizeHostname(host);

        String uri = "";
        if (requestUri.isAbsolute()) {
            host = requestUri.getHost();
            uri = requestUri.normalize().getRawPath();
        } else {
            uri = requestUri.normalize().toString();
        }

        uri = CONSECUTIVE_SLASHES_PATH_PATTERN.matcher(uri).replaceAll("/");

        Boolean match = false;
        if(addressQuery(clientIp, host + uri, sess)){
            match = isMatch(sess);
        }

        boolean block = app.getSettings().getAction().equals("block");
        boolean flag = app.getSettings().getFlag();
        Integer ruleIndex = null;

        ThreatPreventionPassRule matchRule = null;
        for (ThreatPreventionPassRule rule : app.getSettings().getPassRules()){
            if( rule.isMatch(sess) ){
                matchRule = rule;
                block = !rule.getPass();
                flag = rule.getFlag();
                ruleIndex = rule.getRuleId();
                break;
            }
        }

        Integer clientReputation = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION);
        Integer clientThreatmask = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_THREATMASK);
        Integer serverReputation = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION);
        Integer serverThreatmask = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_THREATMASK);

        // should be logging url 0n http-events
        ThreatPreventionEvent fwe = new ThreatPreventionEvent(sess.sessionEvent(), block && match, match && flag, 
            ruleIndex != null ? ruleIndex : 0, 
            clientReputation != null ? clientReputation : 0, 
            clientThreatmask != null ? clientThreatmask : 0, 
            serverReputation != null ? serverReputation : 0, 
            serverThreatmask != null ? serverThreatmask : 0
            );
        app.logEvent(fwe);

        if (match && block) {
            app.incrementBlockCount(); 
            if (flag){
                app.incrementFlagCount();
            }

            updateI18nMap();
            ThreatPreventionBlockDetails bd = new ThreatPreventionBlockDetails(app.getSettings(), host, uri.toString(), 
                matchRule != null ? matchRule.getDescription() : I18nUtil.tr("Threat reputation {0}", app.getThreatFromReputation(serverReputation), i18nMap), 
                clientIp);
            return app.generateNonce(bd);
        } else {
            if (flag){
                app.incrementFlagCount();
            }
        }
        return null;
    }

    /**
     * If expiraton matches language manager, refresh.
     */
    private void updateI18nMap()
    {
        if ((i18nMapLastUpdated + com.untangle.uvm.LanguageManager.CLEANER_LAST_ACCESS_MAX_TIME - 1000) < System.currentTimeMillis()) {
            i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
            i18nMapLastUpdated = System.currentTimeMillis();
        }
    }

}
