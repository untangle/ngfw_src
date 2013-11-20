/**
 * $Id$
 */
package com.untangle.node.smtp;

import static com.untangle.node.util.ASCIIUtil.bbToString;
import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.LF;
import static com.untangle.node.util.Ascii.SP;

import java.nio.ByteBuffer;

import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;

/**
 * Class reprsenting an SMTP Command issued by a client.
 */
public class Command implements Token
{

    private final CommandType m_type;
    private final String m_cmdStr;
    private String m_argStr;

    public Command(CommandType type, String cmdStr, String argStr) throws ParseException {
        m_type = type;
        m_cmdStr = cmdStr;
        m_argStr = argStr;
    }

    public Command(CommandType type) {
        m_type = type;
        m_cmdStr = type.toString();
        m_argStr = null;
    }

    /**
     * Get the string of the command (i.e. "HELO", "RCPT").
     */
    public String getCmdString()
    {
        return m_cmdStr;
    }

    protected void setArgStr(String argStr)
    {
        m_argStr = argStr;
    }

    /**
     * Get the argument to a command. For example, in "MAIL FROM:<>" "FROM:<>" is the argument. This may be null.
     */
    public String getArgString()
    {
        return m_argStr;
    }

    /**
     * Get the type of the command. Be warned - the type may be "UNKNOWN"
     */
    public CommandType getType()
    {
        return m_type;
    }

    /**
     * Convert the command back to a valid line (with terminator). This is done by appending the type with the results
     * of {@link #getArgString getArgString()}.
     */
    public ByteBuffer getBytes()
    {
        // Do a bit of fixup on the string
        String cmdStr = m_type.toString();
        if (getType() == CommandType.UNKNOWN) {
            cmdStr = m_cmdStr;
        }

        String argStr = getArgString();
        boolean arg = false;
        if (argStr != null) {
            argStr = argStr.trim();
            arg = true;
        } else {
            argStr = "";
            arg = false;
        }

        byte[] cmdBytes = cmdStr.getBytes();
        byte[] argBytes = argStr.getBytes();

        int size = cmdBytes.length + (arg ? argBytes.length + 1 : 0) + 3;
        ByteBuffer buf = ByteBuffer.allocate(size);

        buf.put(cmdBytes);
        if (arg) {
            buf.put((byte) SP);
            buf.put(argBytes);
        }
        buf.put((byte) CR);
        buf.put((byte) LF);

        buf.flip();

        return buf;
    }

    /**
     * For debug logging
     */
    public String toDebugString()
    {
        ByteBuffer buf = getBytes();
        buf.limit(buf.limit() - 2);// Remove CRLF for debugging
        return bbToString(buf);
    }

    public String toString()
    {
        return toDebugString();
    }

    public int getEstimatedSize()
    {
        String cmdStr = m_type.toString();
        if (getType() == CommandType.UNKNOWN) {
            cmdStr = m_cmdStr;
        }

        String argStr = getArgString();

        return cmdStr.length() + (argStr == null ? (0) : (argStr.length() + 1)) + 3;
    }
}
