/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: CasingFactory.java,v 1.3 2005/01/29 00:20:22 amread Exp $
 */

package com.metavize.tran.token;

public interface CasingFactory
{
    Casing casing(boolean first);
}
