/*
 * $HeadURL$
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
package com.untangle.node.mail.papi.smtp;

import static com.untangle.node.util.Ascii.CRLF_BA;
import static com.untangle.node.util.Ascii.DASH;
import static com.untangle.node.util.Ascii.SP;

import java.nio.ByteBuffer;

import com.untangle.node.token.Token;


/**
 * Class to encapsulate an SMTP response.
 * The {@link #getArgs arguments} are any Strings
 * after the "NNN" on each response line.
 */
public class Response
    implements Token {

    private static final String[] BLANK_ARGS = new String[0];

    private int m_code;
    private String[] m_args;

    public Response(int code) {
        this(code, "");
    }
    public Response(int code, String arg) {
        m_code = code;
        if(arg == null) {
            arg = "";
        }
        m_args = new String[] {arg};
    }
    public Response(int code, String[] args) {
        m_code = code;
        m_args = (args==null?
                  BLANK_ARGS:args);
    }

    /**
     * Get the numerical code associated with this response
     */
    public int getCode() {
        return m_code;
    }

    /**
     * Get the arguments for the response.  The array may
     * be greater than 1 element if the response was multi-line.
     *
     * @return the argument (never null, although each element
     *         may be null or blank).
     */
    public String[] getArgs() {
        return m_args;
    }


    public ByteBuffer getBytes() {

        int len = 0;
        for(String arg : m_args) {
            len+=3;//NNN
            len+=1;//SP or DASH
            len+=(arg == null?0:arg.length());
            len+=2;//CRLF
        }
        ByteBuffer ret = ByteBuffer.allocate(len);

        byte[] rcBytes = Integer.toString(m_code).getBytes();//Hack

        for(int i = 0; i<m_args.length; i++) {
            ret.put(rcBytes);
            ret.put((i == m_args.length-1?(byte)SP:(byte) DASH));
            if(m_args[i] != null) {
                ret.put(m_args[i].getBytes());
            }
            ret.put(CRLF_BA);
        }


        ret.flip();

        return ret;
    }

    /**
     * For debug logging.
     */
    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Integer.toString(m_code));
        if(getArgs() != null) {
            sb.append(" (");
            if(getArgs().length == 1) {
                sb.append("\"");
                sb.append(getArgs()[0]);
                sb.append("\"");
            }
            else {
                sb.append("multiline response");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public int getEstimatedSize()
    {
        int len = 0;
        for(String arg : m_args) {
            len+=3;//NNN
            len+=1;//SP or DASH
            len+=(arg == null?0:arg.length());
            len+=2;//CRLF
        }

        return len;
    }
}
