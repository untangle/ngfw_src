/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpBlocker.java,v 1.2 2005/01/30 05:53:40 dmorris Exp $
 */

package com.metavize.tran.httpblocker;

import com.metavize.mvvm.tran.Transform;

public interface HttpBlocker extends Transform
{
    HttpBlockerSettings getHttpBlockerSettings();
    void setHttpBlockerSettings(HttpBlockerSettings settings);
}
