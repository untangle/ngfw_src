/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import java.util.List;

import com.untangle.uvm.vnet.*;
import com.untangle.uvm.vnet.event.*;

public class IPSSessionInfo {

    private     List<IPSRuleSignature>  c2sSignatures;
    private     List<IPSRuleSignature>  s2cSignatures;
    private     IPSession               session;
    private     IPDataEvent             event;
    private     String                  uriPath;
    private     boolean                 isServer;

    //ContentOption variables
    public int start;
    public int end;
    public int indexOfLastMatch;

    public IPSSessionInfo(IPSession session) {
        this.session = session;
    }

    //public void setContentOptionStart(int val) { start = val; }
    //public int getContentOptionStart(int val) { return start; }
    //public void setContentOptionEnd(int val) { end = val; }
    //public int getContentOptionEnd(int val) { return end; }

    public void setUriPath(String path) {
        uriPath = path;
    }

    public String  getUriPath() {
        return uriPath;
    }
    /**Do i need to set sessionion data? I dont think so..
     * Check later.
     */
    public IPSession getSession() {
        return session;
    }

    public void setC2SSignatures(List<IPSRuleSignature> signatures) {
        this.c2sSignatures = signatures;
    }

    public void setS2CSignatures(List<IPSRuleSignature> signatures) {
        this.s2cSignatures = signatures;
    }
    public void setEvent(IPDataEvent event) {
        this.event = event;
    }

    public IPDataEvent getEvent() {
        return event;
    }

    public void setFlow(boolean isServer) {
        this.isServer = isServer;
    }

    public boolean isServer() {
        return isServer;
    }

    // First match wins. XX
    public boolean processC2SSignatures() {
        for(IPSRuleSignature sig : c2sSignatures)
            if (sig.execute(this))
                return true;
        return false;
    }

    // First match wins. XX
    public boolean processS2CSignatures() {
        for(IPSRuleSignature sig : s2cSignatures)
            if (sig.execute(this))
                return true;
        return false;
    }

    // For debugging/loggin
    public int numC2SSignatures() {return c2sSignatures.size();}
    public int numS2CSignatures() {return s2cSignatures.size();}

    public void blockSession() {
        if(session instanceof TCPSession) {
            ((TCPSession)session).resetClient();
            ((TCPSession)session).resetServer();
        }
        else if(session instanceof UDPSession) {
            ((UDPSession)session).expireClient();
            ((UDPSession)session).expireServer();
        }
        session.release();
    }



    /**Debug methods*/
    public boolean testSignature(int num) {
        return c2sSignatures.get(num).execute(this);
    }

    public IPSRuleSignature getSignature(int num) {
        return c2sSignatures.get(num);
    }
    // */
}
