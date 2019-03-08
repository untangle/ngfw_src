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

    /**
     * Initialize instance of UnparsableCommand.
     * @param  badLine ByteBuffer of unparsable line.
     * @return         Instance of UnparsableCommand.
     */
    public UnparsableCommand(ByteBuffer badLine) {
        super(CommandType.UNKNOWN);
        m_unparsedLine = badLine;
    }

    /**
     * Return argument string.
     * @return String of unparsed line.
     */
    @Override
    public String getArgString()
    {
        return bbToString(m_unparsedLine);
    }

    /**
     * Return copy of argument string.
     * @return Copy of unparsed line.
     */
    @Override
    public ByteBuffer getBytes()
    {
        return m_unparsedLine.duplicate();
    }
}
