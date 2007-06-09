/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: $
 */

package com.untangle.tran.spam;

public class RBLClientContext {
    private String hostname;
    private String ipAddr;
    private String invertedIPAddr;

    private volatile Boolean isBlacklisted = null;

    public RBLClientContext(String hostname, String ipAddr, String invertedIPAddr) {
        this.hostname = hostname;
        this.ipAddr = ipAddr;
        this.invertedIPAddr = invertedIPAddr;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIPAddr() {
        return ipAddr;
    }

    public String getInvertedIPAddr() {
        return invertedIPAddr;
    }

    public void setResult(Boolean isBlacklisted) {
        this.isBlacklisted = isBlacklisted;
        return;
    }

    public Boolean getResult() {
        return isBlacklisted;
    }
}
