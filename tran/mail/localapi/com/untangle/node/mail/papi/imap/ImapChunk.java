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

package com.untangle.node.mail.papi.imap;

import java.nio.ByteBuffer;

import com.untangle.node.token.Chunk;


/**
 * A chunk (ByteBuffer) as part of the IMAP protocol
 * handling.
 */
public class ImapChunk
    extends Chunk {

    public ImapChunk(ByteBuffer data) {
        super(data==null?
              data:(data.position()>0?data.slice():data));
    }
}
