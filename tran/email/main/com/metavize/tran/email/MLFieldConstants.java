/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import java.nio.*;
import java.util.*;
import java.util.regex.*;

/* ARPA Internet text message format - RFC 822
 * MIME Internet message body format - RFC 2045
 */
public class MLFieldConstants
{
    /* constants */
    /* mail header fields - ignore other fields */
    private final static String RELAY = "^RECEIVED:";
    private final static String FROM = "^FROM:";
    private final static String HSENDER = "^SENDER:";
    private final static String DATE = "^DATE:";
    private final static String TOLIST = "^TO:";
    private final static String CCLIST = "^CC:";
    private final static String BCCLIST = "^BCC:";
    private final static String ORIGINATOR = "^REPLY-TO:";
    private final static String SUBJECT = "^SUBJECT:";
    private final static String CONTENTTYPE = "^CONTENT-TYPE:";
    private final static String XSPAMFLAG = "^X-SPAM-FLAG:";
    private final static String XSPAMSTATUS = "^X-SPAM-STATUS:";
    private final static String XVIRUSSTATUS = "^X-VIRUS-STATUS:";
    private final static String XVIRUSREPORT = "^X-VIRUS-REPORT:";

    private final static String MIMECONTENTTYPE = "^CONTENT-TYPE:";
    private final static String MIMECONTENTDISPOSITION = "^CONTENT-DISPOSITION:";
    private final static String MIMECONTENTENCODE = "^CONTENT-TRANSFER-ENCODING:";
    public final static Pattern RELAYP = Pattern.compile(RELAY, Pattern.CASE_INSENSITIVE);
    public final static Pattern FROMP = Pattern.compile(FROM, Pattern.CASE_INSENSITIVE);
    public final static Pattern HSENDERP = Pattern.compile(HSENDER, Pattern.CASE_INSENSITIVE);
    public final static Pattern DATEP = Pattern.compile(DATE, Pattern.CASE_INSENSITIVE);
    public final static Pattern TOLISTP = Pattern.compile(TOLIST, Pattern.CASE_INSENSITIVE);
    public final static Pattern CCLISTP = Pattern.compile(CCLIST, Pattern.CASE_INSENSITIVE);
    public final static Pattern BCCLISTP = Pattern.compile(BCCLIST, Pattern.CASE_INSENSITIVE);
    public final static Pattern ORIGINATORP = Pattern.compile(ORIGINATOR, Pattern.CASE_INSENSITIVE);
    public final static Pattern SUBJECTP = Pattern.compile(SUBJECT, Pattern.CASE_INSENSITIVE);
    public final static Pattern CONTENTTYPEP = Pattern.compile(CONTENTTYPE, Pattern.CASE_INSENSITIVE);
    public final static Pattern XSPAMFLAGP = Pattern.compile(XSPAMFLAG, Pattern.CASE_INSENSITIVE);
    public final static Pattern XSPAMSTATUSP = Pattern.compile(XSPAMSTATUS, Pattern.CASE_INSENSITIVE);
    public final static Pattern XVIRUSSTATUSP = Pattern.compile(XVIRUSSTATUS, Pattern.CASE_INSENSITIVE);
    public final static Pattern XVIRUSREPORTP = Pattern.compile(XVIRUSREPORT, Pattern.CASE_INSENSITIVE);

    public final static Pattern MIMECONTENTTYPEP = Pattern.compile(MIMECONTENTTYPE, Pattern.CASE_INSENSITIVE);
    public final static Pattern MIMECONTENTDISPOSITIONP = Pattern.compile(MIMECONTENTDISPOSITION, Pattern.CASE_INSENSITIVE);
    public final static Pattern MIMECONTENTENCODEP = Pattern.compile(MIMECONTENTENCODE, Pattern.CASE_INSENSITIVE);

    private final static String XSPAMFLAGYES = XSPAMFLAG + "(" + Constants.LWSP + ")*?YES";
    private final static String XSPAMSTATUSYES = XSPAMSTATUS + "(" + Constants.LWSP + ")*?YES";
    private final static String XVIRUSSTATUSYES = XVIRUSSTATUS + "(" + Constants.LWSP + ")*?YES";
    public final static Pattern XSPAMFLAGYESP = Pattern.compile(XSPAMFLAGYES, Pattern.CASE_INSENSITIVE);
    public final static Pattern XSPAMSTATUSYESP = Pattern.compile(XSPAMSTATUSYES, Pattern.CASE_INSENSITIVE);
    public final static Pattern XVIRUSSTATUSYESP = Pattern.compile(XVIRUSSTATUSYES, Pattern.CASE_INSENSITIVE);

    /* mail header field parameters */
    private final static String BOUNDARY = "boundary";
    private final static String DQUOTE = "\"";
    private final static String BOUNDARYOPEN = BOUNDARY + "=";
    private final static String BOUNDARYCLOSEEOLF = Constants.PEOLINEFEED;
    private final static String BOUNDARYQOPEN = BOUNDARYOPEN + DQUOTE;
    private final static String BOUNDARYQCLOSE = DQUOTE;
    public final static Pattern BOUNDARYOPENP = Pattern.compile(BOUNDARYOPEN, Pattern.CASE_INSENSITIVE);
    public final static Pattern BOUNDARYCLOSEEOLFP = Pattern.compile(BOUNDARYCLOSEEOLF);
    public final static Pattern BOUNDARYQOPENP = Pattern.compile(BOUNDARYQOPEN, Pattern.CASE_INSENSITIVE);
    public final static Pattern BOUNDARYQCLOSEP = Pattern.compile(BOUNDARYQCLOSE);

    private final static String RFCMESSAGE = "message\\/rfc822";
    public final static Pattern RFCMESSAGEP = Pattern.compile(RFCMESSAGE, Pattern.CASE_INSENSITIVE);

    public final static String MIMEHDRPLAIN = "Content-Type: text/plain; charset=\"iso-8859-1\"" + Constants.PCRLF + "Content-Transfer-Encoding: 7bit" + Constants.PCRLF + Constants.PCRLF;

    public final static String MBMARKSTART = "^--";
    public final static String MBMARKEND = "[^(--)]" + Constants.PEOLINEFEED;
    public final static String MBTERMSTART = MBMARKSTART;
    public final static String MBTERMEND = "--" + Constants.PEOLINEFEED;

    /* class variables */

    /* instance variables */

    /* constructors */
    private MLFieldConstants() {}

    /* public methods */

    /* private methods */
}
