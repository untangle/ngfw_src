/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IPDataEvent.java,v 1.1 2004/12/18 00:44:22 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import java.nio.ByteBuffer;

public interface IPDataEvent
{
    ByteBuffer data();
}
