/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPNewSessionRequestImpl.java,v 1.1 2005/01/06 20:51:29 jdi Exp $
 */

package com.metavize.mvvm.tapi.impl;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipe;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest {

    protected TCPNewSessionRequestImpl(Dispatcher disp, com.metavize.mvvm.argon.TCPNewSessionRequest pRequest) {
        super(disp, pRequest);
    }

    public boolean acked() {
        return ((com.metavize.mvvm.argon.TCPNewSessionRequest)pRequest).acked();
    }

    public void rejectReturnRst() {
        ((com.metavize.mvvm.argon.TCPNewSessionRequest)pRequest).rejectReturnRst();
    }

}
