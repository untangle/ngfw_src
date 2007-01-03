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

package com.untangle.tran.mail.papi.pop;

import static com.untangle.tran.util.Ascii.*;
import static com.untangle.tran.util.Rfc822Util.*;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.tran.token.ParseException;
import com.untangle.tran.token.Token;
import com.untangle.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

/* We handle USER, APOP, and AUTH LOGIN but no other AUTH types.
 * (Other AUTH types, such as AUTH KERBEROS_V4 and AUTH CRAM-MD5,
 *  use encryption schemes that we do not handle.)
 *
 * RFC 1939:
 * USER name
 *    Arguments:
 *        a string identifying a mailbox (required), which is of
 *        significance ONLY to the server
 *    Restrictions:
 *        may only be given in the AUTHORIZATION state after the POP3
 *        greeting or after an unsuccessful USER or PASS command
 *    Discussion:
 *        To authenticate using the USER and PASS command
 *        combination, the client must first issue the USER
 *        command.  If the POP3 server responds with a positive
 *        status indicator ("+OK"), then the client may issue
 *        either the PASS command to complete the authentication,
 *        or the QUIT command to terminate the POP3 session.  If
 *        the POP3 server responds with a negative status indicator
 *        ("-ERR") to the USER command, then the client may either
 *        issue a new authentication command or may issue the QUIT
 *        command.
 *        The server may return a positive response even though no
 *        such mailbox exists.  The server may return a negative
 *        response if mailbox exists, but does not permit plaintext
 *        password authentication.
 *
 * APOP name digest
 *    Arguments:
 *        a string identifying a mailbox and a MD5 digest string
 *        (both required)
 *    Restrictions:
 *        may only be given in the AUTHORIZATION state after the POP3
 *        greeting or after an unsuccessful USER or PASS command
 *    Discussion:
 *        Normally, each POP3 session starts with a USER/PASS
 *        exchange.  This results in a server/user-id specific
 *        password being sent in the clear on the network.  For
 *        intermittent use of POP3, this may not introduce a sizable
 *        risk.  However, many POP3 client implementations connect to
 *        the POP3 server on a regular basis -- to check for new
 *        mail.  Further the interval of session initiation may be on
 *        the order of five minutes.  Hence, the risk of password
 *        capture is greatly enhanced.
 *        An alternate method of authentication is required which
 *        provides for both origin authentication and replay
 *        protection, but which does not involve sending a password
 *        in the clear over the network.  The APOP command provides
 *        this functionality.
 *        When the POP3 server receives the APOP command, it verifies
 *        the digest provided.  If the digest is correct, the POP3
 *        server issues a positive response, and the POP3 session
 *        enters the TRANSACTION state.  Otherwise, a negative
 *        response is issued and the POP3 session remains in the
 *        AUTHORIZATION state.
 *
 * RFC 1734:
 * AUTH mechanism
 *   Arguments:
 *       a string identifying an IMAP4 authentication mechanism,
 *       such as defined by [IMAP4-AUTH].  Any use of the string
 *       "imap" used in a server authentication identity in the
 *       definition of an authentication mechanism is replaced with
 *       the string "pop".
 *   Restrictions:
 *       may only be given in the AUTHORIZATION state
 *   Discussion:
 *       The AUTH command indicates an authentication mechanism to
 *       the server.  If the server supports the requested
 *       authentication mechanism, it performs an authentication
 *       protocol exchange to authenticate and identify the user.
 *       Optionally, it also negotiates a protection mechanism for
 *       subsequent protocol interactions.  If the requested
 *       authentication mechanism is not supported, the server
 *       should reject the AUTH command by sending a negative
 *       response.
 *       The authentication protocol exchange consists of a series
 *       of server challenges and client answers that are specific
 *       to the authentication mechanism.  A server challenge,
 *       otherwise known as a ready response, is a line consisting
 *       of a "+" character followed by a single space and a BASE64
 *       encoded string.  The client answer consists of a line
 *       containing a BASE64 encoded string.  If the client wishes
 *       to cancel an authentication exchange, it should issue a
 *       line with a single "*".  If the server receives such an
 *       answer, it must reject the AUTH command by sending a
 *       negative response.
 *       The server is not required to support any particular
 *       authentication mechanism, nor are authentication mechanisms
 *       required to support any protection mechanisms.  If an AUTH
 *       command fails with a negative response, the session remains
 *       in the AUTHORIZATION state and client may try another
 *       authentication mechanism by issuing another AUTH command,
 *       or may attempt to authenticate by using the USER/PASS or
 *       APOP commands.  In other words, the client may request
 *       authentication types in decreasing order of preference,
 *       with the USER/PASS or APOP command as a last resort.
 *       Should the client successfully complete the authentication
 *       exchange, the POP3 server issues a positive response and
 *       the POP3 session enters the TRANSACTION state.
 */
public class PopCommand implements Token
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String LWSP = "\\p{Blank}"; /* linear-white-space */
    private final static String START = "^(" + LWSP + ")*";
    private final static String USER = START + "USER(" + LWSP + ")+";
    private final static String APOP = START + "APOP(" + LWSP + ")+";
    private final static String AUTHLOGIN = START + "AUTH(" + LWSP + ")+LOGIN";
    private final static String CRLF = "\r\n";
    private final static String STLS = START + "(STLS|STARTTLS)" + CRLF;
    private final static String RETR = START + "RETR(" + LWSP + ")+";
    private final static String TOP = START + "TOP(" + LWSP + ")+";
    private final static String PEOLINE = CRLF + "$"; /* protocol EOLINE */
    private final static String LWSPEOL = "(" + LWSP + "|" + PEOLINE + ")";
    private final static Pattern USERP = Pattern.compile(USER, Pattern.CASE_INSENSITIVE);
    private final static Pattern APOPP = Pattern.compile(APOP, Pattern.CASE_INSENSITIVE);
    private final static Pattern AUTHLOGINP = Pattern.compile(AUTHLOGIN, Pattern.CASE_INSENSITIVE);
    private final static Pattern STLSP = Pattern.compile(STLS, Pattern.CASE_INSENSITIVE);
    private final static Pattern RETRP = Pattern.compile(RETR, Pattern.CASE_INSENSITIVE);
    private final static Pattern TOPP = Pattern.compile(TOP, Pattern.CASE_INSENSITIVE);
    private final static Pattern LWSPEOLP = Pattern.compile(LWSPEOL);

    private final static String NO_USER = "unknown";

    private final static String EOLINE = CRLF; /* EOLINE */
    private final static String PASSWDOK = "^PASS .*?" + EOLINE;

    private final String command;
    private final String argument;
    private final String zUser;
    private final boolean hasSpace;
    private final boolean bIsAuthLogin;
    private final boolean bIsTLS;
    private final boolean bIsRETR;
    private final boolean bIsTOP;

    // constructors -----------------------------------------------------------

    private PopCommand(String command, String argument, String zUser, boolean hasSpace, boolean bIsAuthLogin, boolean bIsTLS, boolean bIsRETR, boolean bIsTOP)
    {
        this.command = command;
        this.argument = argument;
        this.zUser = zUser;
        this.hasSpace = hasSpace;
        this.bIsAuthLogin = bIsAuthLogin;
        this.bIsTLS = bIsTLS;
        this.bIsRETR = bIsRETR;
        this.bIsTOP = bIsTOP;
    }

    // static factories -------------------------------------------------------

    public static PopCommand parse(ByteBuffer buf) throws ParseException
    {
        ByteBuffer zDup = buf.duplicate();
        String zTmp = AsciiCharBuffer.wrap(zDup).toString();

        boolean bIsTLS = STLSP.matcher(zTmp).find();
        boolean bIsRETR = RETRP.matcher(zTmp).find();
        boolean bIsTOP = TOPP.matcher(zTmp).find();
        //logger.debug("command: " + zTmp + ", is TLS: " + bIsTLS + ", is RETR: " + bIsRETR + ", is TOP: " + bIsTOP);

        String cmd = consumeToken(zDup);
        if (0 == cmd.length()) {
            throw new ParseException("cannot identify command: " + AsciiCharBuffer.wrap(buf));
        }

        boolean space = eatSpace(zDup);

        String arg = consumeBuf(zDup); /* eat CRLF */

        return new PopCommand(cmd, 0 == arg.length() ? null : arg, null, space, false, bIsTLS, bIsRETR, bIsTOP);
    }

    public static PopCommand parseUser(ByteBuffer buf) throws ParseException
    {
        ByteBuffer zDup = buf.duplicate();
        String zTmp = AsciiCharBuffer.wrap(zDup).toString();

        Matcher zMatcher = STLSP.matcher(zTmp);
        boolean bIsTLS = zMatcher.find();
        //logger.debug("command: " + zTmp + ", is TLS: " + bIsTLS);

        String zUser;
        boolean bIsAuthLogin;

        if (null == (zUser = getUser(zTmp, USERP)) &&
            null == (zUser = getUser(zTmp, APOPP))) {
            zUser = null;

            zMatcher = AUTHLOGINP.matcher(zTmp);
            bIsAuthLogin = zMatcher.find();
        } else {
            bIsAuthLogin = false;
        }

        String cmd = consumeToken(zDup);
        if (0 == cmd.length()) {
            throw new ParseException("cannot identify command: " + AsciiCharBuffer.wrap(buf));
        }

        boolean space = eatSpace(zDup);
        String arg = consumeBuf(zDup); /* eat CRLF */
        return new PopCommand(cmd, 0 == arg.length() ? null : arg, zUser, space, bIsAuthLogin, bIsTLS, false, false);
    }

    // accessors --------------------------------------------------------------

    public String getCommand()
    {
        return command;
    }

    public String getArgument()
    {
        return argument;
    }

    public String getUser()
    {
        return (null == zUser) ? NO_USER : zUser;
    }

    public boolean isUser()
    {
        return (null == zUser) ? false : true;
    }

    public boolean isAuthLogin()
    {
        return bIsAuthLogin;
    }

    public boolean isTLS()
    {
        return bIsTLS;
    }

    public boolean isRETR()
    {
        return bIsRETR;
    }

    public boolean isTOP()
    {
        return bIsTOP;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        int iLen = command.length() + (null == argument ? 0 : argument.length() + 1) + 2;
        ByteBuffer zBuf = ByteBuffer.allocate(iLen);

        zBuf.put(command.getBytes());
        if (true == hasSpace) {
            zBuf.put((byte)SP); /* restore */
        }
        if (null != argument) {
            zBuf.put(argument.getBytes());
        }

        zBuf.flip();

        return zBuf;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        String zTmp = AsciiCharBuffer.wrap(getBytes()).toString();
        if (false == zTmp.matches(PASSWDOK))
        {
            return zTmp;
        }

        return "PASSWD";
    }

    /* consume rest of buffer (including any terminating CRLF) */
    private static String consumeBuf(ByteBuffer zBuf)
    {
        StringBuilder zSBuilder = new StringBuilder();
        while (true == zBuf.hasRemaining()) {
            zSBuilder.append((char) zBuf.get());
        }

        return zSBuilder.toString();
    }

    private static String getUser(String zCmd, Pattern zPattern)
    {
        Matcher zMatcher = zPattern.matcher(zCmd);
        if (false == zMatcher.find()) {
            return null;
        }

        int iStart = zMatcher.end();
        zMatcher = LWSPEOLP.matcher(zCmd);
        if (false == zMatcher.find(iStart)) {
            return null;
        }

        //String zUser = (String) zCmd.subSequence(iStart, zMatcher.start());
        //logger.debug("user name is: " + zUser);
        //return zUser;
        return (String) zCmd.subSequence(iStart, zMatcher.start());
    }

    public int getEstimatedSize()
    {
        return command.length() + (null == argument ? 0 : argument.length() + 1) + 2;
    }
}
