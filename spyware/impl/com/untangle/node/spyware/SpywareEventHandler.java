/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.util.Iterator;
import java.util.Set;
import java.util.List;

import com.untangle.node.util.IPSet;
import com.untangle.node.util.IPSetTrie;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import org.apache.log4j.Logger;

public class SpywareEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SpywareImpl node;

    private IPSet subnetSet  = null;

    public SpywareEventHandler(SpywareImpl node)
    {
        super(node);

        this.node = node;
    }

    public void subnetList(List<GenericRule> list)
    {
        if (null == list) {
            subnetSet = null;
        } else {
            IPSetTrie set = new IPSetTrie();

            for (Iterator<GenericRule> i = list.iterator(); i.hasNext(); ) {
                GenericRule rule = i.next();
                IPMaskedAddress ipm = new IPMaskedAddress(rule.getString());
                try {
                    ipm.bitString();
                } catch (Exception e) {
                    logger.error("BAD RULE: " + rule.getString(), e);
                }
                set.add(ipm,rule);
            }

            this.subnetSet = set;
        }
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event)
    {
        if (null != subnetSet) {
            detectSpyware(event.sessionRequest(), true);
        } else {
            logger.debug("spyware detection disabled");
        }
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
    {
        if (null != subnetSet) {
            detectSpyware(event.sessionRequest(), true);
        } else {
            logger.debug("spyware detection disabled");
        }
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
    {
        Session s = event.session();
        SpywareAccessEvent spe = (SpywareAccessEvent)s.attachment();
        if (null != spe) {
            node.statisticManager.incrSubnetAccess(); // logged subnet access
            node.log(spe);
        } else {
            node.statisticManager.incrPass(); // pass subnet access
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
    {
        Session s = event.session();
        SpywareAccessEvent spe = (SpywareAccessEvent)s.attachment();
        if (null != spe) {
            node.statisticManager.incrSubnetAccess(); // logged subnet access
            node.log(spe);
        } else {
            node.statisticManager.incrPass(); // pass subnet access
        }
    }

    void detectSpyware(IPNewSessionRequest ipr, boolean release)
    {
        IPMaskedAddress ipm = new IPMaskedAddress(ipr.serverAddr().getHostAddress());

        GenericRule ir = (GenericRule)this.subnetSet.getMostSpecific(ipm);

        if (ir == null) {
            node.statisticManager.incrPass(); // pass subnet access
            if (logger.isDebugEnabled()) {
                logger.debug("Subnet scan: " + ipm.toString() + " -> clean.");
            }
            if (release) { ipr.release(); }
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Subnet scan: " + ipm.toString() + " -> DETECTED.");
        }

        node.incrementSubnetScan();

        if (logger.isInfoEnabled()) {
            logger.info("-------------------- Detected Subnet --------------------");
            logger.info("Subnet Name  : " + ir.getName());
            logger.info("Host          : " + ipr.clientAddr().getHostAddress() + ":" + ipr.clientPort());
            logger.info("Suspicious IP : " + ipr.serverAddr().getHostAddress() + ":" + ipr.serverPort());
            logger.info("Matches       : " + ir.getString());
            if (ipr instanceof TCPNewSessionRequest)
                logger.info("Protocol      : TCP");
            if (ipr instanceof UDPNewSessionRequest)
                logger.info("Protocol      : UDP");
            logger.info("----------------------------------------------------------");
        }

        ipr.attach(new SpywareAccessEvent(ipr.pipelineEndpoints(), ir.getName(), new IPMaskedAddress(ir.getString()), ir.getEnabled()));

        /**
         * Blocking has been disabled due to the poor quality of the list and support headaches associated with it.
         */
        /**
        if (ir.getEnabled()) {
            node.incrementSubnetBlock();
            if (ipr instanceof TCPNewSessionRequest) {
                ((TCPNewSessionRequest)ipr).rejectReturnRst(true);
            }
            if (ipr instanceof UDPNewSessionRequest) {
                ipr.rejectReturnUnreachable(IPNewSessionRequest.PROHIBITED,true);
            }
            return;
        }
        */

        if (release) { ipr.release(true); }
    }
}
