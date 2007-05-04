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

package com.untangle.tran.virus;

import java.io.*;
import java.util.*;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tapi.*;
import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.smtp.*;
import com.untangle.tran.mail.papi.smtp.sapi.*;
import com.untangle.tran.mime.*;
import com.untangle.tran.util.*;
import org.apache.log4j.Logger;


/**
 * Protocol Handler which is called-back as messages
 * are found which are candidates for Virus Scanning.
 */
public class SmtpSessionHandler
    extends BufferingSessionHandler {

    private final Logger m_logger = Logger.getLogger(SmtpSessionHandler.class);
    private final Pipeline m_pipeline;
    private final TempFileFactory m_fileFactory;

    private final VirusTransformImpl m_virusImpl;
    private final VirusSMTPConfig m_config;


    public SmtpSessionHandler(TCPSession session,
                              long maxClientWait,
                              long maxSvrWait,
                              VirusTransformImpl impl,
                              VirusSMTPConfig config) {

        super(Integer.MAX_VALUE, maxClientWait, maxSvrWait, true);

        m_virusImpl = impl;
        m_config = config;
        m_pipeline = MvvmContextFactory.context().
            pipelineFoundry().getPipeline(session.id());
        m_fileFactory = new TempFileFactory(m_pipeline);
    }

    @Override
    public BPMEvaluationResult blockPassOrModify(MIMEMessage msg,
                                                 SmtpTransaction tx,
                                                 MessageInfo msgInfo) {
        m_logger.debug("[handleMessageCanBlock] called");
        m_virusImpl.incrementScanCounter();

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Message has: " + candidateParts.length
                           + " scannable parts");
        }

        boolean foundVirus = false;
        //Kind-of a hack.  I need the scanResult
        //for the wrapped message.  If nore than one was found,
        //we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        SMTPVirusMessageAction action = m_config.getMsgAction();
        if(action == null) {
            m_logger.error("SMTPVirusMessageAction null.  Assume REMOVE");
            action = SMTPVirusMessageAction.REMOVE;
        }

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                m_logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                m_logger.warn("Scanning returned null (error already reported).  Skip " +
                              "part assuming local error");
                continue;
            }

            //Make log report
            VirusSmtpEvent event = new VirusSmtpEvent(
                                                      msgInfo,
                                                      scanResult,
                                                      scanResult.isClean()?SMTPVirusMessageAction.PASS:action,
                                                      scanResult.isClean()?SMTPNotifyAction.NEITHER:m_config.getNotifyAction(),
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
                if(action == SMTPVirusMessageAction.PASS) {
                    m_logger.debug("Passing infected part as-per policy");
                }
                else if(action == SMTPVirusMessageAction.BLOCK) {
                    m_logger.debug("Stop scanning remaining parts, as the policy is to block");
                    break;
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
            //Perform notification (if we should)
            if(m_config.getNotificationMessageGenerator().sendNotification(
                                                                           MvvmContextFactory.context().mailSender(),
                                                                           m_config.getNotifyAction(),
                                                                           msg,
                                                                           tx,
                                                                           tx, scanResultForWrap)) {
                m_logger.debug("Notification handled without error");
            }
            else {
                m_logger.warn("Error sending notification");
            }

            if(action == SMTPVirusMessageAction.BLOCK) {
                m_logger.debug("Returning BLOCK as-per policy");
                m_virusImpl.incrementBlockCounter();
                return BLOCK_MESSAGE;
            }
            else if(action == SMTPVirusMessageAction.REMOVE) {
                m_logger.debug("REMOVE (wrap) message");
                MIMEMessage wrappedMsg = m_config.getMessageGenerator().wrap(msg, tx, scanResultForWrap);
                m_virusImpl.incrementRemoveCounter();
                return new BPMEvaluationResult(wrappedMsg);
            }
            else {
                m_logger.debug("Passing infected message (as-per policy)");
            }
        }
        m_virusImpl.incrementPassCounter();
        return PASS_MESSAGE;
    }


    @Override
    public BlockOrPassResult blockOrPass(MIMEMessage msg,
                                         SmtpTransaction tx,
                                         MessageInfo msgInfo) {
        m_logger.debug("[handleMessageCanNotBlock]");
        m_virusImpl.incrementScanCounter();

        //TODO bscott There has to be a way to share more code
        //     with the "blockPassOrModify" method

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("Message has: " + candidateParts.length
                           + " scannable parts");
        }
        SMTPVirusMessageAction action = m_config.getMsgAction();

        //Check for the impossible-to-satisfy action of "REMOVE"
        if(action == SMTPVirusMessageAction.REMOVE) {
            //Change action now, as it'll make the event logs
            //more accurate
            m_logger.debug("Implicitly converting policy from \"REMOVE\"" +
                           " to \"BLOCK\" as we have already begun to trickle");
            action = SMTPVirusMessageAction.BLOCK;
        }

        boolean foundVirus = false;
        VirusScannerResult scanResultForWrap = null;

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                m_logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                m_logger.warn("Scanning returned null (error already reported).  Skip " +
                              "part assuming local error");
                continue;
            }

            if(scanResult.isClean()) {
                m_logger.debug("Part clean");
            }
            else {
                m_logger.debug("Part contained virus");
                if(!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                //Make log report
                VirusSmtpEvent event = new VirusSmtpEvent(
                                                          msgInfo,
                                                          scanResult,
                                                          scanResult.isClean()?SMTPVirusMessageAction.PASS:action,
                                                          scanResult.isClean()?SMTPNotifyAction.NEITHER:m_config.getNotifyAction(),
                                                          m_virusImpl.getScanner().getVendorName());
                m_virusImpl.log(event);

                if(action == SMTPVirusMessageAction.PASS) {
                    m_logger.debug("Passing infected part as-per policy");
                }
                else {
                    m_logger.debug("Scop scanning any remaining parts as we will block message");
                    break;
                }
            }
        }
        if(foundVirus) {
            //Make notification
            if(m_config.getNotificationMessageGenerator().sendNotification(
                                                                           MvvmContextFactory.context().mailSender(),
                                                                           m_config.getNotifyAction(),
                                                                           msg,
                                                                           tx,
                                                                           tx, scanResultForWrap)) {
                m_logger.debug("Notification handled without error");
            }
            else {
                m_logger.error("Error sending notification");
            }
            if(action == SMTPVirusMessageAction.BLOCK) {
                m_logger.debug("Blocking mail as-per policy");
                m_virusImpl.incrementBlockCounter();
                return BlockOrPassResult.BLOCK;
            }
        }
        m_virusImpl.incrementPassCounter();
        return BlockOrPassResult.PASS;
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(MIMEPart part) {

        /*
        //Fake scanning (for test)
        if(System.currentTimeMillis() > 0) {
        if(part.isAttachment()) {
        String fileName = part.getAttachmentName();
        m_logger.debug("Part filename \"" + fileName + "\"");
        if(fileName != null && fileName.startsWith("virus")) {
        m_logger.debug("Pretend part has virus");
        return new VirusScannerResult(false, "MyFakeVirus", false);
        }
        else {
        m_logger.debug("Pretend part does not have virus");
        }
        }
        else {
        m_logger.debug("Pretend part does not have virus");
        }
        return new VirusScannerResult(true, null, false);
        }
        */
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
                m_logger.warn("Received an error scan report.  Assume local error" +
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
