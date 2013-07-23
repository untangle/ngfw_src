/**
 * $Id$
 */
package com.untangle.node.ftp;

import com.untangle.node.token.Casing;
import com.untangle.node.token.Parser;
import com.untangle.node.token.Unparser;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * FTP casing.
 */
class FtpCasing implements Casing
{
    private final Parser parser;
    private final FtpUnparser unparser;

    // constructors -----------------------------------------------------------

    FtpCasing(NodeTCPSession session, boolean clientSide)
    {
        parser = clientSide ? new FtpClientParser(session) : new FtpServerParser(session);
        unparser = new FtpUnparser(session, clientSide);
    }

    // Casing methods ---------------------------------------------------------

    public Parser parser()
    {
        return parser;
    }

    public Unparser unparser()
    {
        return unparser;
    }
}
