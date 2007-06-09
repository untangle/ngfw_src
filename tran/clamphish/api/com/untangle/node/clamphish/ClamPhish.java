/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamPhishNode.java 8965 2007-02-23 20:54:04Z cng $
 */

package com.untangle.node.clamphish;

import com.untangle.uvm.logging.EventManager;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.spam.SpamNode;

public interface ClamPhish extends SpamNode
{
    EventManager<PhishHttpEvent> getPhishHttpEventManager();

    void setClamPhishSettings(ClamPhishSettings spamSettings);
    ClamPhishSettings getClamPhishSettings();

    ClamPhishBlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);
    UserWhitelistMode getUserWhitelistMode();
}
