/**
 * $Id$
 */
package com.untangle.node.phish;

import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.safelist.SafelistNodeView;
import com.untangle.node.smtp.WrappedMessageGenerator;
import com.untangle.node.spam.SpamReport;
import com.untangle.node.spam.SpamSmtpConfig;

/**
 * Protocol Handler which is called-back as scanable messages
 * are encountered.
 */
public class PhishSmtpHandler extends com.untangle.node.spam.SpamSmtpHandler
{
    private static final String MOD_SUB_TEMPLATE = "[PHISH] $MIMEMessage:SUBJECT$";
    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Phish Blocker to be PHISH (a\r\n" +
        "fraudulent email intended to steal information).  The kind of PHISH that was\r\n" +
        "found was $SPAMReport:FULL$";

    private static WrappedMessageGenerator msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);
    
    protected PhishSmtpHandler( NodeTCPSession session, long maxClientWait, long maxSvrWait,
                                PhishNode impl, SpamSmtpConfig config, QuarantineNodeView quarantine, SafelistNodeView safelist)
    {
        super(session, maxClientWait, maxSvrWait, impl, config, quarantine, safelist);
        PhishSmtpHandler.msgGenerator = new WrappedMessageGenerator(MOD_SUB_TEMPLATE,MOD_BODY_TEMPLATE);
    }

    @Override
    protected String getQuarantineCategory()
    {
        return "PHISH";
    }

    @Override
    protected String getQuarantineDetail(SpamReport report)
    {
        return "PHISH";
    }

    @Override
    protected WrappedMessageGenerator getMsgGenerator()
    {
        return PhishSmtpHandler.msgGenerator;
    }

}
