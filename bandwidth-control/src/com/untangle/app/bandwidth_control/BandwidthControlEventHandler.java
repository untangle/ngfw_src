/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import org.apache.log4j.Logger;
import java.util.List;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * This is the traffic EventHandler responsible for Bandwidth Control
 * It mainly evaluates the rules on traffic and takes the specified actions
 */
public class BandwidthControlEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( BandwidthControlEventHandler.class );

    private static final int TCP_HEADER_SIZE_ESTIMATE = 32;
    private static final int IP_HEADER_SIZE = 20;
    private static final int UDP_HEADER_SIZE = 8;
    
    private final int MAX_CHUNK_COUNT = 10;
    
    private BandwidthControlApp app;

    /**
     * Create a BandWidthControlEventHandler instance
     * @param app - the owning BandwidthControlApp
     */
    public BandwidthControlEventHandler(BandwidthControlApp app)
    {
        super(app);

        this.app = app;
    }

    /**
     * Handle a new TCP session request event
     * @param sessionRequest
     */
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        _handleNewSessionRequest( sessionRequest, Protocol.TCP );
    }

    /**
     * Handle a new UDP session request event
     * @param sessionRequest
     */
    public void handleUDPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        _handleNewSessionRequest( sessionRequest, Protocol.UDP );
    }

    /**
     * Handle a new TCP complete event
     * @param session
     */
    public void handleTCPComplete( AppTCPSession session )
    {
        try {
            _handleSession( null, session, Protocol.TCP );
        }
        catch (Exception e) {
            logger.warn("Exception handling TCP Complete: ",e);
        }
    }

    /**
     * Handle a new UDP complete/timeout event
     * @param session
     */
    public void handleUDPComplete( AppUDPSession session )
    {
        try {
            _handleSession( null, session, Protocol.UDP );
        }
        catch (Exception e) {
            logger.warn("Exception handling UDP complete: ",e);
        }
    }

    /**
     * Handle a new UDP client packet
     * @param session
     * @param data - the data (unused)
     * @param header - the packet header
     */
    public void handleUDPClientPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        _handleSession( data, session, Protocol.UDP );
        session.sendServerPacket( data, header );
    }

    /**
     * Handle a new UDP server packet
     * @param session
     * @param data - the data (unused)
     * @param header - the packet header
     */
    public void handleUDPServerPacket( AppUDPSession session, ByteBuffer data, IPPacketHeader header )
    {
        _handleSession( data, session, Protocol.UDP );
        session.sendClientPacket( data, header );
    }

    /**
     * Handle a new TCP client packet
     * @param session
     * @param data - the data (unused)
     */
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        _handleSession( data, session, Protocol.TCP );
        session.sendDataToServer( data );
        return;
    }

    /**
     * Handle a new TCP server packet
     * @param session
     * @param data - the data (unused)
     */
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        _handleSession( data, session, Protocol.TCP );
        session.sendDataToClient( data );
        return;
    }

    /**
     * Reprioritize all existing sessions for the specified address.
     * This is used when state changes about hosts in the host table and we
     * need to reprioritize existing sessions, like when a host's quota
     * is exceeded.
     *
     * @param addr - the address of the host
     * @param reason - just used for logging to debug logs
     */
    protected void reprioritizeHostSessions(InetAddress addr, String reason)
    {
        if ( addr == null )
            return;
        
        logger.info("Reprioritizing Sessions for " + addr.getHostAddress() + " because \"" + reason + "\"");

        for (AppSession sess : this.app.liveAppSessions()) {
            if (addr.equals(sess.getClientAddr()) || addr.equals(sess.getServerAddr())) {
                logger.debug( "Reevaluating AppSession : " + sess.getProtocol() + " " +
                              sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                              sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());


                BandwidthControlRule rule = _findFirstMatch(sess, true);
        
                if (rule != null) {
                    try {
                        rule.getAction().apply( sess );
                    } catch (Exception e) {
                        logger.warn("Failed to reprioritize session: " + sess.getProtocol() + " " +
                                    sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                                    sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort(), e);
                    }
                }
            }
        }
    }

    /**
     * Reprioritize all existing sessions for the specified username.
     * This is used when state changes about users in the user table and we
     * need to reprioritize existing sessions, like when a user's quota
     * is exceeded.
     *
     * @param username - the username
     * @param reason - just used for logging to debug logs
     */
    protected void reprioritizeUserSessions(String username, String reason)
    {
        if ( username == null )
            return;
        
        logger.info("Reprioritizing Sessions for " + username + " because \"" + reason + "\"");

        for (AppSession sess : this.app.liveAppSessions()) {
            if ( username.equals(sess.user())) {
                logger.debug( "Reevaluating AppSession : " + sess.getProtocol() + " " +
                              sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                              sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());


                BandwidthControlRule rule = _findFirstMatch(sess, true);
        
                if (rule != null) {
                    try {
                        rule.getAction().apply( sess );
                    } catch (Exception e) {
                        logger.warn("Failed to reprioritize session: " + sess.getProtocol() + " " +
                                    sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                                    sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort(), e);
                    }
                }
            }
        }
    }
    
    /**
     * This handles both TCP and UDP session request
     * It just creates bandwidth control session state and attaches it to the session
     * @param request the session request
     * @param protocol (UDP/TCP)
     */
    private void _handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "New AppSession Request: " + protocol + " " +
                          request.getOrigClientAddr().getHostAddress() + ":" + request.getOrigClientPort() + " -> " +
                          request.getNewServerAddr().getHostAddress() + ":" + request.getNewServerPort());
        }

        BandwidthControlSessionState sessInfo = new BandwidthControlSessionState();
        request.attach(sessInfo);
    }

    /**
     * This handles session data
     * It will evaulate the rules and take the appropriate action
     * @param data - the packet data
     * @param sess - the session
     * @param protocol (UDP/TCP)
     */
    private void _handleSession( ByteBuffer data, AppSession sess, Protocol protocol )
    {
        BandwidthControlSessionState sessInfo = (BandwidthControlSessionState)sess.attachment();
        if (sessInfo == null) {
            sessInfo = new BandwidthControlSessionState();
            sess.attach(sessInfo);
        }

        if (! this.app.isLicenseValid()) 
            return;
        
        /**
         * If we are too deep in the session - stop running the rules
         */
        sessInfo.chunkCount++;
        if (sessInfo.chunkCount > MAX_CHUNK_COUNT) {
            sess.release();
            return;
        }
        
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Session Event  : " + protocol + " " +
                          sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                          sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());
        }

        /**
         * Check for a matching rule and apply it
         */
        BandwidthControlRule rule = _findFirstMatch(sess);
        
        if (rule != null)
            rule.getAction().apply( sess );

    }
    
    /**
     * Utility function to find the first matching rule of the current ruleset
     * for the specified sessions
     * @param sess - the session
     * @return the matching rule or null if none
     */
    private BandwidthControlRule _findFirstMatch(AppSession sess)
    {
        return _findFirstMatch(sess, false);
    }
    
    /**
     * Utility function to find the first matching rule of the current ruleset
     * for the specified sessions
     * @param sess - the session
     * @param onlyPrioritizeRules - if true only look at prioritization rules
     * @return the matching rule or null if none
     */
    private BandwidthControlRule _findFirstMatch(AppSession sess, boolean onlyPrioritizeRules)
    {
        List<BandwidthControlRule> rules = this.app.getRules();

        if ( logger.isDebugEnabled() ) {
            logger.debug( "Checking Rules against AppSession : " + sess.getProtocol() + " " +
                          sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                          sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());
        }

        for ( BandwidthControlRule rule : rules ) {
            boolean evalRule = true;

            if (onlyPrioritizeRules) {
                /**
                 * Only check rules that set priority for reprioritization if allRules is false
                 * This is used for reprioritizing sessions and we only want to match certain rules
                 */
                evalRule = ( rule.getAction().getActionType() == BandwidthControlRuleAction.ActionType.SET_PRIORITY ||
                             rule.getAction().getActionType() == BandwidthControlRuleAction.ActionType.APPLY_PENALTY_PRIORITY );
            }
            
            if (rule.getEnabled() && evalRule && rule.matches( sess )) {
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "Matched AppSession : " + sess.getProtocol() + " " +
                                  sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                                  sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort() + " matches " + rule.getDescription());
                }
                return rule; /* check no further */
            } else {
                if ( logger.isDebugEnabled() ) {
                    logger.debug( "Checking Rule \"" + rule.getDescription() + "\" against AppSession : " + sess.getProtocol() + " " +
                                  sess.getClientAddr().getHostAddress() + ":" + sess.getClientPort() + " -> " +
                                  sess.getServerAddr().getHostAddress() + ":" + sess.getServerPort());
                }
            }
        }

        return null;
    }

}
