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

package com.untangle.uvm.engine;

import com.untangle.uvm.tapi.TCPNewSessionRequest;

class TCPNewSessionRequestImpl extends IPNewSessionRequestImpl implements TCPNewSessionRequest {

    protected TCPNewSessionRequestImpl(Dispatcher disp,
                                       com.untangle.uvm.argon.TCPNewSessionRequest pRequest,
                                       boolean isInbound) {
        super(disp, pRequest, isInbound);
    }

    public boolean acked() {
        return ((com.untangle.uvm.argon.TCPNewSessionRequest)pRequest).acked();
    }

    public void rejectReturnRst(boolean needsFinalization) {
        ((com.untangle.uvm.argon.TCPNewSessionRequest)pRequest).rejectReturnRst();
        this.needsFinalization = needsFinalization;
    }

    public void rejectReturnRst() {
        rejectReturnRst(false);
    }

}
