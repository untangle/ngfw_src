/**
 * $Id$
 */
package com.untangle.app.smtp;

import static com.untangle.uvm.util.Ascii.CRLF_BA;
import static com.untangle.uvm.util.Ascii.DASH;
import static com.untangle.uvm.util.Ascii.SP;

import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.Token;

/**
 * Class to encapsulate an SMTP response. The {@link #getArgs arguments} are any Strings after the "NNN" on each
 * response line.
 */
public class Response implements Token
{

    private static final String[] BLANK_ARGS = new String[0];

    private int m_code;
    private String[] m_args;

    public Response(int code) {
        this(code, "");
    }

    public Response(int code, String arg) {
        m_code = code;
        if (arg == null) {
            arg = "";
        }
        m_args = new String[] { arg };
    }

    public Response(int code, String[] args) {
        m_code = code;
        m_args = (args == null ? BLANK_ARGS : args);
    }

    /**
     * Get the numerical code associated with this response
     */
    public int getCode()
    {
        return m_code;
    }

    /**
     * Get the arguments for the response. The array may be greater than 1 element if the response was multi-line.
     * 
     * @return the argument (never null, although each element may be null or blank).
     */
    public String[] getArgs()
    {
        return m_args;
    }

    public ByteBuffer getBytes()
    {
        int len = 0;
        byte[] rcBytes = Integer.toString(m_code).getBytes();
        
        for (String arg : m_args) {
            len += rcBytes.length;// NNN
            len += 1;// SP or DASH
            len += (arg == null ? 0 : arg.getBytes().length);
            len += 2;// CRLF
        }
        ByteBuffer ret = ByteBuffer.allocate(len);

        try {
            for (int i = 0; i < m_args.length; i++) {
                ret.put(rcBytes);
                ret.put((i == m_args.length - 1 ? (byte) SP : (byte) DASH));
                if (m_args[i] != null) {
                    ret.put(m_args[i].getBytes());
                }
                ret.put(CRLF_BA);
            }
        } catch (RuntimeException e) {
            String str = "" + m_code + ":";
            for (String arg : m_args) {
                if (arg != null)
                    str += "[" + arg + "] ";
            }
            throw new RuntimeException("Exception while processing response: " + str, e);
        }

        ret.flip();

        return ret;
    }

    /**
     * For debug logging.
     */
    public String toDebugString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(m_code));
        if (getArgs() != null) {
            sb.append(" (");
            if (getArgs().length == 1) {
                sb.append("\"");
                sb.append(getArgs()[0]);
                sb.append("\"");
            } else {
                sb.append("multiline response");
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
