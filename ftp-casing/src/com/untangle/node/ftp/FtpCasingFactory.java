/**
 * $Id$
 */
package com.untangle.node.ftp;

import com.untangle.node.token.Casing;
import com.untangle.node.token.CasingFactory;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * FTP Casing factory.
 */
class FtpCasingFactory implements CasingFactory
{
    private static final Object LOCK = new Object();

    private static FtpCasingFactory FTP_CASING_FACTORY;

    private FtpCasingFactory() { }

    static FtpCasingFactory factory()
    {
        synchronized (LOCK) {
            if (null == FTP_CASING_FACTORY) {
                FTP_CASING_FACTORY = new FtpCasingFactory();
            }
        }

        return FTP_CASING_FACTORY;
    }

    public Casing casing(NodeTCPSession session, boolean inside)
    {
        return new FtpCasing(session, inside);
    }
}
