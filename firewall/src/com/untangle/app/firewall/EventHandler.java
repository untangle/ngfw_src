/*
 * $Id$
 */
package com.untangle.app.firewall;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

public class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);

    private List<FirewallRule> firewallRuleList = new LinkedList<FirewallRule>();

    private boolean blockSilently = true;

    /* Firewall App */
    private final FirewallApp app;

    public EventHandler( FirewallApp app )
    {
        super(app);

        this.app = app;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        boolean block = false;
        boolean flag = false;
        int ruleIndex     = 0;
        FirewallRule matchedRule = null;

        if ( Boolean.TRUE == request.globalAttachment( AppSession.KEY_FTP_DATA_SESSION) ) {
            logger.info("Passing FTP related session: " + request);
            return;
        }
        
        /**
         * Find the matching rule compute block/log verdicts
         */
        for (FirewallRule rule : firewallRuleList) {
            if (rule.isMatch(request.getProtocol(),
                             request.getClientIntf(), request.getServerIntf(),
                             request.getOrigClientAddr(), request.getNewServerAddr(),
                             request.getOrigClientPort(), request.getNewServerPort())) {
                matchedRule = rule;
                break;
            }
        }
        
        if (matchedRule != null) {
            block = matchedRule.getBlock();
            flag = matchedRule.getFlag();
            ruleIndex = matchedRule.getRuleId();
        }

        /**
         * Take the appropriate actions
         */
        if (block) {
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
            FirewallEvent fwe = new FirewallEvent(request.sessionEvent(), block, flag, ruleIndex);
            app.logEvent(fwe);

        } else { /* not blocked */

            if (logger.isDebugEnabled()) {
                logger.debug("Releasing session: " + request);
            }

            /* only finalize if logging */
            request.release();

            /* Increment the pass counter and flag counter */
            app.incrementPassCount();
            if (flag) app.incrementFlagCount();

            /* If necessary log the event */
            FirewallEvent fwe = new FirewallEvent(request.sessionEvent(), block, flag, ruleIndex);
            app.logEvent(fwe);
        }
    }

    public void configure(FirewallSettings settings)
    {
        this.firewallRuleList = settings.getRules();
    }

}
