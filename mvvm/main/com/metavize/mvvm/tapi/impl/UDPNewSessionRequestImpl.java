/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPNewSessionRequestImpl.java,v 1.1 2005/01/06 23:15:42 jdi Exp $
 */

package com.metavize.mvvm.tapi.impl;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipe;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest {

    protected UDPNewSessionRequestImpl(Dispatcher disp, com.metavize.mvvm.argon.UDPNewSessionRequest pRequest) {
        super(disp, pRequest);
    }
}
