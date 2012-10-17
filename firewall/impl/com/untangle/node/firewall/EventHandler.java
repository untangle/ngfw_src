/*
 * $Id$
 */
package com.untangle.node.firewall;

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
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);

    private List<FirewallRule> firewallRuleList = new LinkedList<FirewallRule>();

    private boolean blockSilently = true;

    /* Firewall Node */
    private final FirewallImpl node;

    public EventHandler(FirewallImpl node)
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        boolean block = false;
        boolean flag = false;
        int ruleIndex     = 0;
        FirewallRule matchedRule = null;

        /**
         * Find the matching rule compute block/log verdicts
         */
        for (FirewallRule rule : firewallRuleList) {
            if (rule.isMatch(request.getProtocol(),
                             request.getClientIntf(), request.getServerIntf(),
                             request.getClientAddr(), request.getNatToHost(),
                             request.getClientPort(), request.getNatToPort(),
                             (String)request.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME))) {
                matchedRule = rule;
                break;
            }
        }
        
        if (matchedRule != null) {
            block = matchedRule.getBlock();
            flag = matchedRule.getFlag();
            ruleIndex = matchedRule.getId();
        }

        /**
         * Take the appropriate actions
         */
        if (block) {
            if (logger.isDebugEnabled()) {
                logger.debug("Blocking session: " + request);
            }

            if (blockSilently) {
                request.rejectSilently( true );
            } else {
                if (protocol == Protocol.UDP) {
                    request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE, true );
                } else {
                    ((TCPNewSessionRequest)request).rejectReturnRst( true );
                }
            }

            /* Increment the block counter and flag counter*/
            node.incrementBlockCount(); 
            if (flag) node.incrementFlagCount();

            /* We just blocked, so we have to log too, regardless of what the rule actually says */
            FirewallEvent fwe = new FirewallEvent(request.sessionEvent(), block, flag, ruleIndex);
            request.attach(fwe);

        } else { /* not blocked */

            if (logger.isDebugEnabled()) {
                logger.debug("Releasing session: " + request);
            }

            /* only finalize if logging */
            request.release( true );

            /* Increment the pass counter and flag counter */
            node.incrementPassCount();
            if (flag) node.incrementFlagCount();

            /* If necessary log the event */
            FirewallEvent fwe = new FirewallEvent(request.sessionEvent(), block, flag, ruleIndex);
            request.attach(fwe);
        }
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
    {
        NodeSession s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.logEvent(fe);
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
    {
        NodeSession s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.logEvent(fe);
        }
    }

    public void configure(FirewallSettings settings)
    {
        this.firewallRuleList = settings.getRules();
    }

}
