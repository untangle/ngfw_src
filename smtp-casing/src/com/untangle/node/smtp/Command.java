/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/Command.java $
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.smtp;

//import static com.untangle.node.util.Rfc822Util.*;
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
