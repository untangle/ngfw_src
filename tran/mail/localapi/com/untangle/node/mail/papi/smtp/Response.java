/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.mail.papi.smtp;

import static com.untangle.tran.util.BufferUtil.*;
import static com.untangle.tran.util.ASCIIUtil.*;
import static com.untangle.tran.util.Ascii.*;

import java.nio.ByteBuffer;
import java.util.*;

import com.untangle.tran.token.Token;


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
