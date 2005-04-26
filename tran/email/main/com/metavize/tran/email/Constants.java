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

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.util.MatchAction;
import com.metavize.tran.util.PatternAction;

public class Constants
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(Constants.class.getName());

    /* custom actions */
    public final static String PASS = Action.PASS.toString();
    public final static String BLOCK = Action.BLOCK.toString();
    public final static String EXCHANGE = Action.EXCHANGE.toString();
    public final static int PASS_ACTION = 1;
    public final static int BLOCK_ACTION = 2;
    public final static int EXCHANGE_ACTION = 3;
    public final static int COPYONBLOCK_ACTION = 4;
    public final static int LAST_ACTION = 5;

    /* spam actions */
    public final static String SSMARK = Action.MARK.toString();
    public final static String SSBLOCK = Action.BLOCK.toString();
    public final static String SSFANDWSENDR = Action.BLOCK_AND_WARN_SENDER.toString();
    public final static String SSFANDWRECVR = Action.BLOCK_AND_WARN_RECEIVER.toString();
    public final static String SSFANDWBOTH = Action.BLOCK_AND_WARN_BOTH.toString();

    /* virus actions */
    public final static String VSPASS = Action.PASS.toString();
    public final static String VSBLOCK = Action.BLOCK.toString();
    public final static String VSREPLACE = Action.REPLACE.toString();
    public final static String VSFANDWSENDR = Action.BLOCK_AND_WARN_SENDER.toString();
    public final static String VSFANDWRECVR = Action.BLOCK_AND_WARN_RECEIVER.toString();
    public final static String VSFANDWBOTH = Action.BLOCK_AND_WARN_BOTH.toString();

    /* types */
    /* generic header fields */
    public final static int NONE_IDX = 0;
    public final static int SENDER_IDX = 1; /* plus From */
    public final static int RECIPIENT_IDX = 2; /* plus To, Cc, Bcc */
    public final static int RELAY_IDX = 3;
    public final static int ORIGINATOR_IDX = 4;
    public final static int SUBJECT_IDX = 5;
    public final static int CONTENTTYPE_IDX = 6;
    public final static int MIMECONTENTTYPE_IDX = 7; /* plus MIME Content-Disposition */
    public final static int MIMECONTENTENCODE_IDX = 8;

    public final static int NAME_CNT = MIMECONTENTENCODE_IDX;

    /* Anti-Spam Scanners */
    public final static String SPAMAS = SScanner.SPAMAS.toString();
    public final static int SPAMAS_ID = 0;

    /* Anti-Virus Scanners */
    public final static String FPROTAV = VScanner.FPROTAV.toString();
    public final static String SOPHOSAV = VScanner.SOPHOSAV.toString();
    public final static String HAURIAV = VScanner.HAURIAV.toString();
    public final static String CLAMAV = VScanner.CLAMAV.toString();
    public final static int NOAV_ID = -1;
    public final static int FPROTAV_ID = 0;
    public final static int SOPHOSAV_ID = 1;
    public final static int HAURIAV_ID = 2;
    public final static int CLAMAV_ID = 3;

    public final static int NO_MSGSZ_LIMIT = -1;
    public final static int MSGSZ_MIN = 2048; /* 2K */

    /* specific header fields */
    public final static int FROM_IDX = NAME_CNT + 1; /* From */
    public final static int HSENDER_IDX = NAME_CNT + 2; /* Sender */
    public final static int TOLIST_IDX = NAME_CNT + 3; /* Recipient */
    public final static int CCLIST_IDX = NAME_CNT + 4; /* Recipient */
    public final static int BCCLIST_IDX = NAME_CNT + 5; /* Recipient */
    public final static int MIMECONTENTDISPOSITION_IDX = NAME_CNT + 6; /* MIME Content-Type */
    public final static int XSPAMFLAG_IDX = NAME_CNT + 7;
    public final static int XSPAMSTATUS_IDX = NAME_CNT + 8;
    public final static int DATE_IDX = NAME_CNT + 9; /* Date */
    public final static int XVIRUSSTATUS_IDX = NAME_CNT + 10;
    public final static int XVIRUSREPORT_IDX = NAME_CNT + 11;

    public final static Integer NONE_INT = new Integer(NONE_IDX);
    public final static Integer SENDER_INT = new Integer(SENDER_IDX);
    public final static Integer RECIPIENT_INT = new Integer(RECIPIENT_IDX);
    public final static Integer RELAY_INT = new Integer(RELAY_IDX);
    public final static Integer ORIGINATOR_INT = new Integer(ORIGINATOR_IDX);
    public final static Integer SUBJECT_INT = new Integer(SUBJECT_IDX);
    public final static Integer CONTENTTYPE_INT = new Integer(CONTENTTYPE_IDX);
    public final static Integer MIMECONTENTTYPE_INT = new Integer(MIMECONTENTTYPE_IDX);
    public final static Integer MIMECONTENTENCODE_INT = new Integer(MIMECONTENTENCODE_IDX);
    public final static Integer FROM_INT = new Integer(FROM_IDX);
    public final static Integer HSENDER_INT = new Integer(HSENDER_IDX);
    public final static Integer TOLIST_INT = new Integer(TOLIST_IDX);
    public final static Integer CCLIST_INT = new Integer(CCLIST_IDX);
    public final static Integer BCCLIST_INT = new Integer(BCCLIST_IDX);
    public final static Integer MIMECONTENTDISPOSITION_INT = new Integer(MIMECONTENTDISPOSITION_IDX);
    public final static Integer XSPAMFLAG_INT = new Integer(XSPAMFLAG_IDX);
    public final static Integer XSPAMSTATUS_INT = new Integer(XSPAMSTATUS_IDX);
    public final static Integer DATE_INT = new Integer(DATE_IDX);
    public final static Integer XVIRUSSTATUS_INT = new Integer(XVIRUSSTATUS_IDX);

    /* protocol reference ids */
    public final static char SMTP_RID = 'S';
    public final static char POP3_RID = 'P';
    public final static char IMAP4_RID = 'I';
    public final static char NULL_RID = '*';

    /* patterns for unformatted searches */
    /* end of line = <cr><lf> RFC 821 or <lf> SpamAssassin */
    private final static String CRLF = "\r\n";
    private final static String EOLINE = CRLF;
    private final static String EOLINEFEED = "\r??\n";
    public final static String LWSP = "\\p{Blank}"; /* linear-white-space */
    public final static String PEOLINE = EOLINE + "$"; /* protocol EOLINE */
    public final static String PEOLINEFEED = EOLINEFEED + "$"; /* spam assassin */
    public final static String PCRLF = CRLF; /* protocol CRLF */
    private final static Pattern CRLFP = Pattern.compile(CRLF);
    public final static Pattern PEOLINEP = Pattern.compile(PEOLINE);
    public final static Pattern EOLINEFEEDP = Pattern.compile(EOLINEFEED);

    /* patterns for formatted searches */
    /* null line terminates message header */
    private final static String NULLLINEEOL = "(^|" + EOLINE + ")" + EOLINE;
    private final static String NULLLINEEOLF = "(^|" + EOLINEFEED + ")" + EOLINEFEED;
    private final static Pattern NULLLINEEOLP = Pattern.compile(NULLLINEEOL);
    private final static Pattern NULLLINEEOLFP = Pattern.compile(NULLLINEEOLF);

    public final static String ANYCMD = "^[^:]+:";
    public final static Pattern ANYCMDP = Pattern.compile(ANYCMD);

    /* folded text starts with linear-white-space */
    private final static String FOLDED = "^" + LWSP;
    public final static Pattern FOLDEDP = Pattern.compile(FOLDED);

    private final static String FILEPREFIX = "^file:"; /* file */
    private final static String TEXTPREFIX = "^text:"; /* file text */
    public final static Pattern FILEPREFIXP = Pattern.compile(FILEPREFIX, Pattern.CASE_INSENSITIVE);
    public final static Pattern TEXTPREFIXP = Pattern.compile(TEXTPREFIX, Pattern.CASE_INSENSITIVE);

    private final static String CONTLIST = "(,|>|" + Constants.PEOLINE + ")";
    public final static Pattern CONTLISTP = Pattern.compile(CONTLIST);

    public final static String NO_DEFAULT = "no default";

    public final static String ENCODING = System.getProperty("file.encoding");
    public final static Charset CHARSET = Charset.forName(ENCODING);

    public final static String BASEPATH = System.getProperty("java.io.tmpdir");
    private final static String XMPATH = BASEPATH + File.separator + "email" + File.separator;
    private final static String BLOCKDIR = "block" + File.separator;
    private final static String RDEXCDIR = "read_exc" + File.separator;
    private final static String PRSEXCDIR = "parse_exc" + File.separator;
    private final static String PROEXCDIR = "proto_exc" + File.separator;
    private final static String IOEXCDIR = "io_exc" + File.separator;
    private final static String MDFEXCDIR = "modify_exc" + File.separator;

    public final static int CSBLOCK = 0; /* custom copy on block */
    public final static int ASBLOCK = 1; /* spam copy on block */
    public final static int AVBLOCK = 2; /* virus copy on block */
    public final static int RELAY = 3; /* message relay size limit */

    public final static byte NULLBA[] = { '*', '*', '*' };

    /* parts of notification message that replace blocked messages */
    public final static byte BLOCKFROMBA[] = { 'F', 'r', 'o', 'm', ':', ' ' };
    public final static byte BLOCKTOBA[] = { 'T', 'o', ':', ' ' };
    /* default block body */
    public final static byte BLOCKCSBODYBA[] = { 'A', 'n', ' ', 'e', '-', 'm', 'a', 'i', 'l', ' ', 'b', 'l', 'o', 'c', 'k', ' ', 'r', 'u', 'l', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', ' ', 't', 'h', 'e', ' ', 'd', 'e', 'l', 'i', 'v', 'e', 'r', 'y', ' ', 'o', 'f', ' ', 't', 'h', 'i', 's', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', '.', 13, 10 };
    /* anti-spam block body */
    public final static byte BLOCKASBODYBA[] = { 'A', 'n', ' ', 'e', '-', 'm', 'a', 'i', 'l', ' ', 'a', 'n', 't', 'i', '-', 's', 'p', 'a', 'm', ' ', 'r', 'u', 'l', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', ' ', 't', 'h', 'e', ' ', 'd', 'e', 'l', 'i', 'v', 'e', 'r', 'y', ' ', 'o', 'f', ' ', 't', 'h', 'i', 's', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', '.', 13, 10 };
    /* anti-virus block body */
    public final static byte BLOCKAVBODYBA[] = { 'A', 'n', ' ', 'e', '-', 'm', 'a', 'i', 'l', ' ', 'a', 'n', 't', 'i', '-', 'v', 'i', 'r', 'u', 's', ' ', 'r', 'u', 'l', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', ' ', 't', 'h', 'e', ' ', 'd', 'e', 'l', 'i', 'v', 'e', 'r', 'y', ' ', 'o', 'f', ' ', 't', 'h', 'i', 's', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', '.', 13, 10 };
//    /* message exceeds size limit reject body */
//    public final static byte REJECTBODYBA[] = { 'T', 'h', 'i', 's', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'e', 'x', 'c', 'e', 'e', 'd', 's', ' ', 't', 'h', 'e', ' ', 'm', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 's', 'i', 'z', 'e', ' ', 'l', 'i', 'm', 'i', 't', ' ', 'a', 'n', 'd', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 'b', 'l', 'o', 'c', 'k', 'e', 'd', '.', 13, 10 };
    /* common body */
    public final static byte BLOCKBODYAUTOBA[] = { '[', 'P', 'l', 'e', 'a', 's', 'e', ' ', 'd', 'o', ' ', 'n', 'o', 't', ' ', 'r', 'e', 'p', 'l', 'y', ' ', 't', 'o', ' ', 't', 'h', 'i', 's', ' ', 'a', 'u', 't', 'o', 'm', 'a', 't', 'e', 'd', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', '.', ']', 13, 10 };
    /* common body - copy note */
    public final static byte BLOCKBODYCOPYBA[] = { 13, 10, 'A', ' ', 'c', 'o', 'p', 'y', ' ', 'o', 'f', ' ', 'y', 'o', 'u', 'r', ' ', 'o', 'r', 'i', 'g', 'i', 'n', 'a', 'l', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ' ', 'h', 'a', 's', ' ', 'b', 'e', 'e', 'n', ' ', 's', 'a', 'v', 'e', 'd', '.', ' ', 'C', 'o', 'n', 't', 'a', 'c', 't', ' ', 'y', 'o', 'u', 'r', ' ', 's', 'y', 's', 't', 'e', 'm', ' ', 'a', 'd', 'm', 'i', 'n', 'i', 's', 't', 'r', 'a', 't', 'o', 'r', ' ', 'i', 'f', ' ', 'y', 'o', 'u', ' ', 'n', 'e', 'e', 'd', ' ', 't', 'o', ' ', 'r', 'e', 't', 'r', 'i', 'e', 'v', 'e', ' ', 't', 'h', 'i', 's', ' ', 'c', 'o', 'p', 'y', '.', 13, 10 };
    public final static byte EOLINEBA[] = { 13, 10 };
    public final static byte LINEFEEDBA[] = { 10 };
    public final static byte LOCALHOSTBA[] = { '@', 'l', 'o', 'c', 'a', 'l', 'h', 'o', 's', 't' };
    /* virus was removed */
    public final static byte VIRUSREMOVEDBA[] = { 'A', ' ', 'v', 'i', 'r', 'u', 's', ' ', 'w', 'a', 's', ' ', 'd', 'e', 't', 'e', 'c', 't', 'e', 'd', ' ', 'a', 'n', 'd', ' ', 'r', 'e', 'm', 'o', 'v', 'e', 'd', ' ', 'f', 'r', 'o', 'm', ' ', 't', 'h', 'i', 's', ' ', 'm', 'e', 's', 's', 'a', 'g', 'e', ':', 13, 10 };

    /* we prefer to set file access as 600 but 
     * we may use scanners
     * that run as daemons and cannot read files with root read-only access
     * so we must create decoded files with access set to 644
     */
    public final static byte BASE64STARTBA[] = { 'b', 'e', 'g', 'i', 'n', '-', 'b', 'a', 's', 'e', '6', '4', ' ', '6', '4', '4', ' ' };
    public final static byte BASE64ENDBA[] = { '=', '=', '=', '=' };
    public final static byte UUENCODESTARTBA[] = { 'b', 'e', 'g', 'i', 'n', ' ', '6', '4', '4', ' ' };
    public final static byte UUENCODEENDBA[] = { 'e', 'n', 'd' };

    public final static int RDEXCDIR_VAL = 0;
    public final static int PRSEXCDIR_VAL = 1;
    public final static int PROEXCDIR_VAL = 2;
    public final static int IOEXCDIR_VAL = 3;
    public final static int BLOCKDIR_VAL = 4;
    public final static int MDFEXCDIR_VAL = 5;
    public final static int DEFAULTDIR_VAL = 6;

    public final static String EMPTYSTR = "";
    public final static String XMPREFIX = "xms";

    public final static int FATAL_EXIT = -1;
    public final static int ERROR_EXIT = -2;

    public final static IPDataResult PASS_THROUGH = IPDataResult.PASS_THROUGH;
    public final static IPDataResult DO_NOT_PASS = IPDataResult.DO_NOT_PASS;
    public final static TCPChunkResult READ_MORE_NO_WRITE = TCPChunkResult.READ_MORE_NO_WRITE;

    public final static int CLIENT = 0; /* default */
    public final static int SERVER = 1;

    /* flag values */
    public final static int SCAN_TYPE      = 0x000001;
    public final static int BLOCK_TYPE     = 0x000010;
    public final static int CPONBLOCK_TYPE = 0x000100;
    public final static int NTFYSENDR_TYPE = 0x001000;
    public final static int NTFYRECVR_TYPE = 0x010000;
    public final static int REPLACE_TYPE   = 0x100000;
    public final static int NO_TYPE        = 0x000000;

    /* class variables */
    private static ByteBuffer zMIMEHdrPlainLine;
    private static ByteBuffer zBase64End;
    private static ByteBuffer zUuencodeEnd;
    private static File zBaseDir;
    private static File zBlockDir;
    private static File zReadExcDir;
    private static File zParseExcDir;
    private static File zProtoExcDir;
    private static File zIOExcDir;
    private static File zModifyExcDir;
    private static Pattern zNullLineP;

    /* instance variables */

    /* constructors */
    private Constants() {}

    /* public methods */
    public static Integer convertType(FieldType zType)
    {
        if (true == zType.equals(FieldType.SENDER))
        {
            return SENDER_INT;
        }
        else if (true == zType.equals(FieldType.RECIPIENT))
        {
            return RECIPIENT_INT;
        }
        else if (true == zType.equals(FieldType.RELAY))
        {
            return RELAY_INT;
        }
        else if (true == zType.equals(FieldType.ORIGINATOR))
        {
            return ORIGINATOR_INT;
        }
        else if (true == zType.equals(FieldType.CONTENT_TYPE))
        {
            return CONTENTTYPE_INT;
        }
        else if (true == zType.equals(FieldType.MIME_CONTENT_TYPE))
        {
            return MIMECONTENTTYPE_INT;
        }
        else if (true == zType.equals(FieldType.MIME_CONTENT_ENCODE))
        {
            return MIMECONTENTENCODE_INT;
        }
        else if (true == zType.equals(FieldType.SUBJECT))
        {
            return SUBJECT_INT;
        }
        else
        {
            return NONE_INT;
        }
    }

    public static FieldType convertType(int iType)
    {
        switch(iType)
        {
        default:
            return FieldType.NONE;

        case SUBJECT_IDX:
            return FieldType.SUBJECT;

        case SENDER_IDX:
        case FROM_IDX:
            return FieldType.SENDER;

        case RECIPIENT_IDX:
        case TOLIST_IDX:
        case CCLIST_IDX:
        case BCCLIST_IDX:
            return FieldType.RECIPIENT;

        case RELAY_IDX:
            return FieldType.RELAY;

        case ORIGINATOR_IDX:
            return FieldType.ORIGINATOR;

        case CONTENTTYPE_IDX:
            return FieldType.CONTENT_TYPE;

        case MIMECONTENTTYPE_IDX:
        case MIMECONTENTDISPOSITION_IDX:
            return FieldType.MIME_CONTENT_TYPE;

        case MIMECONTENTENCODE_IDX:
            return FieldType.MIME_CONTENT_ENCODE;
        }
    }

    /* convert spam scanner type */
    public static int convertSSType(String zType, boolean bCopyOnBlock)
    {
        int iType;

        if (true == zType.equals(SSBLOCK))
        {
            if (false == bCopyOnBlock)
            {
                iType = BLOCK_TYPE;
            }
            else
            {
                iType = (CPONBLOCK_TYPE | BLOCK_TYPE);
            }
        }
        else if (true == zType.equals(SSFANDWSENDR))
        {
            iType = NTFYSENDR_TYPE;
        }
        else if (true == zType.equals(SSFANDWRECVR))
        {
            iType = NTFYRECVR_TYPE;
        }
        else if (true == zType.equals(SSFANDWBOTH))
        {
            iType = (NTFYSENDR_TYPE | NTFYRECVR_TYPE);
        }
        else
        {
            iType = NO_TYPE;
        }

        return iType;
    }

    /* convert virus scanner type */
    public static int convertVSType(String zType, boolean bCopyOnBlock)
    {
        int iType;

        if (true == zType.equals(VSBLOCK))
        {
            if (false == bCopyOnBlock)
            {
                iType = BLOCK_TYPE;
            }
            else
            {
                iType = (CPONBLOCK_TYPE | BLOCK_TYPE);
            }
        }
        else if (true == zType.equals(VSREPLACE))
        {
            iType = REPLACE_TYPE;
        }
        else if (true == zType.equals(VSFANDWSENDR))
        {
            iType = NTFYSENDR_TYPE;
        }
        else if (true == zType.equals(VSFANDWRECVR))
        {
            iType = NTFYRECVR_TYPE;
        }
        else if (true == zType.equals(VSFANDWBOTH))
        {
            iType = (NTFYSENDR_TYPE | NTFYRECVR_TYPE);
        }
        else
        {
            iType = NO_TYPE;
        }

        return iType;
    }

    public static boolean setAll()
    {
        if (false == setMIMEHdrPlain())
        {
            return false;
        }
        zBase64End = ByteBuffer.wrap(BASE64ENDBA, BASE64ENDBA.length, 0);
        zUuencodeEnd = ByteBuffer.wrap(UUENCODEENDBA, UUENCODEENDBA.length, 0);

        zBaseDir = new File(Constants.XMPATH);
        zBlockDir = new File(zBaseDir, Constants.BLOCKDIR);
        zReadExcDir = new File(zBaseDir, Constants.RDEXCDIR);
        zParseExcDir = new File(zBaseDir, Constants.PRSEXCDIR);
        zProtoExcDir = new File(zBaseDir, Constants.PROEXCDIR);
        zIOExcDir = new File(zBaseDir, Constants.IOEXCDIR);
        zModifyExcDir = new File(zBaseDir, Constants.MDFEXCDIR);

        try
        {
            if (false == zBaseDir.exists() &&  false == zBaseDir.mkdir())
            {
                zLog.error("Unable to create base directory: " + zBaseDir.getAbsolutePath());
                return false;
            }

            if (false == zBlockDir.exists() && false == zBlockDir.mkdir())
            {
                zLog.error("Unable to create blocked message directory: " + zBlockDir.getAbsolutePath());
                return false;
            }

            if (false == zReadExcDir.exists() && false == zReadExcDir.mkdir())
            {
                zLog.error("Unable to create read exception directory: " + zReadExcDir.getAbsolutePath());
                return false;
            }

            if (false == zParseExcDir.exists() && false == zParseExcDir.mkdir())
            {
                zLog.error("Unable to create parse exception directory: " + zParseExcDir.getAbsolutePath());
                return false;
            }

            if (false == zProtoExcDir.exists() && false == zProtoExcDir.mkdir())
            {
                zLog.error("Unable to create protocol exception directory: " + zProtoExcDir.getAbsolutePath());
                return false;
            }

            if (false == zIOExcDir.exists() && false == zIOExcDir.mkdir())
            {
                zLog.error("Unable to create io exception directory: " + zIOExcDir.getAbsolutePath());
                return false;
            }
        }
        catch (SecurityException e)
        {
            zLog.error("Unable to create exception directory: " + e);
            return false;
        }

        zNullLineP = NULLLINEEOLFP;
        return true;
    }

    public static ByteBuffer getMIMEHdrPlain()
    {
        return zMIMEHdrPlainLine;
    }

    public static ByteBuffer getBase64End()
    {
        return zBase64End;
    }

    public static ByteBuffer getUuencodeEnd()
    {
        return zUuencodeEnd;
    }

    public static File getDir(int iDir)
    {
        if (null == zBaseDir)
        {
            zLog.debug("Dirs not initialized");
            return null;
        }

        switch(iDir)
        {
        case BLOCKDIR_VAL:
            return zBlockDir;

        case RDEXCDIR_VAL:
            return zReadExcDir;

        case PRSEXCDIR_VAL:
            return zParseExcDir;

        case PROEXCDIR_VAL:
            return zProtoExcDir;

        case IOEXCDIR_VAL:
            return zIOExcDir;

        case MDFEXCDIR_VAL:
            return zModifyExcDir;

        default:
            return zBaseDir;
        }
    }

    public static Pattern getNullLine()
    {
        return zNullLineP;
    }

    /* private methods */
    private static boolean setMIMEHdrPlain()
    {
        if (null == zMIMEHdrPlainLine)
        {
            try
            {
                zMIMEHdrPlainLine = CHARSET.newEncoder().encode(CharBuffer.wrap(MLFieldConstants.MIMEHDRPLAIN));
                zMIMEHdrPlainLine.position(zMIMEHdrPlainLine.limit());
            }
            catch (CharacterCodingException e)
            {
                zLog.error("Unable to encode fixed MIME header: " + MLFieldConstants.MIMEHDRPLAIN);
                return false;
            }
        }

        return true;
    }
}
