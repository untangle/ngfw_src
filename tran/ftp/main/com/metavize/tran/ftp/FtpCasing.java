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

package com.metavize.tran.ftp;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Tokenizer;
import com.metavize.tran.token.Untokenizer;

class FtpCasing extends AbstractCasing
{
    private final Tokenizer tokenizer;
    private final FtpUntokenizer untokenizer;

    // constructors -----------------------------------------------------------

    FtpCasing(TCPSession session, boolean clientSide)
    {
        tokenizer = clientSide ? new FtpClientTokenizer(session)
            : new FtpServerTokenizer(session);
        untokenizer = new FtpUntokenizer(session, clientSide);
    }

    // Casing methods ---------------------------------------------------------

    public Tokenizer tokenizer()
    {
        return tokenizer;
    }

    public Untokenizer untokenizer()
    {
        return untokenizer;
    }
}
