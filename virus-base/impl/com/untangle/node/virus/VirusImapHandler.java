/**
 * $Id$
 */
package com.untangle.node.virus;

import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.MessageInfo;
import com.untangle.node.mail.papi.imap.BufferingImapTokenStreamHandler;
import com.untangle.node.mail.papi.WrappedMessageGenerator;
import com.untangle.node.mime.MIMEMessage;
import com.untangle.node.mime.MIMEPart;
import com.untangle.node.mime.MIMEUtil;
import com.untangle.node.util.TempFileFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * ProtocolHandler for Imap.
 */
public class VirusImapHandler extends BufferingImapTokenStreamHandler
{
    private static final String MOD_SUB_TEMPLATE =
        "[VIRUS] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Virus Blocker.\r\n";

    private final Logger logger = Logger.getLogger(VirusImapHandler.class);

    private final VirusNodeImpl virusImpl;
    private TempFileFactory fileFactory;

    private final WrappedMessageGenerator generator;
    
    protected VirusImapHandler(NodeTCPSession session, long maxClientWait, long maxServerWait, VirusNodeImpl node)
    {
        super(maxClientWait, maxServerWait, Integer.MAX_VALUE);
        this.virusImpl = node;
        this.fileFactory = new TempFileFactory( UvmContextFactory.context().pipelineFoundry().getPipeline(session.id()) );
        this.generator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE, MOD_BODY_TEMPLATE);
    }


    @Override
    public HandleMailResult handleMessage(MIMEMessage msg, MessageInfo msgInfo)
    {
        this.logger.debug("[handleMessage]");

        this.virusImpl.incrementScanCount();

        MIMEPart[] candidateParts = MIMEUtil.getCandidateParts(msg);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Message has: " + candidateParts.length
                              + " scannable parts");
        }

        boolean foundVirus = false;
        //Kind-of a hack.  I need the scanResult
        //for the wrapped message.  If more than one was found,
        //we'll just use the first
        VirusScannerResult scanResultForWrap = null;

        String action = this.virusImpl.getSettings().getImapAction();
        if(action == null) {
            this.logger.error("VirusMessageAction null.  Assume REMOVE");
            action = "remove";
        }

        for(MIMEPart part : candidateParts) {
            if(!MIMEUtil.shouldScan(part)) {
                this.logger.debug("Skipping part which does not need to be scanned");
                continue;
            }
            VirusScannerResult scanResult = scanPart(part);

            if(scanResult == null) {
                this.logger.error("Scanning returned null (error already reported).  Skip " +
                                  "part assuming local error");
                continue;
            }

            //Make log report
            VirusMailEvent event = new VirusMailEvent( msgInfo, scanResult, scanResult.isClean()?"pass":action, this.virusImpl.getScanner().getVendorName());
            this.virusImpl.logEvent(event);

            if(scanResult.isClean()) {
                this.logger.debug("Part clean");
            }
            else {
                if(!foundVirus) {
                    scanResultForWrap = scanResult;
                }
                foundVirus = true;

                this.logger.debug("Part contained virus");
                if(action.equals("pass")) {
                    this.logger.debug("Passing infected part as-per policy");
                }
                else {
                    if(part == msg) {
                        this.logger.debug("Top-level message itself was infected.  \"Remove\"" +
                                          "virus by converting part to text");
                    }
                    else {
                        this.logger.debug("Removing infected part");
                    }
                    try {
                        MIMEUtil.removeChild(part);
                    }
                    catch(Exception ex) {
                        this.logger.error("Exception repoving child part", ex);
                    }
                }
            }
        }

        if(foundVirus) {
            if(action.equals("remove")) {
                this.logger.debug("REMOVE (wrap) message");
                MIMEMessage wrappedMsg = this.generator.wrap(msg, scanResultForWrap);
                this.virusImpl.incrementRemoveCount();
                return HandleMailResult.forReplaceMessage(wrappedMsg);
            }
            else {
                this.logger.debug("Passing infected message (as-per policy)");
                this.virusImpl.incrementPassedInfectedMessageCount();
            }
        }
        this.virusImpl.incrementPassCount();
        return HandleMailResult.forPassMessage();
    }

    /**
     * Returns null if there was an error.
     */
    private VirusScannerResult scanPart(MIMEPart part)
    {
        //Get the part as a file
        File f = null;
        try {
            f = part.getContentAsFile(this.fileFactory, true);
        }
        catch(Exception ex) {
            this.logger.error("Exception writing MIME part to file", ex);
            return null;
        }

        //Call VirusScanner
        try {

            VirusScannerResult result = this.virusImpl.getScanner().scanFile(f);
            if(result == null || result == VirusScannerResult.ERROR) {
                this.logger.error("Received an error scan report.  Assume local error" + " and report file clean");
                //TODO bscott This is scary
                return null;
            }
            return result;
        }
        catch(Exception ex) {
            //TODO bscott I'd like to preserve this file and include it
            //     in some type of "report".
            this.logger.error("Exception scanning MIME part in file \"" + f.getAbsolutePath() + "\"", ex);
            //No need to delete the file.  This will be handled by the MIMEPart itself
            //through its normal lifecycle
            return null;
        }
    }


}
