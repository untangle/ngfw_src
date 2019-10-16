/**
 * $Id: EventHandler.java,v 1.00 2018/05/10 20:44:51 dmorris Exp $
 */
package com.untangle.app.threat_prevention;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.app.webroot.WebrootQuery;

/**
 * The Threat Prevention (non HTTP, non HTTPS) event handler
 */
public class ThreatPreventionEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(ThreatPreventionEventHandler.class);

    private List<ThreatPreventionRule> threatPreventionRuleList = new LinkedList<>();

    private boolean blockSilently = true;

    /* Threat Prevention App */
    private final ThreatPreventionApp app;

    private Map<String, String> i18nMap;
    Long i18nMapLastUpdated = 0L;

    /**
     * Create a new EventHandler.
     * @param app - the containing threat prevention app
     */
    public ThreatPreventionEventHandler( ThreatPreventionApp app )
    {
        super(app);

        this.app = app;
    }

    /**
     * Handle a new TCP session
     * @param sessionRequest
     */
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    /**
     * Handle a new UDP session
     * @param sessionRequest
     */
    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    /**
     * Handle a new session
     * @param request
     * @param protocol
     */
    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {

        if( protocol == Protocol.TCP &&
            ( request.getNewServerPort() == 80 ) ||
            ( request.getNewServerPort() == 443) ){
            // Would be better to rework pipeline to not even gett here
            return;
        }

        if ( Boolean.TRUE == request.globalAttachment( AppSession.KEY_FTP_DATA_SESSION) ) {
            logger.info("Passing FTP related session: " + request);
            return;
        }

        boolean block = app.getSettings().getAction().equals(ThreatPreventionApp.ACTION_BLOCK);
        boolean flag = app.getSettings().getFlag();
        Integer ruleIndex = null;

        Boolean match = false;
        if(app.getDecisionEngine().addressQuery(request.getOrigClientAddr(), request.getNewServerAddr(), request)){
            match = app.getDecisionEngine().isMatch(request);
        }

        Integer clientReputation = (Integer) request.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_REPUTATION);
        Integer clientThreatmask = (Integer) request.globalAttachment(AppSession.KEY_THREAT_PREVENTION_CLIENT_CATEGORIES);
        Integer serverReputation = (Integer) request.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_REPUTATION);
        Integer serverThreatmask = (Integer) request.globalAttachment(AppSession.KEY_THREAT_PREVENTION_SERVER_CATEGORIES);

        for (ThreatPreventionRule rule : app.getSettings().getRules()){
            if( rule.isMatch(request.getProtocol(),
                            request.getClientIntf(), request.getServerIntf(),
                            request.getOrigClientAddr(), request.getNewServerAddr(),
                            request.getOrigClientPort(), request.getNewServerPort(),
                            request) ){
                match = true;
                block = rule.getAction().equals(ThreatPreventionApp.ACTION_BLOCK);
                flag = rule.getFlag();
                ruleIndex = rule.getRuleId();
                break;
            }
        }

        // log method?
        ThreatPreventionEvent fwe = new ThreatPreventionEvent(request.sessionEvent(), block && match, match && flag, 
            ruleIndex != null ? ruleIndex : 0, 
            clientReputation != null ? clientReputation : 0, 
            clientThreatmask != null ? clientThreatmask : 0, 
            serverReputation != null ? serverReputation : 0, 
            serverThreatmask != null ? serverThreatmask : 0
            );
        app.logEvent(fwe);

        // !!!! VERIFY NO MATCH IF BCTID NOT RUNNING

        /**
         * Take the appropriate actions
         */
        if (match && block) {
            if (logger.isDebugEnabled()) {
                logger.debug("Blocking session: " + request);
            }

            if (blockSilently) {
                request.rejectSilently();
            } else {
                if (protocol == Protocol.UDP) {
                    request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE );
                } else {
                    ((TCPNewSessionRequest)request).rejectReturnRst();
                }
            }

            /* Increment the block counter and flag counter*/
            app.incrementBlockCount(); 
            if (flag) app.incrementFlagCount();

            /* We just blocked, so we have to log too, regardless of what the rule actually says */
        } else { /* not blocked */
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing session: " + request);
            }

            /* only finalize if logging */
            request.release();

            /* Increment the pass counter and flag counter */
            app.incrementPassCount();
            if (match && flag){
                app.incrementFlagCount();
            }
        }
    }

}
