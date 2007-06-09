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

package com.untangle.node.mail.papi;

import static com.untangle.node.util.Ascii.*;
import static com.untangle.node.util.Rfc822Util.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.untangle.node.mime.FileMIMESource;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEMessageHeaders;
import com.untangle.node.mime.MIMEParsingInputStream;
import com.untangle.node.token.Token;

public class MIMEMessageT implements Token
{
    /* need reference copy of File
     * because File cannot be accessed through FileMIMESource
     */
    private final File zMsgFile;
    private final FileMIMESource zFMSource;

    private MIMEMessageHeaders zMMHeader;
    private MIMEMessage zMMessage;
    private MessageInfo zMsgInfo;

    // constructors -----------------------------------------------------------

    public MIMEMessageT(File zMsgFile)
    {
        this.zMsgFile = zMsgFile;
        zFMSource = new FileMIMESource(zMsgFile);

        zMMHeader = null;
        zMMessage = null;
        zMsgInfo = null;
    }

    // static factories -------------------------------------------------------

    // accessors --------------------------------------------------------------

    public File getFile()
    {
        return zMsgFile;
    }

    public FileMIMESource getFileMIMESource()
    {
        return zFMSource;
    }

    public MIMEParsingInputStream getInputStream() throws IOException
    {
        return zFMSource.getInputStream();
    }

    public void setMIMEMessageHeader(MIMEMessageHeaders zMMHeader)
    {
        this.zMMHeader = zMMHeader;
        return;
    }

    public MIMEMessageHeaders getMIMEMessageHeader()
    {
        return zMMHeader;
    }

    public void setMIMEMessage(MIMEMessage zMMessage)
    {
        this.zMMessage = zMMessage;
        return;
    }

    public MIMEMessage getMIMEMessage()
    {
        return zMMessage;
    }

    public void setMessageInfo(MessageInfo zMsgInfo)
    {
        this.zMsgInfo = zMsgInfo;
        return;
    }

    public MessageInfo getMessageInfo()
    {
        return zMsgInfo;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return null;
    }

    public int getEstimatedSize()
    {
        return 0;
    }
}
