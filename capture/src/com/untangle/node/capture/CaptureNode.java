/**
 * $Id$
 */

package com.untangle.node.capture;

import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.node.EventEntry;
import com.untangle.uvm.node.Node;
import java.util.ArrayList;
import java.net.InetAddress;

public interface CaptureNode extends Node
{
    enum BlingerType
    {
        SESSALLOW, SESSBLOCK, SESSQUERY, AUTHGOOD, AUTHFAIL
    }

    CaptureSettings getCaptureSettings();

    CaptureSettings getSettings();

    void setCaptureSettings(CaptureSettings settings);

    void setSettings(CaptureSettings settings);

    ArrayList<CaptureUserEntry> getActiveUsers();

    EventEntry[] getUserEventQueries();

    EventEntry[] getRuleEventQueries();

    int userAuthenticate(InetAddress address, String username, String password);

    int userActivate(InetAddress address, String username, String agree, boolean anonymous);

    int userActivate(InetAddress address, String agree);

    int userAdminLogout(InetAddress address);

    int userLogin(InetAddress address, String username);

    int userLogout(InetAddress address);

    boolean isClientAuthenticated(InetAddress clientAddr);

    PassedAddress isSessionAllowed(InetAddress clientAddr, InetAddress serverAddr);

    CaptureRule checkCaptureRules(IPNewSessionRequest sessreq);

    CaptureRule checkCaptureRules(NodeTCPSession session);

    boolean isUserInCookieTable(InetAddress address, String username);

    void removeUserFromCookieTable(InetAddress address);

}
