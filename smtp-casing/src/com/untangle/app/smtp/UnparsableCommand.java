/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.AsciiUtil.bbToString;

import java.nio.ByteBuffer;

/**
 * Class reprsenting an unparsable line reveived when a Command was expected.
 */
public class UnparsableCommand extends Command
{

    private ByteBuffer m_unparsedLine;

    public UnparsableCommand(ByteBuffer badLine) {
        super(CommandType.UNKNOWN);
        m_unparsedLine = badLine;
    }

    @Override
    public String getArgString()
    {
        return bbToString(m_unparsedLine);
    }

    @Override
    public ByteBuffer getBytes()
    {
        return m_unparsedLine.duplicate();
    }
}
