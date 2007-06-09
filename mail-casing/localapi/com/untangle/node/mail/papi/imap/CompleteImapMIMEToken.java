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

import com.untangle.uvm.tapi.Pipeline;
import com.untangle.uvm.tapi.event.TCPStreamer;
import com.untangle.node.mail.papi.CompleteMIMEToken;
import com.untangle.node.mail.papi.MIMETCPStreamer;
import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mime.MIMEMessage;
import org.apache.log4j.Logger;


/**
 * Class representing a Complete MIME message.
 * This will be issued if an upstream Node
 * has buffered a complete message.
 * <br><br>
 * Adds a different {@link #toImapTCPStreamer streaming}
 * method which causes the leading literal ("{nnn}\r\n")
 * to be prepended.
 */
public class CompleteImapMIMEToken
    extends CompleteMIMEToken {

    private final Logger m_logger =
        Logger.getLogger(CompleteImapMIMEToken.class);

    public CompleteImapMIMEToken(MIMEMessage msg,
                                 MessageInfo msgInfo) {
        super(msg, msgInfo);
    }

    /**
     * Create a TCPStreamer for Imap (which includes the leading literal).
     */
    public TCPStreamer toImapTCPStreamer(Pipeline pipeline,
                                         boolean disposeMessageWhenDone) {

        MIMETCPStreamer mimeStreamer = createMIMETCPStreamer(pipeline, disposeMessageWhenDone);
        int len = (int) mimeStreamer.getFileLength();

        m_logger.debug("About to return a Literal-leading streamer for literal length: " + len);

        return new LiteralLeadingTCPStreamer(
                                             mimeStreamer,
                                             len);
    }
}
