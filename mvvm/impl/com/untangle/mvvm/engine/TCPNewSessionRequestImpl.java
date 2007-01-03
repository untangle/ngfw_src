/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import com.untangle.mvvm.tapi.TCPNewSessionRequest;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest {

    protected TCPNewSessionRequestImpl(Dispatcher disp,
                                       com.untangle.mvvm.argon.TCPNewSessionRequest pRequest,
                                       boolean isInbound) {
        super(disp, pRequest, isInbound);
    }

    public boolean acked() {
        return ((com.untangle.mvvm.argon.TCPNewSessionRequest)pRequest).acked();
    }

    public void rejectReturnRst() {
        ((com.untangle.mvvm.argon.TCPNewSessionRequest)pRequest).rejectReturnRst();
    }

}
