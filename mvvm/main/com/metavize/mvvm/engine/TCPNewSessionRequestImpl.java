/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.tapi.TCPNewSessionRequest;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest {

    protected TCPNewSessionRequestImpl(Dispatcher disp,
                                       com.metavize.mvvm.argon.TCPNewSessionRequest pRequest,
                                       boolean isInbound) {
        super(disp, pRequest, isInbound);
    }

    public boolean acked() {
        return ((com.metavize.mvvm.argon.TCPNewSessionRequest)pRequest).acked();
    }

    public void rejectReturnRst() {
        ((com.metavize.mvvm.argon.TCPNewSessionRequest)pRequest).rejectReturnRst();
    }

}
