/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail;

import java.nio.ByteBuffer;
import java.util.List;

public interface ByteEncoder
{
    List<ByteBuffer> encode(ByteBuffer buf);

    ByteBuffer endEncoding();
}
