
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

package com.metavize.tran.virus;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.PopStateMachine;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.MIMEPart;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.TempFileFactory;
import org.apache.log4j.Logger;

public class VirusPopHandler extends PopStateMachine
{
    private final static Logger logger = Logger.getLogger(VirusPopHandler.class);
    private final static Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private final VirusScanner zScanner;
    private final String zVendorName;

    private final VirusMessageAction zMsgAction;
    private final boolean bScan;

    // constructors -----------------------------------------------------------

    VirusPopHandler(TCPSession session, VirusTransformImpl transform)
    {
        super(session);

        zScanner = transform.getScanner();
        zVendorName = zScanner.getVendorName();

        VirusPOPConfig zConfig;
        if (IntfConverter.INSIDE == session.clientIntf())
        {
            zConfig = transform.getVirusSettings().getPOPInbound();
logger.debug("inside");
        }
        else
        {
            zConfig = transform.getVirusSettings().getPOPOutbound();
logger.debug("outside");
        }
        bScan = zConfig.getScan();
        zMsgAction = zConfig.getMsgAction();
logger.debug("scan: " + bScan + ", message action: " + zMsgAction);
    }

    // PopStateMachine methods -----------------------------------------------

    protected TokenResult scanMessage() throws TokenException
    {
        if (true == zMMessage.isMultipart())
        {
logger.debug("message contains MIME parts");
            MIMEPart azMPart[] = zMMessage.getLeafParts(true);
            TempFileFactory zTFFactory = new TempFileFactory();

            File zMPFile;

            for (MIMEPart zMPart : azMPart)
            {
                if (false == zMPart.isMultipart())
                {
                    try
                    {
                        zMPFile = zMPart.getContentAsFile(zTFFactory, true);
                    }
                    catch (IOException exn)
                    {
                        throw new TokenException("cannot get message/mime part file: ", exn);
                    }

                    scanFile(zMPFile);
                }
            }
        }

        return new TokenResult(new Token[] { zMMHolderT }, null);
    }

    private void scanFile(File zFile) throws TokenException
    {
        if (false == bScan)
        {
            return;
        }

        try
        {
logger.debug("scanning message attachments");
            VirusScannerResult zScanResult = zScanner.scanFile(zFile.getPath());
            eventLogger.info(new VirusMailEvent(zMsgInfo, zScanResult, zMsgAction, zVendorName));

//XXXX message action - pass, clean
            if (VirusMessageAction.REMOVE == zMsgAction)
            {
                try
                {
                    zMMessage.getMPHeaders().addHeaderField("X-MV-Virus-Found", "YES");
                }
                catch (HeaderParseException exn2)
                {
                    throw new TokenException("cannot add header field: ", exn2);
                }
            }
            /* else CLEAN - do nothing */

            return;
        }
        catch (IOException exn)
        {
            throw new TokenException("cannot scan message/mime part file: ", exn);
        }
        catch (InterruptedException exn)
        { // XXX deal with this in scanner
            throw new TokenException("scan interrupted: ", exn);
        }
    }
}
