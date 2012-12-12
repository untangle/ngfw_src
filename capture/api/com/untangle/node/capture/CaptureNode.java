/**
 * $Id: CaptureNode.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.Node;
import java.util.ArrayList;
import java.net.InetAddress;

public interface CaptureNode extends Node
{
    enum BlingerType { SESSALLOW, SESSBLOCK, SESSQUERY, AUTHGOOD, AUTHFAIL }

    CaptureSettings getCaptureSettings();
    CaptureSettings getSettings();

    void setCaptureSettings(CaptureSettings settings);
    void setSettings(CaptureSettings settings);

    ArrayList<CaptureUserEntry> getActiveUsers();

    EventLogQuery[] getUserEventQueries();
    EventLogQuery[] getRuleEventQueries();

    int userAuthenticate(InetAddress address, String username, String password);
    int userActivate(InetAddress address, String agree);
    int userAdminLogout(InetAddress address);
    int userLogout(InetAddress address);

    boolean isClientAuthenticated(InetAddress clientAddr);
    PassedAddress isSessionAllowed(InetAddress clientAddr,InetAddress serverAddr);
    CaptureRule checkCaptureRules(IPNewSessionRequest sessreq);
    CaptureRule checkCaptureRules(NodeTCPSession session);
}
