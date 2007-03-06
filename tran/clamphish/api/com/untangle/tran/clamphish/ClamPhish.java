/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamPhishTransform.java 8965 2007-02-23 20:54:04Z cng $
 */

package com.untangle.tran.clamphish;

import com.untangle.tran.http.UserWhitelistMode;
import com.untangle.tran.spam.SpamTransform;

public interface ClamPhish extends SpamTransform
{
    ClamPhishBlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);
    UserWhitelistMode getUserWhitelistMode();
}
