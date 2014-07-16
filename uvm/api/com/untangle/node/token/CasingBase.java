/**
d
 */
package com.untangle.node.token;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.NodeSession;

/**
 * Adapts a Token session's underlying byte-stream a <code>Casing</code>.
 */
public class CasingBase extends AbstractEventHandler
{
    protected final Logger logger = Logger.getLogger(CasingBase.class);

    protected final Parser parser;
    protected final Unparser unparser;
    protected final boolean clientSide;
    protected volatile boolean releaseParseExceptions;

    public CasingBase( Node node, Parser parser, Unparser unparser, boolean clientSide, boolean releaseParseExceptions )
    {
        super(node);
        this.parser = parser;
        this.unparser = unparser;
        this.clientSide = clientSide;
        this.releaseParseExceptions = releaseParseExceptions;
    }

    public boolean getReleaseParseExceptions()
    {
        return releaseParseExceptions;
    }

    public void setReleaseParseExceptions(boolean releaseParseExceptions)
    {
        this.releaseParseExceptions = releaseParseExceptions;
    }
}
