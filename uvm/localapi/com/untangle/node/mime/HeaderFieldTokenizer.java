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

package com.untangle.node.mime;

import static com.untangle.node.util.Ascii.*;
import static com.untangle.node.util.ASCIIUtil.*;

/**
 * Tokenizes MIME header field values
 */
public class HeaderFieldTokenizer {

    private static byte[] DEF_DELIMS;

    static {
        DEF_DELIMS = new byte[MIMEUtil.MIME_SPECIALS.length + 2];
        DEF_DELIMS[0] = HT_B;
        DEF_DELIMS[1] = SP_B;
        System.arraycopy(MIMEUtil.MIME_SPECIALS, 0, DEF_DELIMS, 2, MIMEUtil.MIME_SPECIALS.length);
    }

    public enum TokenType {
        ATOM,
        DELIM,
        OPEN_COMMENT,
        CLOSE_COMMENT,
        QTEXT
    };

    /**
     * Class reprsenting a returned token
     */
    public class Token {
        private final byte m_delim;
        private final StringBuilder m_sb;
        private final TokenType m_tokenType;

        private Token(TokenType type,
                      byte delim) {
            m_delim = delim;
            m_sb = null;
            m_tokenType = type;
        }
        private Token(TokenType type,
                      StringBuilder sb) {
            m_delim = 0;
            m_sb = sb;
            m_tokenType = type;
        }
        /**
         * Only applies for DELIM.  Otherwise, returns 0
         */
        public byte getDelim() {
            return m_delim;
        }
        /**
         * Get the text of this Token.  Only applies to "QTEXT" and "ATOM"
         */
        public StringBuilder getText() {
            return m_sb;
        }
        /**
         * Get the token type
         */
        public TokenType getType() {
            return m_tokenType;
        }
        public String toString() {
            return m_delim==0?
                getText().toString():new String(new byte[] {getDelim()});
        }
        /**
         * Returns true if this is a delim (not an ATOM or QTEXT)
         */
        public boolean isDelim() {
            return m_delim != 0;
        }

        /**
         * Append the value of this token (either a single char
         * or the return of {@link #getText
         */
        public void appendTo(StringBuilder sb) {
            if(getType() == TokenType.ATOM ||
               getType() == TokenType.QTEXT) {
                sb.append(toString());
            }
            else {
                sb.append((char) getDelim());
            }
        }
    }

    private int m_pos = 0;
    private final int m_len;
    private final byte[] m_data;
    private final byte[] m_delims;
    private int m_openCommentCount = 0;

    public HeaderFieldTokenizer(String str) {
        this(str.getBytes(), DEF_DELIMS);
    }
    public HeaderFieldTokenizer(byte[] bytes) {
        this(bytes, DEF_DELIMS);
    }
    public HeaderFieldTokenizer(byte[] bytes,
                                byte[] delims) {
        m_data = bytes;
        m_delims = delims;
        m_pos = 0;
        m_len = m_data.length;
    }

    /**
     * Get the original String being parsed.
     */
    public String getOriginal() {
        return new String(m_data);
    }

    /**
     * Get the open comment count.  <b>Warning - this count
     * drops to zero as the last closing ")" is returned</b>
     */
    public int openCommentCount() {
        return m_openCommentCount;
    }

    /**
     * Gets the current position.  Initialy, this
     * is zero.  As each token is returned,
     * this is the position of the next byte (i.e. one past the
     * end of the token just returned).
     */
    public int position() {
        return m_pos;
    }

    /**
     * Get the next token w/o comments
     */
    public Token nextTokenIgnoreComments() {
        Token ret = nextToken();
        while(ret != null &&
              (openCommentCount() > 0 ||
               ret.getDelim() == CLOSE_PAREN_B)) {
            ret = nextToken();
        }
        return ret;
    }

    /**
     * Get the next token
     */
    public Token nextTokenWithComments() {
        return nextToken();
    }

    private Token nextToken() {

        StringBuilder sb = null;

        while(m_pos < m_len) {
            //Check for pure delim
            if(isDelim(m_data[m_pos]) &&
               (m_data[m_pos] != QUOTE_B)) {

                if(m_data[m_pos] == OPEN_PAREN_B) {
                    if(sb==null) {
                        m_openCommentCount++;
                        return new Token(TokenType.OPEN_COMMENT, m_data[m_pos++]);
                    }
                    else {
                        return new Token(TokenType.ATOM, sb);
                    }
                }
                if(m_data[m_pos] == CLOSE_PAREN_B) {
                    if(sb==null) {
                        m_openCommentCount--;
                        return new Token(TokenType.OPEN_COMMENT, m_data[m_pos++]);
                    }
                    else {
                        return new Token(TokenType.ATOM, sb);
                    }
                }

                return sb==null?
                    new Token(TokenType.DELIM, m_data[m_pos++]):
                    new Token(TokenType.ATOM, sb);
            }

            if(sb == null) {
                sb = new StringBuilder();
            }

            //QText
            if(m_data[m_pos] == QUOTE_B) {
                //Drain quote
                m_pos++;
                while(m_pos < m_len) {
                    if(m_data[m_pos] == BACK_SLASH_B) {
                        //Check if ended in "\"
                        if(m_pos+1 >= m_len) {
                            sb.append((char) m_data[m_pos++]);
                            return new Token(TokenType.ATOM, sb);
                        }
                        if(m_data[m_pos+1] == QUOTE_B) {
                            m_pos++;
                            m_pos++;
                            sb.append(QUOTE);
                        }
                        else {
                            sb.append(m_data[m_pos++]);
                        }
                    }
                    else {
                        if(m_data[m_pos] == QUOTE_B) {
                            m_pos++;
                            return new Token(TokenType.QTEXT, sb);
                        }
                        sb.append((char) m_data[m_pos++]);
                    }
                }
                return new Token(TokenType.QTEXT, sb);
            }
            sb.append((char) m_data[m_pos++]);
        }
        return sb==null?null:new Token(TokenType.ATOM, sb);
    }

    private final boolean isDelim(final byte b) {
        for(byte d : m_delims) {
            if(b == d) {
                return true;
            }
        }
        return false;
    }

    /*

    public static void main(String[] args) {
    test("text/plain name=\"foo\"");
    test("text/plain name=\\\"foo\"");
    test("text/plain name=\"foo");
    test("text/plain (some comment\"with\" qhotes \"\") name=\"foo");

    test("text/plain name=eicar.com");
    test("text/plain name=\"\"eicar.com");
    test("text/plain name=.\"eicar.com\"");
    test("text/plain name=eicar .com");
    test("text/plain name=\"eicar.com");
    test("text/plain name==?us-ascii?Q?eicar.com?=");
    test("text/plain name==?us-ascii?Q?eicar?=.com");
    test("text/plain name==?us-ascii?Q?eicar?= =?us-ascii?Q?.com?=");
    test("text/plain name=\"eicar.=?us-ascii?Q?com?=\"");
    test("text/plain name=\"eicar.=?us-ascii?Q?com?=");
    test("text/plain name=eicar.=?us-ascii?Q?com?=");
    test("text/plain name=eicar.=?us-ascii?Q?co?=m");

    test("text/plain name==?us-ascii?b?eicar.com?=");
    test("text/plain name==?us-ascii?b?eicar?=.com");
    test("text/plain name==?us-ascii?b?eicar?= =?us-ascii?b?.com?=");
    test("text/plain name=\"eicar.=?us-ascii?b?com?=\"");
    test("text/plain name=\"eicar.=?us-ascii?b?com?=");
    test("text/plain name=eicar.=?us-ascii?b?com?=");
    test("text/plain name=eicar.=?us-ascii?b?co?=m");
    }


    private static void test(String str) {
    System.out.println("\n\n\n*************Testing: |" + str + "|");
    HeaderFieldTokenizer tokenizer = new HeaderFieldTokenizer(str);
    Token token = null;
    while((token = tokenizer.nextToken()) != null) {
    System.out.print(token.getType() + " |");
    if(token.getType() == TokenType.QTEXT ||
    token.getType() == TokenType.ATOM) {
    System.out.print(token.getText().toString());
    }
    else {
    System.out.print((char) token.getDelim());
    }
    System.out.println("|");

    }
    }
    */
}
