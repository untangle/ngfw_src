/*
		m_virusImpl.increment
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.virus;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.imap.BufferingImapTokenStreamHandler;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEPart;
import com.untangle.node.mime.MIMEUtil;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.TCPSession;

/**
 * ProtocolHandler for Imap.
 */
public class VirusImapHandler
    extends BufferingImapTokenStreamHandler {

    private final Logger m_logger =
        Logger.getLogger(VirusImapHandler.class);

    private final VirusNodeImpl m_virusImpl;
    private final VirusIMAPConfig m_config;
    private TempFileFactory m_fileFactory;

    protected VirusImapHandler(TCPSession session,
                               long maxClientWait,
                               long maxServerWait,
                               VirusNodeImpl node,
                               VirusIMAPConfig config) {

        super(maxClientWait, maxServerWait, Integer.MAX_VALUE);
        m_virusImpl = node;
        m_config = config;
        m_fileFactory = new TempFileFactory(
                                            UvmContextFactory.context().
                                            pipelineFoundry().getPipeline(session.id())
                                            );
    }


    @Override
    public HandleMailResult handleMessage(MIMEMessage msg,
                                          MessageInfo msgInfo) {
        m_logger.debug("[handleMessage]");

        m_virusImpl.incrementScanCount();

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Message has: " + candidateParts.length
                           + " scannable parts");
        }

        boolean foundVirus = false;
        //Kind-of a hack.  I need the scanResult
        //for the wrapped message.  If more than one was found,
        //we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        VirusMessageAction action = m_config.getMsgAction();
        if(action == null) {
            m_logger.error("VirusMessageAction null.  Assume REMOVE");
            action = VirusMessageAction.REMOVE;
        }

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                m_logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                m_logger.error("Scanning returned null (error already reported).  Skip " +
                               "part assuming local error");
                continue;
            }

            //Make log report
            VirusMailEvent event = new VirusMailEvent(
                                                      msgInfo,
                                                      scanResult,
                                                      scanResult.isClean()?VirusMessageAction.PASS:action,
                                                      m_virusImpl.getScanner().getVendorName());
            m_virusImpl.log(event);

            if(scanResult.isClean()) {
                m_logger.debug("Part clean");
            }
            else {
                if(!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                m_logger.debug("Part contained virus");
                if(action == VirusMessageAction.PASS) {
                    m_logger.debug("Passing infected part as-per policy");
                }
                else {
                    if(part == msg) {
                        m_logger.debug("Top-level message itself was infected.  \"Remove\"" +
                                       "virus by converting part to text");
                    }
                    else {
                        m_logger.debug("Removing infected part");
                    }
                    try {
                        MIMEUtil.removeChild(part);
                    }
                    catch(Exception ex) {
                        m_logger.error("Exception repoving child part", ex);
                    }
                }
            }
        }

        if(foundVirus) {
            if(action == VirusMessageAction.REMOVE) {
                m_logger.debug("REMOVE (wrap) message");
                MIMEMessage wrappedMsg = m_config.getMessageGenerator().wrap(msg, scanResultForWrap);
                m_virusImpl.incrementRemoveCount();
                return HandleMailResult.forReplaceMessage(wrappedMsg);
            }
            else {
                m_logger.debug("Passing infected message (as-per policy)");
		m_virusImpl.incrementPassedInfectedMessageCount();
            }
        }
        m_virusImpl.incrementPassCount();
        return HandleMailResult.forPassMessage();
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(MIMEPart part) {

        //Get the part as a file
        File f = null;
        try {
            f = part.getContentAsFile(m_fileFactory, true);
        }
        catch(Exception ex) {
            m_logger.error("Exception writing MIME part to file", ex);
            return null;
        }

        //Call VirusScanner
        try {

            VirusScannerResult result = m_virusImpl.getScanner().scanFile(f);
            if(result == null || result == VirusScannerResult.ERROR) {
                m_logger.error("Received an error scan report.  Assume local error" +
                               " and report file clean");
                //TODO bscott This is scary
                return null;
            }
            return result;
        }
        catch(Exception ex) {
            //TODO bscott I'd like to preserve this file and include it
            //     in some type of "report".
            m_logger.error("Exception scanning MIME part in file \"" +
                           f.getAbsolutePath() + "\"", ex);
            //No need to delete the file.  This will be handled by the MIMEPart itself
            //through its normal lifecycle
            return null;
        }
    }


}
