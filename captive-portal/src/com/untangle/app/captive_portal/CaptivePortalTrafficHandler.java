/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.UvmContextFactory;
import org.apache.log4j.Logger;

/**
 * This class contains the main traffic handlers where we decide if traffic
 * should be captured or allowed.
 * 
 * @author mahotz
 * 
 */
public class CaptivePortalTrafficHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private CaptivePortalApp app = null;

    /**
     * Constructor
     * 
     * @param app
     *        The application instance that created us
     */
    public CaptivePortalTrafficHandler(CaptivePortalApp app)
    {
        super(app);
        this.app = app;
    }

    /**
     * This is the handler for all TCP traffic. The basic goal is to block all
     * traffic from unauthenticated clients, and allow all traffic from
     * authenticated clients. We apply capture and passed host rules as
     * appropriate and handle edge cases where we need to work cooperatively
     * with other apps and services that may be installed.
     * 
     * @param sessreq
     *        The session request passed to us when the session was created
     * @return = nothing
     */
    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessreq)
    {
        // first we look for and ignore all traffic on port 80 since
        // the http handler will take care of all that
        if (sessreq.getNewServerPort() == 80) {
            sessreq.release();
            return;
        }

        // next check is to see if the user is already authenticated
        if (app.isClientAuthenticated(sessreq.getOrigClientAddr()) == true) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // not authenticated so check both of the pass lists
        PassedAddress passed = app.isSessionAllowed(sessreq.getOrigClientAddr(), sessreq.getNewServerAddr());

        if (passed != null) {
            if (passed.getLog() == true) {
                CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), false);
                app.logEvent(logevt);
            }

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // not authenticated and no pass list match so check the rules
        CaptureRule rule = app.checkCaptureRules(sessreq);

        // by default we allow traffic so if there is no rule pass the traffic
        if (rule == null) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // if we found a pass rule then log and let the traffic pass
        if (rule.getCapture() == false) {
            CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), rule);
            app.logEvent(logevt);

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // If the traffic needs to be captured and ssl inspector is active then
        // we set a special flag to force it to inspect even if it would
        // otherwise be ignored. This will let us capture the HTTP session
        // and ensure any redirect is sent using the MitM certificate which
        // will eliminate errors on capture redirect even to pages that we
        // aren't ultimately going to inspect once the client has authenticated.
        // We store the authentication type as the flag so ssl inspector can
        // ignore the same sessions we allow to pass so the login page can
        // be loaded from the external server.
        if (sessreq.globalAttachment(AppSession.KEY_SSL_INSPECTOR_SERVER_MANAGER) != null) {
            switch (app.getSettings().getAuthenticationType())
            {
            case MICROSOFT:
                sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE, "MICROSOFT");
                break;
            case FACEBOOK:
                sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE, "FACEBOOK");
                break;
            case GOOGLE:
                sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE, "GOOGLE");
                break;
            case ANY_OAUTH:
                sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE, "ANY_OAUTH");
                break;
            default:
                sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE, "CAPTURE");
                break;
            }
            sessreq.release();
            return;
        }

        // the traffic needs to be blocked but we have detected SSL traffic
        // so we add a special global attachment that the https handler uses
        // to detect sessions that need https-->http redirection but only if
        // that feature is not disabled.
        if ((app.getSettings().getDisableSecureRedirect() == false) && (sessreq.getNewServerPort() == 443)) {
            sessreq.globalAttach(AppSession.KEY_CAPTIVE_PORTAL_REDIRECT, sessreq.getOrigClientAddr());
            sessreq.release();
            return;
        }

        // not yet allowed and we found a block rule so shut it down
        CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), rule);
        app.logEvent(logevt);
        app.incrementBlinger(CaptivePortalApp.BlingerType.SESSBLOCK, 1);
        sessreq.rejectReturnRst();
    }

    /**
     * This is the handler for all UDP traffic. The basic goal is to block all
     * traffic from unauthenticated clients, and allow all traffic from
     * authenticated clients. We apply capture and passed host rules as
     * appropriate and handle edge cases where we need to work cooperatively
     * with other apps and services that may be installed. We also have special
     * handling for port 53 DNS traffic which we must service to allow the
     * initial browser request and capture redirect to happen.
     * 
     * @param sessreq
     *        The session request passed to us when the session was created
     * @return = nothing
     */
    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequest sessreq)
    {
        // first check is to see if the user is already authenticated
        if (app.isClientAuthenticated(sessreq.getOrigClientAddr()) == true) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // not authenticated so check both of the pass lists
        PassedAddress passed = app.isSessionAllowed(sessreq.getOrigClientAddr(), sessreq.getNewServerAddr());

        if (passed != null) {
            if (passed.getLog() == true) {
                CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), false);
                app.logEvent(logevt);
            }

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // not authenticated and no pass list match so check the rules
        CaptureRule rule = app.checkCaptureRules(sessreq);

        // by default we allow traffic so if there is no rule pass the traffic
        if (rule == null) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // if we found a pass rule then log and let the traffic pass
        if (rule.getCapture() == false) {
            CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), rule);
            app.logEvent(logevt);

            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSALLOW, 1);
            sessreq.release();
            return;
        }

        // traffic not yet allowed so we hook DNS traffic which will
        // allow us to do the lookup ourselves. this will ensure we
        // can't be circumvented by creative UDP port 53 traffic and it
        // will allow HTTP requests to become established which is
        // required for the http-casing to do the redirect
        if (sessreq.getNewServerPort() == 53) {
            // just return here which will cause all subsequent query
            // packets to hit the handleUDPClientPacket method
            return;
        }

        // not yet allowed and we found a block rule and the traffic
        // isn't DNS so shut it down
        CaptureRuleEvent logevt = new CaptureRuleEvent(sessreq.sessionEvent(), rule);
        app.logEvent(logevt);
        app.incrementBlinger(CaptivePortalApp.BlingerType.SESSBLOCK, 1);
        sessreq.rejectSilently();
    }

    /**
     * This handler will only see UDP packets with a target port of 53 sent from
     * an unauthenticated client to an external DNS server. We hook queries for
     * our FQDN and answer them directly picking the best response based on the
     * client interface. For all other queries we do a standard lookup.
     * 
     * We first make sure we received a valid DNS query. If not, we return a
     * REFUSED message, otherwise we do the name to address lookup, and send
     * back the response. The primary goal is to service DNS requests ourselves
     * rather than pass traffic externally to thwart attempts by unauthenticated
     * clients to tunnel traffic in creative ways. We also need to give back a
     * sane response to LAN and VPN clients to handle cases where the capture
     * redirect is generated using the hostname instead of IP address.
     * 
     * @param session
     *        The session details
     * @param data
     *        The UDP data received from the client
     * @param header
     *        The IP packet header received from the client
     * @return = nothing
     */
    @Override
    public void handleUDPClientPacket(AppUDPSession session, ByteBuffer data, IPPacketHeader header)
    {
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        DNSPacket packet = new DNSPacket();
        ByteBuffer response = null;
        InetAddress addr = null;

        // extract the DNS query from the client packet
        packet.ExtractQuery(data.array(), data.limit());
        logger.debug(packet.queryString());

        // if it doesn't seem like a valid query create a REFUSED response 
        if (packet.isValidDNSQuery() == false) {
            app.incrementBlinger(CaptivePortalApp.BlingerType.SESSBLOCK, 1);
            response = packet.GenerateResponse(null);

            // send the packet to the client and return
            session.sendClientPacket(response, header);
            logger.debug(packet.replyString());
            return;
        }

        // start with our hostname but prefer hostName + domainName if both are
        // defined, and always add the trailing dot for the name comparison
        String fullName = (hostName + ".");
        if ((domainName != null) && (domainName.length() > 0)) fullName = (hostName + "." + domainName + ".");

        // if the query is for our hostname we get the HTTP address for the
        // client interface where the query was received
        if (fullName.toLowerCase().equals(packet.getQname().toLowerCase())) {
            logger.debug("Query for our hostname detected. Returning our address for client interface " + Integer.toString(session.getClientIntf()));
            addr = UvmContextFactory.context().networkManager().getInterfaceHttpAddress(session.getClientIntf());
        }

        // if any other query or addr is null because getInterfaceHttpAddress
        // returned null we just do a standard resolver lookup.
        if (addr == null) {
            try {
                addr = InetAddress.getByName(packet.getQname());
            }

            // if resolution fails for any reason addr will be null and
            // the response generator will create a servfail message
            catch (Exception e) {
                logger.info("Exception attempting to resolve " + packet.getQname() + " = " + e);
            }
        }

        app.incrementBlinger(CaptivePortalApp.BlingerType.SESSQUERY, 1);
        response = packet.GenerateResponse(addr);

        // send the packet to the client
        session.sendClientPacket(response, header);
        logger.debug(packet.replyString());
    }
}
