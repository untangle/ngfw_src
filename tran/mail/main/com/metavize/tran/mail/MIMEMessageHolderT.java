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

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.Rfc822Util.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.mime.FileMIMESource;
import com.metavize.tran.mime.MIMEMessage;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.mime.MIMEParsingInputStream;

public class MIMEMessageHolderT implements Token
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

    public MIMEMessageHolderT(File zMsgFile)
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
}
