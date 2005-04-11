/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

import java.nio.ByteBuffer;

public interface Tokenizer
{
    TokenStreamer endSession();
    TokenizerResult tokenize(ByteBuffer chunk) throws TokenizerException;
    void handleTimer();
}
