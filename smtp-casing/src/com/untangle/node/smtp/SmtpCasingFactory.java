/**
 * $Id$
 */
package com.untangle.node.smtp;

import com.untangle.node.token.Casing;
import com.untangle.node.token.CasingFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

public class SmtpCasingFactory implements CasingFactory
{
    private static final SmtpCasingFactory SMTP_CASING_FACTORY = new SmtpCasingFactory();

    private SmtpCasingFactory() {
    }

    public static SmtpCasingFactory factory()
    {
        return SMTP_CASING_FACTORY;
    }

    public Casing casing(NodeTCPSession session, boolean clientSide)
    {
        return new SmtpCasing(session, clientSide);
    }
}
