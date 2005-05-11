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
import java.lang.Exception;
import java.nio.*;
import java.nio.charset.CharacterCodingException;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.tran.util.CBufferWrapper;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

/* required - if MIME part exists, then object must exist
 * optional - if MIME part exists, then object may or may not exist
 */
public class MIMEPart
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MIMEPart.class.getName());

    private final static String FILENAME = "FILENAME=";
    private final static String NAME = "NAME=";
    private final static Pattern FILENAMEP = Pattern.compile(FILENAME, Pattern.CASE_INSENSITIVE);
    private final static Pattern NAMEP = Pattern.compile(NAME, Pattern.CASE_INSENSITIVE);

    private final static String QUOTE = "\"";
    private final static Pattern QUOTEP = Pattern.compile(QUOTE);

    /* class variables */

    /* instance variables */
    private ArrayList zHdrs = null; /* MIME header - required */
    private ArrayList zBodys = null; /* MIME body - optional */

    /* variables (that follow this line) represent views of above variables */

    private Object zNested = null;

    /* these fields can be folded */
    private ArrayList zContentTypes = null;
    private ArrayList zContentDispositions = null;
    /* if MIME part contains file attachment,
     * Content-Type contains name and
     * Content-Disposition contain filename
     * which e-mail readers use inconsistently
     * so combine type and disposition
     * - separate them later if future testing shows need to do so
     */
    private ArrayList zContentTDs = null; /* combines type and disposition */
    private ArrayList zContentEncodes = null;

    /* these lists are collections of all interesting fields
     * from this MIME part and its nested objects
     * (such as nested messages and nested MIME body)
     */
    private ArrayList zAllTDs = null; /* combines type and disposition */
    private ArrayList zAllCEs = null;

    /* constructors */
    public MIMEPart() {}

    /* public methods */
    public void setHdr()
    {
        if (null == zHdrs)
        {
            zHdrs = new ArrayList();
        }
        return;
    }

    public void setHdr(CBufferWrapper zCLine)
    {
        if (null == zHdrs)
        {
            zHdrs = new ArrayList();
        }
        zHdrs.add(zCLine);
        return;
    }

    public void addHdr(CBufferWrapper zCLine)
    {
        zHdrs.add(zCLine);
        return;
    }

    public void setBody()
    {
        if (null == zBodys)
        {
            zBodys = new ArrayList();
        }
        return;
    }

    public void setBody(CBufferWrapper zCLine)
    {
        if (null == zBodys)
        {
            zBodys = new ArrayList();
        }
        zBodys.add(zCLine);
        return;
    }

    public void addBody(CBufferWrapper zCLine)
    {
        zBodys.add(zCLine);
        return;
    }

    /* get header */
    public ArrayList getHdr()
    {
        return zHdrs;
    }

    /* get raw body (which may contain nested object) */
    public ArrayList getRawBody()
    {
        return zBodys;
    }

    /* get body (if body does not contain nested object) */
    public ArrayList getBody()
    {
        if (null == zNested)
        {
            return zBodys;
        }

        return null;
    }

    /* get nested object */
    public Object getNested()
    {
        return zNested;
    }

    public ArrayList getPTO(int iFieldType)
    {
        switch(iFieldType)
        {
        case Constants.MIMECONTENTTYPE_IDX:
            return zAllTDs;

        case Constants.MIMECONTENTENCODE_IDX:
            return zAllCEs;

        default:
            return null;
        }
    }

    public ArrayList getContentType()
    {
        return zContentTypes;
    }

    public ArrayList getContentDisposition()
    {
        return zContentDispositions;
    }

    public ArrayList getContentEncode()
    {
        return zContentEncodes;
    }

    public void reset()
    {
        if (null != zContentTypes)
        {
            zContentTypes.clear();
            zContentTypes = null;
        }

        if (null != zContentDispositions)
        {
            zContentDispositions.clear();
            zContentDispositions = null;
        }

        if (null != zContentTDs)
        {
            zContentTDs.clear();
            zContentTDs = null;
        }

        if (null != zContentEncodes)
        {
            zContentEncodes.clear();
            zContentEncodes = null;
        }

        if (null != zAllTDs)
        {
            zAllTDs.clear();
            zAllTDs = null;
        }

        if (null != zAllCEs)
        {
            zAllCEs.clear();
            zAllCEs = null;
        }

        if (null != zHdrs)
        {
            zHdrs.clear();
        }

        if (null != zBodys)
        {
            zBodys.clear();
        }

        return;
    }

    public void flush()
    {
        if (null != zHdrs)
        {
            zHdrs.clear();
            zHdrs = null; /* we don't want to repeat this during reset */
        }

        if (null != zBodys)
        {
            zBodys.clear();
            zBodys = null; /* we don't want to repeat this during reset */
        }

        flushNested();
        reset();

        return;
    }

    public void parseHdr() throws ParseException
    {
        Matcher zFOLDEDMatcher;
        CBufferWrapper zCLine;

        int iLastType = Constants.NONE_IDX;
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
                 * so we don't care about last field
                 * that we found
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

            /* identify MIME part header field */
            iLastType = identify(zCLine, iLastType);
        }

        /* we won't always know if this MIME part contains any nested object
         * until we parse header or body of this MIME part
         */

        zAllTDs = new ArrayList();
        zAllCEs = new ArrayList();

        if (null != zContentTypes)
        {
            zContentTDs = new ArrayList();

            zContentTDs.addAll(zContentTypes);
            zAllTDs.addAll(zContentTypes);
        }

        if (null != zContentDispositions)
        {
            if (null == zContentTDs)
            {
                zContentTDs = new ArrayList();
            }

            zContentTDs.addAll(zContentDispositions);
            zAllTDs.addAll(zContentDispositions);
        }

        if (null != zContentEncodes)
        {
            zAllCEs.addAll(zContentEncodes);
        }

        return;
    }

    public void parseBody() throws ParseException
    {
        if (null == zBodys)
        {
            throw new ParseException("Unable to parse MIME part body because this MIME part contains a header that is not properly terminated.");
        }
        else if (null == zContentTypes ||
                 true == zContentTypes.isEmpty())
        {
            throw new ParseException("Unable to parse MIME part body because this MIME part contains a header that does not contain a Content-Type field.");
        }

        if (true == isMsgNested(zContentTypes))
        {
            /* this message contains nested message */
            MLMessage zMsg = new MLMessage();
            ArrayList zMsgDatas = zMsg.getData();
            zMsgDatas.addAll(zBodys);
            zMsg.parse(true);

            MIMEBody zTmpMIMEBody = zMsg.getMIMEBody();
            if (null != zTmpMIMEBody)
            {
                addPTO(zTmpMIMEBody);
            }

            zNested = (Object) zMsg;
        }
        else
        {
            String zCTBoundary = MLMessage.getCTBoundary(zContentTypes);
            if (null != zCTBoundary)
            {
                /* this message contains nested MIME body */
                MIMEBody zMIMEBody = new MIMEBody();
                zMIMEBody.parse(zCTBoundary, zBodys);
                addPTO(zMIMEBody);

                zNested = (Object) zMIMEBody;
            }
        }
        /* else this MIME part has no rfc822 message attribute or
         * MIME boundary value and
         * thus, it has no nested message or
         * nested MIME body, respectively
         * (if this MIME part has no nested object,
         * we are not interested in its body)
         */

        if (0 == zAllTDs.size())
        {
            zAllTDs = null;
        }

        if (0 == zAllCEs.size())
        {
            zAllCEs = null;
        }

        return;
    }

    /* unparse reverses parseHdr and parseBody */
    public void unparse(ArrayList zDataSrc, boolean bByteBuffer)
    {
        /* add MIME part header */
        zDataSrc.addAll(MLLine.toBuffer(zHdrs, bByteBuffer));

        if (null == zNested)
        {
            if (null != zBodys)
            {
                /* add MIME part body */
                zDataSrc.addAll(MLLine.toBuffer(zBodys, bByteBuffer));
            }

            return;
        }
        /* else MIME part body has nested object - add nested object */

        if (true == (zNested instanceof MLMessage))
        {
            MLMessage zMsg = (MLMessage) zNested;
            zMsg.unparse(zDataSrc, bByteBuffer);
        }
        else if (true == (zNested instanceof MIMEBody))
        {
            MIMEBody zMIMEBody = (MIMEBody) zNested;
            zMIMEBody.unparse(zDataSrc, bByteBuffer);
        }

        return;
    }

    /* replace header and body of MIME part and nested objects */
    public void replace(int iFieldType, ArrayList zReplacement, Matcher zMatcher) throws ModifyException
    {
        /* search MIME part header
         * - if we find match in MIME part header,
         *   replace entire MIME part
         */
        CBufferWrapper zCLine;

        ArrayList zList = getHdrField(iFieldType);
        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zMatcher.reset(zCLine);
            if (true == zMatcher.find())
            {
                /* swap MIME part header and body with replacement file text */
                replace(zReplacement);
                return;
            }
        }

        if (null == zNested)
        {
            return;
        }
        /* else MIME part body has nested object - search nested object */

        MIMEBody zMIMEBody;

        if (true == (zNested instanceof MLMessage))
        {
            MLMessage zMsg = (MLMessage) zNested;
            /* note that
             * - nested message may contain MIME body
             * - if we find any matches in MIME part header
             *   of MIME body of nested message,
             *   we replace MIME parts
             *   (we do not match message header fields and
             *    do not replace message)
             */
            zMIMEBody = zMsg.getMIMEBody();
        }
        else if (true == (zNested instanceof MIMEBody))
        {
            zMIMEBody = (MIMEBody) zNested;
        }
        else
        {
            zMIMEBody = null;
        }

        if (null != zMIMEBody)
        {
            /* search nested MIME body
             * - if we find match in MIME part header
             *   (of this nested object),
             *   replace associated MIME part
             */
            zMIMEBody.replace(iFieldType, zReplacement, zMatcher);
        }

        return;
    }

    /* replace header and body of MIME part
     * - strip original header and body
     * - add new header and replacement text
     */
    public void replace(Object zReplacement) throws ModifyException
    {
        zLog.debug("replace MIME part");

        /* cache MIME part boundary delimiter/marker */
        CBufferWrapper zCLine = (CBufferWrapper) zHdrs.get(0);

        /* we will modify original MIME part
         * (modification will invalidate all references
         * including any nested object that this MIME part contains)
         * so flush nested object and reset references in this MIME part
         */
        flushNested();
        reset();

        zHdrs.clear(); /* delete original MIME part header */
        zHdrs.add(zCLine); /* restore MIME part boundary delimiter/marker */

        /* this MIME part may not be of type "text/plain"
         * - for simplicity,
         *   we'll always build this MIME part header as type "text/plain"
         *   so that we can attach replacement text as clear text
         */
        zHdrs.add(new CBufferWrapper(Constants.getMIMEHdrPlain()));

        zBodys.clear(); /* delete original MIME part body */

        /* append duplicate replacement text to this MIME part
         * - we must duplicate replacement text and
         *   cannot use multiple references to same replacement text because
         *   we may use replacement text multiple times in same message and
         *   we cannot rewind replacement text
         *   after we send replacement text (in array of lines) to Smith
         */
        if (true == (zReplacement instanceof ArrayList))
        {
            for (Iterator zIter = ((ArrayList) zReplacement).iterator(); true == zIter.hasNext(); )
            {
                zCLine = (CBufferWrapper) zIter.next();
                //zLog.debug("replace MIME part: " + zCLine + ", " + zCLine.get());
                zBodys.add(new CBufferWrapper(zCLine.get().duplicate()));
            }
        }
        else if (true == (zReplacement instanceof ByteBuffer))
        {
            zCLine = new CBufferWrapper(((ByteBuffer) zReplacement).duplicate());
            //zLog.debug("replace MIME part: " + zCLine + ", " + zCLine.get());
            zBodys.add(zCLine);
        }
        else if (true == (zReplacement instanceof CBufferWrapper))
        {
            zCLine = new CBufferWrapper(((CBufferWrapper) zReplacement).get().duplicate());
            //zLog.debug("replace MIME part: " + zCLine + ", " + zCLine.get());
            zBodys.add(zCLine);
        }
        /* we release (and let GC process) after we resend duplicated data */

        return;
    }

    /* scan MIME part and nested objects */
    public VirusScannerResult scan(ByteBuffer zReplacement, VirusScanner zScanner, boolean bReplace) throws ModifyException, IOException, InterruptedException
    {
        if (null == zNested)
        {
            File zTmpFile;
            CBufferWrapper zCName;
            ByteBuffer zName;
            ByteBuffer zStart;
            ByteBuffer zEnd;
            int iLineSz;

            if (true == MLFieldConstants.isEncoding(Constants.BASE64P, zContentEncodes))
            {
                zTmpFile = getTmpFile();
                zCName = getFileName(zTmpFile);
                zName = zCName.get();
                zName.rewind();

                zStart = Constants.getBase64Start(zName);
                zEnd = Constants.getBase64End();

                iLineSz = Constants.BASE64_LINESZ;
            }
            else if (true == MLFieldConstants.isEncoding(Constants.UUENCODEP, zContentEncodes))
            {
                zTmpFile = getTmpFile();
                zCName = getFileName(zTmpFile);
                zName = zCName.get();
                zName.rewind();

                zStart = Constants.getUuencodeStart(zName);
                zEnd = Constants.getUuencodeEnd();

                iLineSz = Constants.UUENCODE_LINESZ;
            }
            else
            {
                zLog.debug("This MIME part is not encoded in base64 or uuencode format; bypassing this MIME part");
                return null;
            }

            //CBufferWrapper zCDummy = new CBufferWrapper(null);
            //zLog.debug("encoded file start tag: " + zCDummy.renew(zStart) + ", " + zStart);
            //zLog.debug("encoded file: " + zBodys);
            //zLog.debug("encoded file end tag: " + zCDummy.renew(zEnd) + ", " + zEnd);

            ArrayList zRawBodys = MLLine.toBuffer(zBodys, true);
            String zFileName = zTmpFile.getName();

            File zDFile = null;
            try
            {
                zDFile = MLLine.decodeBufs(zStart, zRawBodys, zEnd, zFileName, iLineSz);
            }
            catch (IOException e)
            {
                zLog.error("This MIME part contains encoded data that could not be decoded; bypassing this MIME part: " + e);
                /* fall through */
            }
            catch (InterruptedException e)
            {
                zLog.error("This MIME part contains encoded data that was not be decoded; bypassing this MIME part: " + e);
                /* fall through */
            }

            /* if we cannot decode file, then ignore for now */
            if (null == zDFile)
            {
                /* restore this MIME part */
                MLLine.fromBuffer(zRawBodys, zBodys, true);

                zLog.error(zContentTDs);
                zLog.error(zContentEncodes);
                zLog.error(zBodys);

                zCName = null; /* release, let GC process */
                zStart = null; /* release, let GC process */
                zEnd = null; /* release, let GC process */
                zRawBodys.clear(); /* release, let GC process */
                zRawBodys = null; /* release, let GC process */

                zTmpFile.delete(); /* delete tmp file */
                zTmpFile = null; /* release, let GC process */

                return null;
            }

            //zLog.debug("MIME part decoded file: " + zDFile.getAbsolutePath());
            VirusScannerResult zScanResult = zScanner.scanFile(zDFile.getAbsolutePath());

            zDFile.delete(); /* delete decoded file */
            zDFile = null; /* release, let GC process */
            zTmpFile.delete(); /* delete tmp file */
            zTmpFile = null; /* release, let GC process */

            if (null == zScanResult)
            {
                /* restore this MIME part */
                MLLine.fromBuffer(zRawBodys, zBodys, true);

                zRawBodys.clear(); /* release, let GC process */
                zStart = null; /* release, let GC process */
                zEnd = null; /* release, let GC process */

                throw new ModifyException("Unable to scan the contents of this MIME part for the presence of a virus");
            }

            if (false == bReplace ||
                true == zScanResult.isClean())
            {
                /* restore this MIME part */
                MLLine.fromBuffer(zRawBodys, zBodys, true);
            }
            else
            {
                /* replace this MIME part */
                String zVirusName = zScanResult.getVirusName();
                ByteBuffer zTmp = ByteBuffer.allocate(zReplacement.limit() + zVirusName.length() + Constants.EOLINEBA.length);
                zReplacement.rewind();
                zTmp.put(zReplacement);
                zTmp.put(zVirusName.getBytes());
                zTmp.put(Constants.EOLINEBA);

                replace(zTmp);
                zTmp = null; /* release, let GC process */
            }

            zCName = null; /* release, let GC process */
            zStart = null; /* release, let GC process */
            zEnd = null; /* release, let GC process */
            zRawBodys.clear(); /* release, let GC process */
            zRawBodys = null; /* release, let GC process */

            //zLog.debug("scanner: " + zScanner + ", result: " + zScanResult);
            return zScanResult;
        }
        /* else MIME part body has nested object - scan nested object */

        MIMEBody zMIMEBody;

        if (true == (zNested instanceof MLMessage))
        {
            MLMessage zMsg = (MLMessage) zNested;
            /* note that
             * - nested message may contain MIME body
             */
            zMIMEBody = zMsg.getMIMEBody();
        }
        else if (true == (zNested instanceof MIMEBody))
        {
            zMIMEBody = (MIMEBody) zNested;
        }
        else
        {
            zMIMEBody = null;
        }

        if (null != zMIMEBody)
        {
            /* scan next nested MIME body */
            return zMIMEBody.scan(zReplacement, zScanner, bReplace);
        }

        return null;
    }

    /* private methods */
    private ArrayList getHdrField(int iFieldType)
    {
        switch(iFieldType)
        {
        case Constants.MIMECONTENTTYPE_IDX:
            return zContentTDs;

        case Constants.MIMECONTENTENCODE_IDX:
            return zContentEncodes;

        default:
            return null;
        }
    }

    private void flushNested()
    {
        if (null == zNested)
        {
            return;
        }

        if (true == (zNested instanceof MLMessage))
        {
            MLMessage zMsg = (MLMessage) zNested;
            zMsg.flush();
        }
        else if (true == (zNested instanceof MIMEBody))
        {
            MIMEBody zMIMEBody = (MIMEBody) zNested;
            zMIMEBody.flush();
        }

        zNested = null;
        return;
    }

    /* identify MIME header field if we are interested in it and
     * return MIME header field type
     */
    private int identify(CBufferWrapper zCLine, int iLastType) throws ParseException
    {
        switch(iLastType)
        {
        default:
            return Constants.NONE_IDX;

        case Constants.NONE_IDX:
            break;

        case Constants.MIMECONTENTTYPE_IDX:
            zContentTypes.add(zCLine);
            return Constants.MIMECONTENTTYPE_IDX;

        case Constants.MIMECONTENTDISPOSITION_IDX:
            zContentDispositions.add(zCLine);
            return Constants.MIMECONTENTDISPOSITION_IDX;

        case Constants.MIMECONTENTENCODE_IDX:
            zContentEncodes.add(zCLine);
            return Constants.MIMECONTENTENCODE_IDX;
        }

        Matcher zMatcher;

        zMatcher = MLFieldConstants.MIMECONTENTTYPEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zContentTypes)
            {
                throw new ParseException("Unable to parse header of this MIME part because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zContentTypes = new ArrayList();
            zContentTypes.add(zCLine);
            return Constants.MIMECONTENTTYPE_IDX;
        } /* else not MIMECONTENTTYPE */

        zMatcher = MLFieldConstants.MIMECONTENTDISPOSITIONP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zContentDispositions)
            {
                throw new ParseException("Unable to parse header of this MIME part because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zContentDispositions = new ArrayList();
            zContentDispositions.add(zCLine);
            return Constants.MIMECONTENTDISPOSITION_IDX;
        } /* else not MIMECONTENTDISPOSITION */

        zMatcher = MLFieldConstants.MIMECONTENTENCODEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            if (null != zContentEncodes)
            {
                throw new ParseException("Unable to parse header of this MIME part because the header contains a field that has been specified more than once: \"" + zCLine + "\"");
            }

            zContentEncodes = new ArrayList();
            zContentEncodes.add(zCLine);
            return Constants.MIMECONTENTENCODE_IDX;
        } /* else not MIMECONTENTENCODE */

        /* we don't care about this MIME part header field */
        return Constants.NONE_IDX;
    }

    /* returns true if MIME part contains rfc822 message attribute */
    private boolean isMsgNested(ArrayList zCTypes)
    {
        Matcher zMatcher;
        CBufferWrapper zCLine;

        for (Iterator zIter = zCTypes.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zMatcher = MLFieldConstants.RFCMESSAGEP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                return true;
            }
        }

        return false;
    }

    /* collect fields of interest from MIME body nested in this MIME part */
    private void addPTO(MIMEBody zMIMEBody)
    {
        ArrayList zTmp = zMIMEBody.getPTO(Constants.MIMECONTENTTYPE_IDX);
        if (null != zTmp)
        {
            zAllTDs.addAll(zTmp);
        }

        zTmp = zMIMEBody.getPTO(Constants.MIMECONTENTENCODE_IDX);
        if (null != zTmp)
        {
            zAllCEs.addAll(zTmp);
        }

        return;
    }

    private File getTmpFile() throws ModifyException
    {
        if (null == zContentTDs)
        {
            throw new ModifyException("Unable to find the name of the file for this MIME part; this MIME part contains a Content-Transfer-Encoding header field but does not contain a Content-Type or Content-Disposition header field");
        }

        CBufferWrapper zCLine;
        Matcher zMatcher;

        CBufferWrapper zCTName = null;
        CBufferWrapper zCDName = null;

        for (Iterator zIter = zContentTDs.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();

            /* note that we search for filename before name
             * because filename pattern includes name pattern
             */
            zMatcher = FILENAMEP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                zCDName = stripQuotes(zCLine, zMatcher.end());
                continue;
            }

            zMatcher = NAMEP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                zCTName = stripQuotes(zCLine, zMatcher.end());
                continue;
            }
        }

        /* we prefer to use filename from MIME Content-Disposition rather than
         * name from MIME Content-Type
         */
        CBufferWrapper zCTmpFile = (null != zCDName ? zCDName : zCTName);

        try
        {
            String zTmpFile;
            if (null == zCTmpFile)
            {
                /* this MIME part contains Content-Type and/or
                 * Content-Disposition header field
                 * but neither specifies name or filename type
                 * so use arbitrary prefix
                 */
                zTmpFile = Constants.TMP_FNAME;
            }
            else
            {
                zTmpFile = zCTmpFile.toString();
            }

            return File.createTempFile(zTmpFile, null);
        }
        catch (IOException e)
        {
            throw new ModifyException("Unable to generate a name for the file in this MIME part: " + e);
        }
    }

    private CBufferWrapper getFileName(File zFile) throws ModifyException
    {
        ByteBuffer zLine;

        try
        {
            zLine = MLLine.toByteBuffer(zFile.getName());
        }
        catch (CharacterCodingException e)
        {
            throw new ModifyException("Unable to find the name of the file for this MIME part; this MIME part contains a Content-Type and/or Content-Disposition header field but neither field specifies a name or filename type: " + e);
        }

        return new CBufferWrapper(zLine);
    }

    private CBufferWrapper stripQuotes(CBufferWrapper zCLine, int iStart)
    {
        CBufferWrapper zCTmp = (CBufferWrapper) zCLine.subSequence(iStart, zCLine.length());
        Matcher zMatcher = QUOTEP.matcher(zCTmp);
        if (true == zMatcher.find())
        {
            iStart = zMatcher.end();   
            CBufferWrapper zCTmp2 = (CBufferWrapper) zCTmp.subSequence(iStart, zCTmp.length());
            zMatcher = QUOTEP.matcher(zCTmp2);
            if (true == zMatcher.find())
            {
                zCTmp = (CBufferWrapper) zCTmp2.subSequence(0, zMatcher.start());
            }
        }

        return zCTmp;
    }

    /* does MIME Content-Transfer-Encoding specify this encoder? */
    private boolean isEncoding(Pattern zEncoderP)
    {
        if (null == zContentEncodes)
        {
            return false;
        }

        CBufferWrapper zCLine;
        Matcher zMatcher;

        for (Iterator zIter = zContentEncodes.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();

            zMatcher = zEncoderP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                return true;
            }
        }

        return false;
    }
}
