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

import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.*;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.tran.util.*;
import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

/* required - if MIME body exists, then object must exist
 * optional - if MIME body exists, then object may or may not exist
 */
public class MIMEBody
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MIMEBody.class.getName());

    /* class variables */

    /* instance variables */
    /* array of MIMEPart:
     * MIME Content-Type,
     *      Content-Disposition,
     *      Content-Transfer-Encodings
     * MIME Data
     */
    private ArrayList zMIMEParts; /* required */
    private ArrayList zMIMEPros = null; /* optional */
    private ArrayList zMIMEEpis = null; /* required */

    /* variables (that follow this line) represent views of above variables */

    /* these lists are collections of all interesting fields from MIME parts */
    private ArrayList zAllTDs = null; /* combines type and disposition */
    private ArrayList zAllCEs = null;

    /* constructors */
    public MIMEBody()
    {
        zMIMEParts = new ArrayList();
    }

    /* public methods */
    public ArrayList getParts()
    {
        return zMIMEParts;
    }

    public MIMEPart getPart(int iIdx)
    {
        return (MIMEPart) zMIMEParts.get(iIdx);
    }

    public ArrayList getPrologue()
    {
        return zMIMEPros;
    }

    public ArrayList getEpilogue()
    {
        return zMIMEEpis;
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

    public void reset()
    {
        if (null != zMIMEParts)
        {
            MIMEPart zMIMEPart;
            for (Iterator zIter = zMIMEParts.iterator(); true == zIter.hasNext(); )
            {
                zMIMEPart = (MIMEPart) zIter.next();
                zMIMEPart.reset();
            }

            zMIMEParts.clear();
        }

        if (null != zMIMEPros)
        {
            zMIMEPros.clear();
        }

        if (null != zMIMEEpis)
        {
            zMIMEEpis.clear();
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

        return;
    }

    public void flush()
    {
        reset();

        zMIMEParts = null;
        zMIMEPros = null;
        zMIMEEpis = null;

        return;
    }

    /* RFC 2046:
     *  The Content-Type field for multipart entities requires one
     *  parameter, "boundary". The boundary delimiter line is then
     *  defined as a line consisting entirely of two hyphen characters
     *  ("-", decimal value 45) followed by the boundary parameter value
     *  from the Content-Type header field, optional linear whitespace,
     *  and a terminating CRLF.
     *
     *  The boundary delimiter line following the last body part is a
     *  distinguished delimiter that indicates that no further body parts
     *  will follow.  Such a delimiter line is identical to the previous
     *  delimiter lines, with the addition of two more hyphens after the
     *  boundary parameter value.
     */
    public void parse(String zCTBoundary, ArrayList zList) throws ParseException
    {
        /* build MIME part boundary delimiter/marker and terminator */
        zLog.debug("MIME part boundary: " + zCTBoundary);
        String zMBoundaryMark = MLFieldConstants.MBMARKSTART + zCTBoundary + MLFieldConstants.MBMARKEND;
        String zMBoundaryTerm = MLFieldConstants.MBTERMSTART + zCTBoundary + MLFieldConstants.MBTERMEND;
        Pattern zMBoundaryMarkP = Pattern.compile(zMBoundaryMark);
        Pattern zMBoundaryTermP = Pattern.compile(zMBoundaryTerm);

        ListIterator zLIter;
        Matcher zMatcher;
        CBufferWrapper zCLine;
        CBufferWrapper zCTmp;

        zAllTDs = new ArrayList();
        zAllCEs = new ArrayList();

        Pattern zNullLineP = Constants.getNullLine();
        MIMEPart zMIMEPart = null;
        boolean bHdrDone = false;

        for (zLIter = zList.listIterator(); true == zLIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zLIter.next();
            zMatcher = zMBoundaryMarkP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                if (null != zMIMEPart)
                {
                    zLog.debug("MIME part body done");
                    /* we've identified lines that define MIME part body */
                    zMIMEPart.parseBody();
                    addPTO(zMIMEPart);
                }

                /* new MIME part starts here */
                zMIMEPart = new MIMEPart();
                zMIMEPart.setHdr(zCLine);
                bHdrDone = false;

                zLog.debug("MIME part hdr (new): " + zCLine + ", " + zCLine.get());
                zMIMEParts.add(zMIMEPart);
                continue; /* for loop */
            }
            else if (null == zMIMEPart)
            {
                /* we haven't found any MIME parts yet
                 * so this must be "hidden" MIME part prologue
                 */
                if (null == zMIMEPros)
                {
                    zMIMEPros = new ArrayList();
                }

                zLog.debug("MIME prologue: " + zCLine + ", " + zCLine.get());
                zMIMEPros.add(zCLine);
                continue; /* for loop */
            }
            /* else this buffer belongs to current MIME part */

            /* MIME part may contain multiple NULLLINE but
             * only first one is important
             * - first NULLLINE identifies end of MIME part header
             * - all other NULLLINE are part of MIME part body
             */
            if (false == bHdrDone)
            {
                zMatcher = zNullLineP.matcher(zCLine);
                if (true == zMatcher.find())
                {
                    /* we've identifed lines that define MIME part header
                     * - we add lines that define MIME part body
                     *   to this MIME part later
                     */

                    zLog.debug("MIME part hdr: " + zCLine + ", " + zCLine.get());
                    /* add NULLLINE to MIME part header */
                    zMIMEPart.addHdr(zCLine);
                    zMIMEPart.parseHdr();
                    bHdrDone = true;

                    /* message body starts here */
                    zMIMEPart.setBody();

                    /* we have no more buffers to add to message header */
                    continue; /* for loop */
                }

                zLog.debug("MIME part hdr: " + zCLine + ", " + zCLine.get());
                /* we have another buffer containing header of MIME part */
                zMIMEPart.addHdr(zCLine);
                continue; /* for loop */
            }

            zMatcher = zMBoundaryTermP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                zLog.debug("MIME part body done");
                /* we've identified lines that define last MIME part body */
                zMIMEPart.parseBody();
                addPTO(zMIMEPart);

                zLog.debug("MIME epilogue: " + zCLine + ", " + zCLine.get());
                /* message body contains no more MIME parts */
                if (null == zMIMEEpis)
                {
                    zMIMEEpis = new ArrayList();
                }
                zMIMEEpis.add(zCLine); /* add boundary terminator to epilogue */
                break; /* for loop */
            }

            //zLog.debug("MIME part body: " + zCLine + ", " + zCLine.get());
            /* we have another buffer containing body of MIME part */
            zMIMEPart.addBody(zCLine);
        }

        if (0 == zAllTDs.size())
        {
            zAllTDs = null;
        }

        if (0 == zAllCEs.size())
        {
            zAllCEs = null;
        }

        if (null == zMIMEEpis)
        {
            throw new ParseException("Unable to parse MIME parts of this message because the MIME parts are not properly terminated.");
        }

        /* we've already found all MIME parts
         * so rest of list must be "hidden" MIME part epilogue
         */
        if (true == zLIter.hasNext())
        {
            zMIMEEpis.addAll(zList.subList(zLIter.nextIndex(), zList.size()));
        }

        return;
    }

    /* unparse reverses parse */
    public void unparse(ArrayList zDataSrc, boolean bByteBuffer)
    {
        if (null != zMIMEPros)
        {
            /* MIME prologue */
            zDataSrc.addAll(MLLine.toBuffer(zMIMEPros, bByteBuffer));
        }

        /* MIME parts */
        ArrayList zMPTmp;
        MIMEPart zMIMEPart;

        for (Iterator zIter = zMIMEParts.iterator(); true == zIter.hasNext(); )
        {
            zMIMEPart = (MIMEPart) zIter.next();
            zMIMEPart.unparse(zDataSrc, bByteBuffer);
        }

        if (null != zMIMEEpis)
        {
            /* MIME epilogue */
            zDataSrc.addAll(MLLine.toBuffer(zMIMEEpis, bByteBuffer));
        }

        return;
    }

    /* replace header and body of MIME part that matches specified field */
    public void replace(int iFieldType, ArrayList zReplacement, Matcher zMatcher) throws ModifyException
    {
        MIMEPart zMIMEPart;

        /* search all MIME parts */
        for (Iterator zMPIter = zMIMEParts.iterator(); true == zMPIter.hasNext(); )
        {
            zMIMEPart = (MIMEPart) zMPIter.next();
            zMIMEPart.replace(iFieldType, zReplacement, zMatcher);
        }

        return;
    }

    /* scan body of MIME part and
     * if virus is found and if clean is requested,
     * remove virus from MIME part
     */
    public VirusScannerResult scan(ByteBuffer zReplacement, VirusScanner zScanner, boolean bReplace) throws ModifyException, IOException, InterruptedException
    {
        MIMEPart zMIMEPart;
        VirusScannerResult zScanResult;

        VirusScannerResult zReturnResult = null;

        /* search all MIME parts */
        for (Iterator zMPIter = zMIMEParts.iterator(); true == zMPIter.hasNext(); )
        {
            zMIMEPart = (MIMEPart) zMPIter.next();
            zScanResult = zMIMEPart.scan(zReplacement, zScanner, bReplace);
            //zLog.debug("scan result: " + zScanResult);

            if (null != zScanResult)
            {
                /* we only log 1st result or
                 * 1st infected virus result
                 */
                if (null == zReturnResult ||
                    true == zReturnResult.isClean())
                {
                    zReturnResult = zScanResult;
                    //zLog.debug("return result: " + zReturnResult);
                }
            }
        }

        //zLog.debug("final return result: " + zReturnResult);
        return zReturnResult;
    }

    /* private methods */
    /* collect fields of interest from this MIME part */
    private void addPTO(MIMEPart zMIMEPart)
    {
        ArrayList zTmp = zMIMEPart.getPTO(Constants.MIMECONTENTTYPE_IDX);
        if (null != zTmp)
        {
            zAllTDs.addAll(zTmp);
        }

        zTmp = zMIMEPart.getPTO(Constants.MIMECONTENTENCODE_IDX);
        if (null != zTmp)
        {
            zAllCEs.addAll(zTmp);
        }

        return;
    }
}
