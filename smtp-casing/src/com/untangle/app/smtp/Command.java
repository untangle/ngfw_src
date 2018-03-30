/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.AsciiUtil.bbToString;
import static com.untangle.uvm.util.Ascii.CR;
import static com.untangle.uvm.util.Ascii.LF;
import static com.untangle.uvm.util.Ascii.SP;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * Class reprsenting an SMTP Command issued by a client.
 */
public class Command implements Token
{
    private final CommandType type;
    private final String cmdStr;
    private String argStr;

    /**
     * Initialize command.
     * 
     * @param  type   CommandType of command.
     * @param  cmdStr String of command.
     * @param  argStr Argument string for command.
     * @return        Instance of Command.
     */
    public Command(CommandType type, String cmdStr, String argStr)
    {
        this.type = type;
        this.cmdStr = cmdStr;
        this.argStr = argStr;
    }

    /**
     * Initialize command using type string and argments to null.
     * 
     * @param  type   CommandType of command.
     * @return        Instance of Command.
     */
    public Command(CommandType type)
    {
        this.type = type;
        this.cmdStr = type.toString();
        this.argStr = null;
    }

    /**
     * Get the string of the command (i.e. "HELO", "RCPT").
     *
     * @return String of command.
     */
    public String getCmdString()
    {
        return this.cmdStr;
    }

    /**
     * Set command argument.
     *
     * @param argStr String of argument to command.
     */
    protected void setArgStr(String argStr)
    {
        this.argStr = argStr;
    }

    /**
     * Get the argument to a command. For example, in "MAIL FROM:<>" "FROM:<>" is the argument. This may be null.
     
     * @return String of argument.
     */
    public String getArgString()
    {
        return this.argStr;
    }

    /**
     * Get the type of the command. Be warned - the type may be "UNKNOWN"
     *
     * @return CommandType of type.
     */
    public CommandType getType()
    {
        return this.type;
    }

    /**
     * Convert the command back to a valid line (with terminator). This is done by appending the type with the results
     * of {@link #getArgString getArgString()}.
     *
     * @return ByteBuffer of bytes.
     */
    public ByteBuffer getBytes()
    {
        // Do a bit of fixup on the string
        String tmpCmdStr = this.type.toString();
        if (getType() == CommandType.UNKNOWN) {
            tmpCmdStr = this.cmdStr;
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

        byte[] cmdBytes = tmpCmdStr.getBytes();
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
     *
     * @return String of buffer.
     */
    public String toDebugString()
    {
        ByteBuffer buf = getBytes();
        buf.limit(buf.limit() - 2);// Remove CRLF for debugging
        return bbToString(buf);
    }

    /**
     * Buffer as a String.
     *
     * @return String of buffer.
     */
    public String toString()
    {
        return toDebugString();
    }
}
