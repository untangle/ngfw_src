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
import com.untangle.app.http.HttpRedirect;

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
     * Perform threat lookup on specified addresses and attach results to sesesion.
     * @param  clientAddress      Client IP address.
     * @param  serverAddress      Server IP address.
     * @param  sessionAttachments Sessions's attachments to attach results.
     * @return                    boolean true if results were obtained, false if not.
     */
    public boolean addressQuery(InetAddress clientAddress, InetAddress serverAddress, SessionAttachments sessionAttachments)
    {
        return addressQuery(clientAddress, serverAddress, null, sessionAttachments);
    }

    /**
     * Perform threat lookup on specified addresses and attach results to sesesion.
     * @param  clientAddress      Client IP address.
     * @param  url                Server URL address.
     * @param  sessionAttachments Sessions's attachments to attach results.
     * @return                    boolean true if results were obtained, false if not.
     */
    public boolean addressQuery(InetAddress clientAddress, String url, SessionAttachments sessionAttachments)
    {
        return addressQuery(clientAddress, null, url, sessionAttachments);
    }

    /**
     * Perform threat lookup on specified addresses and attach results to sesesion.
     * @param  clientAddress      Client IP address.
     * @param  serverAddress      Server IP address.
     * @param  serverUrl          Server URL address.
     * @param  sessionAttachments Sessions's attachments to attach results.
     * @return                    boolean true if results were obtained, false if not.
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

        sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION, 0);
        sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_CATEGORIES, 0);
        sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION, 0);
        sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_CATEGORIES, 0);

        long lookupTimeBegin = System.currentTimeMillis();
        JSONArray answer = null;
        if(!serverLocal){
            if(serverUrl != null){
                answer = app.webrootQuery.urlGetInfo(serverUrl);
            }else{
                answer = app.webrootQuery.urlGetInfo(serverAddress != null ? serverAddress.getHostAddress() : null);
            }
        }
        if(!clientLocal){
            JSONArray clientAnswer = app.webrootQuery.ipGetInfo(clientAddress.getHostAddress(), serverAddress != null ? serverAddress.getHostAddress() : null);
            if(clientAnswer != null){
                if(answer == null){
                    answer = clientAnswer;
                }else{
                    try{
                        answer.put(clientAnswer.getJSONObject(0));
                    }catch(Exception e){}
                }
            }
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
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_CATEGORIES, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_THREATMASK_KEY));
                        }else if(!serverLocal && serverAddress != null && ip.equals(serverAddress.getHostAddress())){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_CATEGORIES, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_IPINFO_THREATMASK_KEY));
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
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_CLIENT_CATEGORIES, threatmask);
                        }else if(!serverLocal && 
                                ( ( serverAddress != null && ip.equals(serverAddress.getHostAddress()) ) ||
                                  ( serverUrl != null && ip.equals(serverUrl) )
                                ) 
                                ){
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION, ipInfo.getInt(WebrootQuery.BCTI_API_DAEMON_RESPONSE_URLINFO_REPUTATION_KEY));
                            sessionAttachments.globalAttach(AppSession.KEY_THREAT_PREVENTION_SERVER_CATEGORIES, threatmask);
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
     * Determine if cliient/server reputations match settings threashold.
     * @param  sessionAttachments SessionAttachments containing KEY_THREAT_PREVENTION_CLIENT_REPUTATION and/or KEY_THREAT_PREVENTION_CLIENT_REPUTATION.
     * @return                    true if match is within threshold, otherwise false.
     */
    public Boolean isMatch(SessionAttachments sessionAttachments)
    {
        Integer clientReputation = (Integer) sessionAttachments.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION);
        Integer serverReputation = (Integer) sessionAttachments.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION);
        Integer threatLevel = app.getSettings().getReputationThreshold();

        // time of getSettings().getValue() vs synchronized values.

        return ( ( ( serverReputation != null ) && serverReputation > 0 && serverReputation <= app.getSettings().getReputationThreshold() ) )
                ||
                ( ( ( clientReputation != null ) && clientReputation > 0 && clientReputation <= app.getSettings().getReputationThreshold() ) );
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
     * @return HttpRedirect object if redirect to block, null if no block.
     */
    public HttpRedirect checkRequest(AppTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        /*
         * this stores whether this visit should be flagged for any reason
         */
        Boolean isFlagged = false;
        /*
         * this stores the corresponding reason for the flag/block
         */
        URI requestUri = null;
        int pos;

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

        /*
         * We have seen a case where the host in the "Host"
         * header actually has a port number appended to it
         * bctid doesn't work with the port appended to the
         * host, so strip it here if it exists (NGFW-12877)
         */
        pos = host.indexOf(':');
        if (pos > 0) {
            host = host.substring(0, pos);
        }

        Boolean match = false;
        if(addressQuery(clientIp, sess.getServerAddr(), host + uri, sess)){
            match = isMatch(sess);
        }

        boolean block = app.getSettings().getAction().equals(ThreatPreventionApp.ACTION_BLOCK);
        boolean flag = app.getSettings().getFlag();
        Integer ruleIndex = null;

        ThreatPreventionRule matchRule = null;
        for (ThreatPreventionRule rule : app.getSettings().getRules()){
            if( rule.isMatch(sess) ){
                match = true;
                matchRule = rule;
                block = rule.getAction().equals(ThreatPreventionApp.ACTION_BLOCK);
                flag = rule.getFlag();
                ruleIndex = rule.getRuleId();
                break;
            }
        }

        Integer clientReputation = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION);
        if(clientReputation == null){
            clientReputation = 0;
        }
        Integer clientThreatmask = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_CATEGORIES);
        if(clientThreatmask == null){
            clientThreatmask = 0;
        }
        Integer serverReputation = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION);
        if(serverReputation == null){
            serverReputation = 0;
        }
        Integer serverThreatmask = (Integer) sess.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_CATEGORIES);
        if(serverThreatmask == null){
            serverThreatmask = 0;
        }

        app.incrementThreatCount(serverReputation);

        ThreatPreventionHttpEvent fwe = new ThreatPreventionHttpEvent(
            requestLine.getRequestLine(),
            sess.sessionEvent(),
            match && block, 
            match && flag, 
            ruleIndex != null ? ruleIndex : 0, 
            clientReputation,
            clientThreatmask,
            serverReputation,
            serverThreatmask
            );
        app.logEvent(fwe);

        if (match && block) {
            app.incrementBlockCount(); 
            if (flag){
                app.incrementFlagCount();
            }

            updateI18nMap();
            return (
                new HttpRedirect(
                    app.generateResponse(
                        new ThreatPreventionBlockDetails( app.getSettings(), host, uri.toString(), 
                            matchRule != null ? matchRule.getDescription() : I18nUtil.tr("Threat reputation {0}", app.getThreatFromReputation(serverReputation), i18nMap), clientIp),
                            sess, uri.toString(), header),
                        HttpRedirect.RedirectType.BLOCK)
            );
        } else {
            if (flag){
                app.incrementFlagCount();
            }
        }
        app.incrementPassCount();
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
