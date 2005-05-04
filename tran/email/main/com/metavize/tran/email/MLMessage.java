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

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.NumberFormatException;
import java.nio.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.*;
import com.metavize.tran.util.*;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

/* XXX - to do
 * - for folded fields,
 *   we may need to concantenate multiple text lines
 *   into single text line
 *   to properly match fields against patterns
 */
/* message contains header and body
 * message header contains fields with values
 * - message header field values may be extended as parameter/value pairs
 * message body contains data (of arbitrary or MIME form)
 * - data in MIME form (MIME body) contain collection of MIME parts
 * - MIME part contains header and body
 * - MIME part header contains fields with values
 * - MIME part header field values may be extended as parameter/value pairs
 * - MIME part body contains data (of arbitrary, message, or MIME form)
 *   - data in message form (nested message) contain message (see above)
 *   - data in MIME form contain collection of MIME parts (see above)
 *
 * required - if message exists, then object must exist
 * optional - if message exists, then object may or may not exist
 */
public class MLMessage
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MLMessage.class.getName());

    private final static byte BASE64_FNAMEBA[] = { 'M', 'S', 'G', 'b', '6', '4' };
    private final static byte UUENCODE_FNAMEBA[] = { 'M', 'S', 'G', 'u', 'u', 'e' };

    private final static String SQUOTESTR = "\\'";
    private final static String SQUOTELSTR = "\\\\'";
    private final static String OPARENSTR = "\\(";
    private final static String OPARENLSTR = "\\\\(";
    private final static String CPARENSTR = "\\)";
    private final static String CPARENLSTR = "\\\\)";
    private final static String PLUSSTR = "\\+";
    private final static String PLUSLSTR = "\\\\+";
    private final static String COMMASTR = "\\,";
    private final static String COMMALSTR = "\\\\,";
    private final static String DASHSTR = "\\-";
    private final static String DASHLSTR = "\\\\-";
    private final static String DOTSTR = "\\.";
    private final static String DOTLSTR = "\\\\.";
    private final static String COLONSTR = "\\:";
    private final static String COLONLSTR = "\\\\:";
    private final static String EQUALSTR = "\\=";
    private final static String EQUALLSTR = "\\\\=";
    private final static String QUESTSTR = "\\?";
    private final static String QUESTLSTR = "\\\\?";
    private final static Pattern SQUOTESTRP = Pattern.compile(SQUOTESTR);
    private final static Pattern OPARENSTRP = Pattern.compile(OPARENSTR);
    private final static Pattern CPARENSTRP = Pattern.compile(CPARENSTR);
    private final static Pattern PLUSSTRP = Pattern.compile(PLUSSTR);
    private final static Pattern COMMASTRP = Pattern.compile(COMMASTR);
    private final static Pattern DASHSTRP = Pattern.compile(DASHSTR);
    private final static Pattern DOTSTRP = Pattern.compile(DOTSTR);
    private final static Pattern COLONSTRP = Pattern.compile(COLONSTR);
    private final static Pattern EQUALSTRP = Pattern.compile(EQUALSTR);
    private final static Pattern QUESTSTRP = Pattern.compile(QUESTSTR);

    private final static int NOT_SET = -1;

    /* class variables */

    /* instance variables */
    private CharsetDecoder zDecoder;
    private CharsetEncoder zEncoder;

    /* zSender and zRcpts override
     * sender and recipient info in mail message
     * - sender should be same as from and
     *   recipients should be same as tolist, cclist, and bcclist
     *   (zSender is generated from sender info and
     *    zRcpts is generated from recipient info)
     *   but if they are not,
     *   transport services will only use zSender and zRcpts
     *
     * sender and recipient fields cannot be folded
     * but multiple recipients may be specified (on separate lines)
     * so we build array list of CBufferWrappers for recipients
     */
    private CBufferWrapper zSender = null; /* sender - optional */
    private ArrayList zRcpts = null; /* recipients - optional */
    /* array list of CBufferWrappers
     * - message header, body, and MIME body buffers
     */
    private ArrayList zDatas; /* required */

    private int iSize = NOT_SET;
    private boolean bIsReady;

    /* variables (that follow this line) represent views of above variables */

    private ArrayList zHdrs = null; /* header */
    private ArrayList zBodys = null; /* body and MIME body */
    private int iEncodedType = Constants.NOENCODE_TYPE;

    private MLMessagePTO zPTO = null;

    private MIMEBody zMIMEBody = null;

    /* message body may start immediately after message header or
     * within first MIME part of MIME body
     * (message body contains body and/or MIME body but
     * if MIME body is present,
     * then first MIME part of MIME body contains actual message body and
     * MIME parts that follow are actual MIME parts)
     */
    private MIMEPart zMsgBodyStartMP = null;

    /* these fields can be folded */
    private ArrayList zRelays = null;
    private ArrayList zFroms = null;
    private ArrayList zHSenders = null;
    private ArrayList zDates = null; /* not usually folded */
    private ArrayList zToLists = null;
    private ArrayList zCcLists = null;
    private ArrayList zBccLists = null; /* usually null */
    private ArrayList zReplyTos = null;
    private ArrayList zSubjects = null; /* not usually folded */
    private ArrayList zContentTypes = null;
    private ArrayList zContentEncodes = null;
    private ArrayList zXSpamFlags = null;
    private ArrayList zXSpamStatuss = null;
    private ArrayList zXVirusStatuss = null;
    private ArrayList zXVirusReports = null;

    /* constructors */
    public MLMessage()
    {
        zDecoder = Constants.CHARSET.newDecoder();
        zEncoder = Constants.CHARSET.newEncoder();
        zDatas = new ArrayList();
        bIsReady = false;
    }

    /* public methods */
    public void setSender(CBufferWrapper zSender)
    {
        this.zSender = zSender;
        return;
    }

    public void setRcpt()
    {
        if (null == zRcpts)
        {
            zRcpts = new ArrayList();
        }
        return;
    }

    public void setRcpt(CBufferWrapper zRcpt)
    {
        if (null == zRcpts)
        {
            zRcpts = new ArrayList();
        }
        zRcpts.add(zRcpt);
        return;
    }

    public void addRcpt(CBufferWrapper zRcpt)
    {
        zRcpts.add(zRcpt);
        return;
    }

    public void addData(CBufferWrapper zData)
    {
        zDatas.add(zData);
        return;
    }

    public void clearSize()
    {
        iSize = NOT_SET;
        return;
    }

    public int setSize(int iSize)
    {
        return this.iSize = iSize;
    }

    public int setSize(String zSzStr)
    {
        try
        {
            this.iSize = Integer.valueOf(zSzStr).intValue();
        }
        catch (NumberFormatException e)
        {
            zLog.error("Unable to convert message size string (" + zSzStr + ") to integer value: " + e);
            clearSize();
        }

        return this.iSize;
    }

    public CBufferWrapper getSender()
    {
        return zSender;
    }

    public ArrayList getRcpt()
    {
        return zRcpts;
    }

    public ArrayList getData()
    {
        return zDatas;
    }

    public int getSize()
    {
        if (NOT_SET == iSize &&
            false == zDatas.isEmpty())
        {
            CBufferWrapper zCTmp;

            iSize = 0;
            for (Iterator zIter = zDatas.iterator(); true == zIter.hasNext(); )
            {
                zCTmp = (CBufferWrapper) zIter.next();
                iSize += zCTmp.length();
            }
            zLog.debug("msg size: " + iSize);
        }

        return iSize;
    }

    public MLMessagePTO getPTO()
    {
        return zPTO;
    }

    public ArrayList getRelay()
    {
        return zRelays;
    }

    public ArrayList getFrom()
    {
        ArrayList zTmp = new ArrayList();
        if (null != zFroms &&
            false == zFroms.isEmpty())
        {
            zTmp.addAll(zFroms);
        }
        if (null != zHSenders &&
            false == zHSenders.isEmpty())
        {
            zTmp.addAll(zHSenders);
        }
        return zTmp;
    }

    public ArrayList getDate()
    {
        return zDates;
    }

    public ArrayList getToList()
    {
        return zToLists;
    }

    public ArrayList getCcList()
    {
        return zCcLists;
    }

    public ArrayList getBccList()
    {
        return zBccLists;
    }

    public ArrayList getReplyTo()
    {
        return zReplyTos;
    }

    public ArrayList getSubject()
    {
        return zSubjects;
    }

    public ArrayList getContentType()
    {
        return zContentTypes;
    }

    public ArrayList getContentEncodes()
    {
        return zContentEncodes;
    }

    public ArrayList getXSpamStatus()
    {
        return zXSpamStatuss;
    }

    public ArrayList getXVirusReport()
    {
        return zXVirusReports;
    }

    public MIMEBody getMIMEBody()
    {
        return zMIMEBody;
    }

    public boolean isEmpty()
    {
        return zDatas.isEmpty();
    }

    public boolean isReady()
    {
        return bIsReady;
    }

    /* reset contents of message */
    public void reset()
    {
        if (null != zHdrs)
        {
            zHdrs.clear();
        }

        if (null != zBodys)
        {
            zBodys.clear();
        }

        iEncodedType = Constants.NOENCODE_TYPE;

        if (null != zPTO)
        {
            zPTO.reset();
        }
        bIsReady = false;

        if (null != zMIMEBody)
        {
            zMIMEBody.reset();
        }

        zMsgBodyStartMP = null;

        if (null != zRelays)
        {
            zRelays.clear();
            zRelays = null;
        }

        if (null != zFroms)
        {
            zFroms.clear();
            zFroms = null;
        }

        if (null != zHSenders)
        {
            zHSenders.clear();
            zHSenders = null;
        }

        if (null != zDates)
        {
            zDates.clear();
            zDates = null;
        }

        if (null != zToLists)
        {
            zToLists.clear();
            zToLists = null;
        }

        if (null != zCcLists)
        {
            zCcLists.clear();
            zCcLists = null;
        }

        if (null != zBccLists)
        {
            zBccLists.clear();
            zBccLists = null;
        }

        if (null != zReplyTos)
        {
            zReplyTos.clear();
            zReplyTos = null;
        }

        if (null != zSubjects)
        {
            zSubjects.clear();
            zSubjects = null;
        }

        if (null != zContentTypes)
        {
            zContentTypes.clear();
            zContentTypes = null;
        }

        if (null != zContentEncodes)
        {
            zContentEncodes.clear();
            zContentEncodes = null;
        }

        if (null != zXSpamFlags)
        {
            zXSpamFlags.clear();
            zXSpamFlags = null;
        }

        if (null != zXSpamStatuss)
        {
            zXSpamStatuss.clear();
            zXSpamStatuss = null;
        }

        if (null != zXVirusStatuss)
        {
            zXVirusStatuss.clear();
            zXVirusStatuss = null;
        }

        if (null != zXVirusReports)
        {
            zXVirusReports.clear();
            zXVirusReports = null;
        }

        return;
    }

    /* flush/delete contents of message */
    public void flush()
    {
        if (null != zPTO)
        {
            zPTO.flush();
            zPTO = null; /* we don't want to repeat this during reset */
        }

        if (null != zMIMEBody)
        {
            zMIMEBody.flush();
            zMIMEBody = null; /* we don't want to repeat this during reset */
        }

        reset();

        zSender = null;
        if (null != zRcpts)
        {
            zRcpts.clear();
            zRcpts = null;
        }
        zHdrs = null;
        zBodys = null;

        zDatas.clear();
        zDatas.trimToSize();

        clearSize();

        return;
    }

    /* break message data buffers into text components and
     * identify interesting components
     *
     * RFC 822:
     *  A message consists of header fields and, optionally, a body.
     *  The body is simply a sequence of lines containing ASCII characters.
     *  It is separated from the headers by a null line  (i.e.,  a
     *  line with nothing preceding the CRLF).
     */
    public void parse(boolean bParseBody) throws ParseException
    {
        ListIterator zLIter;
        Matcher zMatcher;
        CBufferWrapper zCLine;

        if (null == zHdrs)
        {
            zHdrs = new ArrayList();
        }

        Pattern zNullLineP = Constants.getNullLine();

        /* limit message header field search to message header */
        for (zLIter = zDatas.listIterator(); true == zLIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zLIter.next();

            zMatcher = zNullLineP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                /* we've identified text lines that define message header
                 * - message header includes everything
                 *   from start to this null line
                 * - message body includes everything that
                 *   follows this null line
                 */

                zHdrs.add(zCLine); /* add NULLLINE to message header */

                /* we have no more buffers to add to message header
                 * - message body starts here
                 */
                break; /* for loop */
            }

            zHdrs.add(zCLine);
        }

        if (true == zHdrs.isEmpty())
        {
            throw new ParseException("Unable to parse message because this message is empty (lacks a header)");
        }

        parseHdr();

        if (false == zLIter.hasNext())
        {
            /* message is not properly formatted
             * (e.g., message has no null line
             *  to delineate message header from message body and
             *  thus, doesn't contain message body)
             * so we process what we have
             */
            setPTO();
            return; /* this message has no body */
        }

        /* rest of message is message body (which may include MIME body) */
        if (null == zBodys)
        {
            zBodys = new ArrayList(zDatas.subList(zLIter.nextIndex(), zDatas.size()));
        }
        else
        {
            zBodys.addAll(zDatas.subList(zLIter.nextIndex(), zDatas.size()));
        }

        if (false == bParseBody ||
            true == zBodys.isEmpty())
        {
            setPTO();
            return;
        }

        parseBody();

        if (null == zMIMEBody)
        {
            setPTO();
            return; /* this message has no MIME body */
        }
        /* else message contains MIME body
         * so first MIME part of MIME body contains message body
         */
        zMsgBodyStartMP = zMIMEBody.getPart(0);

        setPTO();
        return;
    }

    /* unparse reverses parseHdr and parseBody */
    public void unparse(ArrayList zDataSrc, boolean bByteBuffer)
    {
        /* message hdr */
        zDataSrc.addAll(MLLine.toBuffer(zHdrs, bByteBuffer));

        if (null == zBodys ||
            true == zBodys.isEmpty())
        {
            /* no message body (and thus, no MIME body) */
            return;
        }

        if (null == zMIMEBody)
        {
            /* message body has no MIME body (we didn't parse message body) */
            zDataSrc.addAll(MLLine.toBuffer(zBodys, bByteBuffer));
            return;
        }
        /* else message body has MIME body */

        zMIMEBody.unparse(zDataSrc, bByteBuffer);
        return;
    }

    public void modify(XMailScannerCache zXMSCache, Object zAction) throws ModifyException
    {
        boolean bIsFile;

        MatchAction zMatchAction = (MatchAction) zAction;
        String zKeyStr = zMatchAction.getPatternAction().getValue();
        Matcher zKeyMatcher = Constants.FILEPREFIXP.matcher(zKeyStr);
        if (false == zKeyMatcher.find())
        {
            zKeyMatcher = Constants.TEXTPREFIXP.matcher(zKeyStr);
            if (false == zKeyMatcher.find())
            {
                bIsFile = false; /* replacement value is literal text */
            }
            else
            {
                bIsFile = true; /* replacement value is file text */
            }
        }
        else
        {
            bIsFile = true; /* replacement value is file text */
        }

        /* exchange value may be String (literal) or
         * ArrayList (file/file text data)
         */
        Object zNewValue = zXMSCache.getExchValue(zKeyStr);
        Matcher zMatcher = zMatchAction.getMatcher();
        CBufferWrapper zCLine = (CBufferWrapper) zMatchAction.getMatchLine();

        int iFieldType = zMatchAction.getType();
        switch(iFieldType)
        {
        default:
        case Constants.NONE_IDX:
            break;

        case Constants.SENDER_IDX:
        case Constants.FROM_IDX:
        case Constants.HSENDER_IDX:
        case Constants.RECIPIENT_IDX:
        case Constants.TOLIST_IDX:
        case Constants.CCLIST_IDX:
        case Constants.BCCLIST_IDX:
        case Constants.RELAY_IDX:
        case Constants.ORIGINATOR_IDX:
        case Constants.SUBJECT_IDX:
        case Constants.CONTENTTYPE_IDX:
            if (false == bIsFile)
            {
                replace(zCLine, (String) zNewValue, zMatcher);
                break;
            }
            /* else we have file text and
             * need to replace body of message
             */

            replaceBody((ArrayList) zNewValue, zMatcher);
            break;

        case Constants.MIMECONTENTTYPE_IDX:
        case Constants.MIMECONTENTENCODE_IDX:
            if (false == bIsFile)
            {
                replace(zCLine, (String) zNewValue, zMatcher);
                break;
            }
            /* else we have file text and
             * need to replace body of any matching MIME parts of MIME body
             */

            if (null != zMIMEBody)
            {
                zMIMEBody.replace(iFieldType, (ArrayList) zNewValue, zMatcher);
            }

            break;
        }

        /* we've modified contents of message so reconstruct message */
        ArrayList zTmpDatas = unparse(false); /* format new (modified) data */
        reset(); /* clear (org) message view */
        zDatas.clear(); /* release (new) unformatted data */
        zDatas.addAll(zTmpDatas); /* add (new) formatted data */
        return;
    }

    public VirusScannerResult scan(XMailScannerCache zXMSCache, VirusScanner zScanner, boolean bReplace) throws ModifyException, IOException, InterruptedException
    {
        if (null == zMIMEBody)
        {
            return null;
        }

        ByteBuffer zReplLine = zXMSCache.getVirusRemoved();
        VirusScannerResult zScanResult = zMIMEBody.scan(zReplLine, zScanner, bReplace);

        if (true == bReplace &&
            (null != zScanResult && false == zScanResult.isClean()))
        {
            /* we've modified contents of message so reconstruct message */
            ArrayList zTmpDatas = unparse(false); /* format new (modified) data */
            reset(); /* clear (org) message view */
            zDatas.clear(); /* release (new) unformatted data */
            zDatas.addAll(zTmpDatas); /* add (new) formatted data */
        }

        return zScanResult;
    }

    /* strip boundary parameter value from Content-Type field */
    public static String getCTBoundary(ArrayList zCTList) throws ParseException
    {
        if (null == zCTList ||
            true == zCTList.isEmpty())
        {
            return null;
        }

        String zStr = null;

        Matcher zMatcher;
        CBufferWrapper zCLine;
        CBufferWrapper zCTmp;

        for (Iterator zIter = zCTList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();

            zMatcher = MLFieldConstants.BOUNDARYQOPENP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                /* boundary parameter value is quoted */
                zCTmp = (CBufferWrapper) zCLine.subSequence(zMatcher.end(), zCLine.length());
                zMatcher = MLFieldConstants.BOUNDARYQCLOSEP.matcher(zCTmp);
                /* fall through */
            }
            else
            {
                zMatcher = MLFieldConstants.BOUNDARYOPENP.matcher(zCLine);
                if (true == zMatcher.find())
                {
                    /* boundary parameter value is not quoted */
                    zCTmp = (CBufferWrapper) zCLine.subSequence(zMatcher.end(), zCLine.length());
                    zMatcher = MLFieldConstants.BOUNDARYCLOSEEOLFP.matcher(zCTmp);
                    /* fall through */
                }
                else
                {
                    /* boundary parameter is not present in this field line */
                    continue; /* for loop */
                }
            }

            if (false == zMatcher.find())
            {
                throw new ParseException("Unable to parse header of this message because the content-type field contains an invalid boundary parameter value: \"" + zCLine + "\"");
            }
            /* else found boundary parameter close char sequence */

            /* we cannot use Matcher.group() here because
             * Matcher assumes that its input sequence is String and
             * we are using CBufferWrapper (CharSequence)
             */
            zStr = maskMeta((CBufferWrapper) zCTmp.subSequence(0, zMatcher.start()));
            break; /* for loop */
        }

        return zStr;
    }

    /* get list of source buffers (ByteBuffers) that backs this message */
    public ArrayList getDataBuffers()
    {
        if (true == isEmpty())
        {
            flush(); /* delete remaining contents of message */
            return null;
        }

        ArrayList zDataSrc = unparse(true); /* format current data */
        return zDataSrc;
    }

    /* replace list of source buffers (ByteBuffers) that backs this message
     * with list of new source buffers
     */
    public void putDataBuffers(ArrayList zDataSrc)
    {
        MLLine.fromBuffer(zDataSrc, zDatas, true);
        return;
    }

    public void putDataBuffers(ArrayList zDataSrc, boolean bParseBody) throws ParseException
    {
        putDataBuffers(zDataSrc);

        reset(); /* we have new backing data so reset this message */
        /* scanner returns each text line on its own buffer so
         * they is no raw data to process here
         */
        parse(bParseBody);
        /* we do not need to create new zMsgInfo or update original zMsgInfo */
        return;
    }

    /* check if message has any SpamAssassin fields with yes values */
    public boolean isSpam()
    {
        CBufferWrapper zCLine;
        Matcher zMatcher;

        if (null != zXSpamFlags)
        {
            zCLine = (CBufferWrapper) zXSpamFlags.get(0);
            zMatcher = MLFieldConstants.XSPAMFLAGYESP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                return true;
            }
        }

        if (null != zXSpamStatuss)
        {
            zCLine = (CBufferWrapper) zXSpamStatuss.get(0);
            zMatcher = MLFieldConstants.XSPAMSTATUSYESP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                return true;
            }
        }

        return false;
    }

    /* check if message has any ClamAssassin fields with yes values */
    public boolean isVirus()
    {
        CBufferWrapper zCLine;
        Matcher zMatcher;

        if (null != zXVirusStatuss)
        {
            zCLine = (CBufferWrapper) zXVirusStatuss.get(0);
            zMatcher = MLFieldConstants.XVIRUSSTATUSYESP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                return true;
            }
        }

        return false;
    }

    public String toString()
    {
        return "sender: " + zSender + "\n" + "recipients: " + zRcpts + "\n" + "message: " + zDatas + ", " + zDatas.size();
    }

    /* private methods */
    /* decode bytes to chars */
    private CharBuffer toCharBuffer(ByteBuffer zLine)
    {
        CharBuffer zCBLine;

        zLine.rewind();
        try
        {
            zCBLine = MLLine.toCharBuffer(zDecoder, zLine);
        }
        catch (CharacterCodingException e)
        {
            zLog.warn("line contains non-ascii characters; stripping non-ascii characters from line: " + zLine + ", " + e);
            zCBLine = CharBuffer.wrap(MVChar.stripNonASCII(zLine));
        }

        return zCBLine;
    }

    /* decode string to bytes */
    private ByteBuffer toByteBuffer(String zStr) throws ModifyException
    {
        ByteBuffer zLine;
        try
        {
            zLine = MLLine.toByteBuffer(zEncoder, zStr);
        }
        catch (CharacterCodingException e)
        {
            throw new ModifyException("Unable to encode string: \"" + zStr + "\": " + e);
        }

        return zLine;
    }

    /* search for and identify interesting fields
     *
     * note that each buffer contains one and only one text line and
     * each text line ends with EOLINE
     *
     * LWSP = linear-white-space = space or horizontal tab
     *
     * RFC 822:
     *  Each header field can be viewed as a single, logical  line  of
     *  ASCII  characters,  comprising  a field-name and a field-body.
     *  For convenience, the field-body  portion  of  this  conceptual
     *  entity  can be split into a multiple-line representation; this
     *  is called "folding".  The general rule is that wherever  there
     *  may  be  linear-white-space  (NOT  simply  LWSP-chars), a CRLF
     *  immediately followed by AT LEAST one LWSP-char may instead  be
     *  inserted.
     */
    private void parseHdr() throws ParseException
    {
        Matcher zFOLDEDMatcher;
        CBufferWrapper zCLine;

        int iLastType = Constants.NONE_IDX;

        /* header field
         * - one field per text line or
         *   if folded, one field spans multiple text lines
         */
        for (Iterator zIter = zHdrs.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();

            /* check for LWSP at start of text line
             * - indicates that text line is continuation of last field
             */
            zFOLDEDMatcher = Constants.FOLDEDP.matcher(zCLine);
            if (false == zFOLDEDMatcher.find())
            {
                /* field is not folded
                 * so this must be new field
                 * (and we no longer care about last field that we found)
                 */
                iLastType = Constants.NONE_IDX;
            }
            else /* FOLDEDMatcher is true */
            {
                if (Constants.NONE_IDX == iLastType)
                {
                    /* if we don't care about last field,
                     * then we don't care about any folded fields that follow
                     */
                    continue;
                }
                /* else last field is folded
                 * - add this text line to last field
                 */
            }

            /* identify message header field */
            iLastType = identify(zCLine, iLastType);
        }

        return;
    }

    /* identify message header field if we are interested in it and
     * return message header field type
     *
     * for most field types,
     * only one copy may exist for each message header
     * so once field type has been set,
     * we do not allow subsequent fields of the same type
     */
    private int identify(CBufferWrapper zCLine, int iLastType) throws ParseException
    {
        /* if type is specified, this line is part of folded field */
        switch(iLastType)
        {
        default:
            throw new ParseException("Unable to parse header of this message because the header contains an unknown field type: \"" + zCLine + "\"");

        case Constants.NONE_IDX:
            break; /* not folded field - identify new field below */

        case Constants.RELAY_IDX:
            zRelays.add(zCLine);
            return Constants.RELAY_IDX;

        case Constants.FROM_IDX:
            zFroms.add(zCLine);
            return Constants.FROM_IDX;

        case Constants.HSENDER_IDX:
            zHSenders.add(zCLine);
            return Constants.HSENDER_IDX;

        case Constants.DATE_IDX:
            zDates.add(zCLine);
            return Constants.DATE_IDX;

        case Constants.TOLIST_IDX:
            zToLists.add(zCLine);
            return Constants.TOLIST_IDX;

        case Constants.CCLIST_IDX:
            zCcLists.add(zCLine);
            return Constants.CCLIST_IDX;

        case Constants.BCCLIST_IDX:
            zBccLists.add(zCLine);
            return Constants.BCCLIST_IDX;

        case Constants.ORIGINATOR_IDX:
            zReplyTos.add(zCLine);
            return Constants.ORIGINATOR_IDX;

        case Constants.SUBJECT_IDX:
            zSubjects.add(zCLine);
            return Constants.SUBJECT_IDX;

        case Constants.CONTENTTYPE_IDX:
            zContentTypes.add(zCLine);
            return Constants.CONTENTTYPE_IDX;

        case Constants.CONTENTENCODE_IDX:
            zContentEncodes.add(zCLine);
            return Constants.CONTENTENCODE_IDX;

        case Constants.XSPAMFLAG_IDX:
            zXSpamFlags.add(zCLine);
            return Constants.XSPAMFLAG_IDX;

        case Constants.XSPAMSTATUS_IDX:
            zXSpamStatuss.add(zCLine);
            return Constants.XSPAMSTATUS_IDX;

        case Constants.XVIRUSSTATUS_IDX:
            zXVirusStatuss.add(zCLine);
            return Constants.XVIRUSSTATUS_IDX;

        case Constants.XVIRUSREPORT_IDX:
            zXVirusReports.add(zCLine);
            return Constants.XVIRUSREPORT_IDX;
        }

        Matcher zMatcher;

        zMatcher = MLFieldConstants.RELAYP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null == zRelays)
            {
                /* multiple relay header fields may appear in each message */
                zRelays = new ArrayList();
            }

            zRelays.add(zCLine);
            return Constants.RELAY_IDX;
        } /* else not RELAY */

        zMatcher = MLFieldConstants.FROMP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zFroms)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zFroms = new ArrayList();
            zFroms.add(zCLine);
            return Constants.FROM_IDX;
        } /* else not FROM */

        zMatcher = MLFieldConstants.HSENDERP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zHSenders)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zHSenders = new ArrayList();
            zHSenders.add(zCLine);
            return Constants.HSENDER_IDX;
        } /* else not FROM */

        zMatcher = MLFieldConstants.DATEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zDates)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zDates = new ArrayList();
            zDates.add(zCLine);
            return Constants.FROM_IDX;
        } /* else not DATE */

        zMatcher = MLFieldConstants.TOLISTP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zToLists)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zToLists = new ArrayList();
            zToLists.add(zCLine);
            return Constants.TOLIST_IDX;
        } /* else not TOLIST */

        zMatcher = MLFieldConstants.CCLISTP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zCcLists)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zCcLists = new ArrayList();
            zCcLists.add(zCLine);
            return Constants.CCLIST_IDX;
        } /* else not CCLIST */

        zMatcher = MLFieldConstants.BCCLISTP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zBccLists)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zBccLists = new ArrayList();
            zBccLists.add(zCLine);
            return Constants.BCCLIST_IDX;
        } /* else not BCCLIST */

        zMatcher = MLFieldConstants.ORIGINATORP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zReplyTos)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zReplyTos = new ArrayList();
            zReplyTos.add(zCLine);
            return Constants.ORIGINATOR_IDX;
        } /* else not ORIGINATOR */

        zMatcher = MLFieldConstants.SUBJECTP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zSubjects)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zSubjects = new ArrayList();
            zSubjects.add(zCLine);
            return Constants.SUBJECT_IDX;
        } /* else not SUBJECT */

        zMatcher = MLFieldConstants.CONTENTTYPEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zContentTypes)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zContentTypes = new ArrayList();
            zContentTypes.add(zCLine);
            return Constants.CONTENTTYPE_IDX;
        } /* else not CONTENTTYPE */

        zMatcher = MLFieldConstants.CONTENTENCODEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zContentEncodes)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zContentEncodes = new ArrayList();
            zContentEncodes.add(zCLine);
            return Constants.CONTENTENCODE_IDX;
        } /* else not CONTENTENCODE */

        zMatcher = MLFieldConstants.XSPAMFLAGP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zXSpamFlags)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zXSpamFlags = new ArrayList();
            zXSpamFlags.add(zCLine);
            return Constants.XSPAMFLAG_IDX;
        } /* else not XSPAMFLAG */

        zMatcher = MLFieldConstants.XSPAMSTATUSP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zXSpamStatuss)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zXSpamStatuss = new ArrayList();
            zXSpamStatuss.add(zCLine);
            return Constants.XSPAMSTATUS_IDX;
        } /* else not XSPAMSTATUS */

        zMatcher = MLFieldConstants.XVIRUSSTATUSP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zXVirusStatuss)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zXVirusStatuss = new ArrayList();
            zXVirusStatuss.add(zCLine);
            return Constants.XVIRUSSTATUS_IDX;
        } /* else not XVIRUSSTATUS */

        zMatcher = MLFieldConstants.XVIRUSREPORTP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zXVirusReports)
            {
                throw new ParseException("Unable to parse header of this message because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zXVirusReports = new ArrayList();
            zXVirusReports.add(zCLine);
            return Constants.XVIRUSREPORT_IDX;
        } /* else not XVIRUSREPORT */

        /* we don't care about this message header field */
        return Constants.NONE_IDX;
    }

    private void parseBody() throws ParseException
    {
        decodeBody();

        String zCTBStr = getCTBoundary(zContentTypes);
        if (null != zCTBStr)
        {
            /* process MIME body */
            if (null == zMIMEBody)
            {
                zMIMEBody = new MIMEBody();
            }
            zMIMEBody.parse(zCTBStr, zBodys);

            return;
        }
        /* else this message has no MIME boundary value and
         * thus, it has no MIME body
         * (if this message has no MIME body,
         * we are not interested in its body and
         * will not parse body)
         */

        return;
    }

    /* if message body is encoded, then decode it so that we can process it */
    private void decodeBody()
    {
        ByteBuffer zName;
        ByteBuffer zStart;
        ByteBuffer zEnd;
        int iLineSz;

        if (true == MLFieldConstants.isEncoding(Constants.BASE64P, zContentEncodes))
        {
            iEncodedType = Constants.BASE64_TYPE;

            zName = ByteBuffer.wrap(BASE64_FNAMEBA, BASE64_FNAMEBA.length, 0);
            zName.rewind();

            zStart = Constants.getBase64Start(zName);
            zEnd = Constants.getBase64End();

            iLineSz = Constants.BASE64_LINESZ;
        }
        else if (true == MLFieldConstants.isEncoding(Constants.UUENCODEP, zContentEncodes))
        {
            iEncodedType = Constants.UUENCODE_TYPE;

            zName = ByteBuffer.wrap(UUENCODE_FNAMEBA, UUENCODE_FNAMEBA.length, 0);
            zName.rewind();

            zStart = Constants.getUuencodeStart(zName);
            zEnd = Constants.getUuencodeEnd();

            iLineSz = Constants.UUENCODE_LINESZ;
        }
        else
        {
            zLog.debug("This message body is not encoded in base64 or uuencode format");
            return;
        }

        ArrayList zRawBodys = MLLine.toBuffer(zBodys, true);

        File zDFile = null;
        try
        {
            zDFile = MLLine.decodeBufs(zStart, zRawBodys, zEnd, Constants.TMP_FNAME, iLineSz);
        }
        catch (IOException e)
        {
            zLog.error("Unable to scan contents of this message: " + e);
            /* fall through */
        }
        catch (InterruptedException e)
        {
            zLog.error("Unable to scan contents of this message: " + e);
            /* fall through */
        }

        /* if we cannot decode this message, then ignore for now */
        if (null == zDFile)
        {
            /* restore this message body */
            MLLine.fromBuffer(zRawBodys, zBodys, true);
            iEncodedType = Constants.NOENCODE_TYPE;

            zLog.error(zContentEncodes);
            zLog.error(zBodys);

            zName = null; /* release, let GC process */
            zStart = null; /* release, let GC process */
            zEnd = null; /* release, let GC process */
            zRawBodys.clear(); /* release, let GC process */
            zRawBodys = null; /* release, let GC process */

            return;
        }

        ArrayList zDecodedBodys = null;
        try
        {
            zDecodedBodys = MLLine.readFile(zEncoder, zDFile.getAbsolutePath());
        }
        catch (IOException e)
        {
            zLog.error("Unable to read this message (after decoding it): " + e);
            /* fall through */
        }

        zDFile.delete(); /* delete decoded file */
        zDFile = null;

        /* if we cannot read this message, then ignore for now */
        if (null == zDecodedBodys)
        {
            /* restore this message body */
            MLLine.fromBuffer(zRawBodys, zBodys, true);
            iEncodedType = Constants.NOENCODE_TYPE;

            zLog.error(zContentEncodes);
            zLog.error(zBodys);

            zName = null; /* release, let GC process */
            zStart = null; /* release, let GC process */
            zEnd = null; /* release, let GC process */
            zRawBodys.clear(); /* release, let GC process */
            zRawBodys = null; /* release, let GC process */

            return;
        }

        /* we are not going to re-encode message after we are done
         * so strip encoding info from message header and
         * swap encoded message body with decoded message body
         */
        if (true == zHdrs.removeAll(zContentEncodes))
        {
            /* replace original message data */
            zDatas.clear();
            zDatas.addAll(zHdrs);
            zDatas.addAll(zDecodedBodys);

            zBodys.clear();
            zBodys = zDecodedBodys;
        }

        zName = null; /* release, let GC process */
        zStart = null; /* release, let GC process */
        zEnd = null; /* release, let GC process */
        zRawBodys.clear(); /* release, let GC process */
        zRawBodys = null; /* release, let GC process */

        return;
    }

    /* rebuild message data (recombine text components into buffers) and
     * return as buffer list
     * - buffers may be returned as ByteBuffers (true) or
     *   CBufferWrappers (false)
     */
    private ArrayList unparse(boolean bByteBuffer)
    {
        ArrayList zDataSrc = new ArrayList();
        unparse(zDataSrc, bByteBuffer);

        return zDataSrc;
    }

    private void setPTO()
    {
        if (null == zPTO)
        {
            zPTO = new MLMessagePTO(this);
        }
        else
        {
            zPTO.update();
        }

        bIsReady = true;
        return;
    }

    /* swap original value with replacement value in this text line */
    private void replace(CBufferWrapper zOrgCLine, String zReplStr, Matcher zMatcher) throws ModifyException
    {
        ByteBuffer zLine = zOrgCLine.getSrc();
        CharBuffer zCBLine = toCharBuffer(zLine);

        /* replace all matching text within char buffer and
         * get replacement result in form of string
         */
        zMatcher.reset(zCBLine);
        String zNewStr = zMatcher.replaceAll(zReplStr);

        /* convert replacement string result into new buffer */
        ByteBuffer zNewSrcLine = toByteBuffer(zNewStr);

        /* search message data for original source buffer and
         * swap it out with new one that we just built
         */
        CBufferWrapper zCLine;
        for (Iterator zIter = zHdrs.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            if (zLine == zCLine.getSrc())
            {
                /* replace old source buffer with new source buffer */
                zCLine.renew(zNewSrcLine);
                break;
            }
        }

        return;
    }

    /* replace text in message body */
    private void replaceBody(ArrayList zReplList, Matcher zMatcher) throws ModifyException
    {
        zLog.debug("replace body");
        if (null == zBodys ||
            true == zBodys.isEmpty())
        {
            return;
        }
        else if (null != zMIMEBody)
        {
            /* message body is embedded in this MIME part */
            zMsgBodyStartMP.replace(zReplList);
            return;
        }
        /* else message body has no MIME body */

        zBodys.clear(); /* truncate message body */

        /* append duplicate replacement text to this MIME part
         * - we must duplicate replacement text and
         *   cannot use multiple references to same replacement text because
         *   we may use replacement text multiple times in same message and
         *   we cannot rewind replacement text
         *   after we send replacement text (in array of lines) to Smith
         */
        CBufferWrapper zCLine;

        for (Iterator zIter = zReplList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zBodys.add(new CBufferWrapper(zCLine.get().duplicate()));
        }

        return;
    }

    /* Content-Type boundary can contain any combination of these characters:
     * DIGIT, ALPHA, "'", "(", ")", "+", "_", ",", "-", ".", "/", ":", "=", "?"
     *
     * Since most of these non-DIGIT and non-ALPHA characters are
     * also Pattern metacharacters,
     * we must mask these metacharacters (by escaping them)
     * in order to use CTBoundary to compile Pattern and
     * use this Pattern to search for CTBoundary matches
     */
    private static String maskMeta(CBufferWrapper zCLine)
    {
        String zStr = zCLine.toString();

        zStr = replaceAll(zStr, SQUOTESTRP, SQUOTELSTR);
        zStr = replaceAll(zStr, OPARENSTRP, OPARENLSTR);
        zStr = replaceAll(zStr, CPARENSTRP, CPARENLSTR);
        zStr = replaceAll(zStr, PLUSSTRP, PLUSLSTR);
        zStr = replaceAll(zStr, COMMASTRP, COMMALSTR);
        zStr = replaceAll(zStr, DOTSTRP, DOTLSTR);
        zStr = replaceAll(zStr, COLONSTRP, COLONLSTR);
        zStr = replaceAll(zStr, QUESTSTRP, QUESTLSTR);

        /* handle most common (and frequent) cases after least common cases
         * (least common cases will involve search with fewer characters)
         */
        zStr = replaceAll(zStr, EQUALSTRP, EQUALLSTR);
        zStr = replaceAll(zStr, DASHSTRP, DASHLSTR);

        return zStr;
    }

    private static String replaceAll(String zOrgStr, Pattern zPattern, String zReplStr)
    {
        Matcher zMatcher = zPattern.matcher(zOrgStr);
        if (true == zMatcher.find())
        {
            return zMatcher.replaceAll(zReplStr);
        }

        return zOrgStr;
    }
}
