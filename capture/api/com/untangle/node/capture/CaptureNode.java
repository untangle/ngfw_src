/**
 * $Id: CaptureNode.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.Node;
import java.util.ArrayList;

public interface CaptureNode extends Node
{
    enum BlingerType { SESSALLOW, SESSBLOCK, SESSPROXY, AUTHGOOD, AUTHFAIL }

    CaptureSettings getSettings();
    void setSettings(CaptureSettings settings);

    ArrayList<CaptureUserEntry> getActiveUsers();

    EventLogQuery[] getLoginEventQueries();
    EventLogQuery[] getBlockEventQueries();
    EventLogQuery[] getRuleEventQueries();

    int userAuthenticate(String address, String username, String password);
    int userActivate(String address, String agree);
    int userLogout(String address);

    boolean isSessionAllowed(String clientAddr,String serverAddr);
    CaptureRule checkCaptureRules(NodeSession session);
}
