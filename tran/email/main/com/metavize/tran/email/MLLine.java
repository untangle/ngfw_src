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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Exception;
import java.lang.InterruptedException;
import java.lang.Process;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.regex.*;
import org.apache.log4j.Logger;

import com.metavize.tran.util.*;

public class MLLine
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MLLine.class.getName());

    private final static int PRC_SUCCESS = 0;
    private final static int PRC_FAILURE = 1;

    /* class variables */

    /* instance variables */

    /* constructors */
    private MLLine() {}

    /* public methods */
    /* copy src list of buffers or text lines into dst list of text lines */
    public static void fromBuffer(ArrayList zSrcList, ArrayList zDstList, boolean bSrcByteBuffer)
    {
        if (true == bSrcByteBuffer)
        {
            CBufferWrapper zCLine;
            ByteBuffer zLine;
            Iterator zDstIter;
            Iterator zSrcIter;

            /* renew CBufferWrappers with new ByteBuffers */
            for (zDstIter = zDstList.iterator(), zSrcIter = zSrcList.iterator(); true == zDstIter.hasNext() && true == zSrcIter.hasNext(); )
            {
                zCLine = (CBufferWrapper) zDstIter.next();
                zLine = (ByteBuffer) zSrcIter.next();
                zCLine.renew(zLine);
                //zLog.debug("src line (replace): " + zCLine + ", " + zLine);
            }

            /* remove extra CBufferWrappers */
            for (; true == zDstIter.hasNext() && false == zSrcIter.hasNext(); )
            {
                zDstIter.next();
                zDstIter.remove();
                //zLog.debug("src line (remove)");
            }

            /* add new CBufferWrappers */
            for ( ; true == zSrcIter.hasNext(); )
            {
                zLine = (ByteBuffer) zSrcIter.next();
                zCLine = new CBufferWrapper(zLine);
                zDstList.add(zCLine);
                //zLog.debug("src line (add): " + zCLine + ", " + zLine);
            }
        }
        else
        {
            zDstList.clear();
            zDstList.addAll(zSrcList);
        }

        return;
    }

    /* copy src list of text lines into new list of buffers or text lines
     * - we do not release src list objects
     */
    public static ArrayList toBuffer(ArrayList zSrcList, boolean bSrcByteBuffer)
    {
        if (true == bSrcByteBuffer)
        {
            ArrayList zNewList = new ArrayList(zSrcList.size());

            CBufferWrapper zCLine;
            ByteBuffer zLine;

            for (Iterator zSrcIter = zSrcList.iterator(); true == zSrcIter.hasNext(); )
            {
                zCLine = (CBufferWrapper) zSrcIter.next();
                zLine = zCLine.get();
                zCLine.renew(null);
                zNewList.add(zLine);
            }

            return zNewList; /* return new list of ByteBuffers */
        }

        return zSrcList; /* return original list of CBufferWrappers */
    }

    /* convert ByteBuffer to CharBuffer */
    public static CharBuffer toCharBuffer(CharsetDecoder zDecoder, ByteBuffer zLine) throws CharacterCodingException
    {
        zDecoder.reset();
        return zDecoder.decode(zLine);
    }

    /* convert ByteBuffer to String */
    public static String toString(CharsetDecoder zDecoder, ByteBuffer zLine) throws CharacterCodingException
    {
        return toCharBuffer(zDecoder, zLine).toString();
    }

    /* convert ByteBuffer to CharBuffer */
    public static CharBuffer toCharBuffer(ByteBuffer zLine) throws CharacterCodingException
    {
        return Constants.CHARSET.newDecoder().decode(zLine);
    }

    /* convert ByteBuffer to String */
    public static String toString(ByteBuffer zLine) throws CharacterCodingException
    {
        return toCharBuffer(zLine).toString();
    }

    /* convert CharBuffer to ByteBuffer */
    public static ByteBuffer toByteBuffer(CharsetEncoder zEncoder, CharBuffer zCBLine) throws CharacterCodingException
    {
        zEncoder.reset();
        ByteBuffer zLine = zEncoder.encode(zCBLine);
        zLine.position(zLine.limit()); /* set position to indicate that ByteBuffer contains data */

        return zLine;
    }

    /* convert String to ByteBuffer */
    public static ByteBuffer toByteBuffer(CharsetEncoder zEncoder, String zStr) throws CharacterCodingException
    {
        return toByteBuffer(zEncoder, CharBuffer.wrap(zStr));
    }

    /* convert CharBuffer to ByteBuffer */
    public static ByteBuffer toByteBuffer(CharBuffer zCBLine) throws CharacterCodingException
    {
        ByteBuffer zLine = Constants.CHARSET.newEncoder().encode(zCBLine);
        zLine.position(zLine.limit()); /* set position to indicate that ByteBuffer contains data */

        return zLine;
    }

    /* convert String to ByteBuffer */
    public static ByteBuffer toByteBuffer(String zStr) throws CharacterCodingException
    {
        return toByteBuffer(CharBuffer.wrap(zStr));
    }

    /* copy contents of list to file in specified exception directory */
    public static String toFile(ArrayList zList, int iExcepDir)
    {
        File zDir = Constants.getDir(iExcepDir);
        if (null == zDir)
        {
            zLog.error("Unable to create file (to save message) because destination directory (" + iExcepDir + ") does not exist.");
            return null;
        }

        File zFile;
        FileWriter zFileW;

        try
        {
            zFile = File.createTempFile(Constants.XMPREFIX, null, zDir);
            zFileW = new FileWriter(zFile);
            //zLog.debug("created file: " + zFile.getAbsolutePath() + ", " + zFileW); /* for debugging */
        }
        catch (IOException e)
        {
            zLog.error("Unable to create file: " + e);
            return null;
        }

        CharsetDecoder zDecoder = Constants.CHARSET.newDecoder();

        CBufferWrapper zCLine;
        ByteBuffer zLine;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();
            zLine = zCLine.get();
            zLine.rewind();

            try
            {
                zFileW.write(toCharBuffer(zDecoder, zLine).array());
                zFileW.flush();
            }
            catch (IOException e)
            {
                zLog.error("Unable to write: " + zCLine + " to file: " + e);
                return zFile.getAbsolutePath();
            }
            catch (Exception e)
            {
                zLog.error("Unable to write: " + zCLine + " to file (dir id = " + iExcepDir + "): " + e);
                return zFile.getAbsolutePath();
            }
        }

        try
        {
            zFileW.close();
        }
        catch (IOException e)
        {
            zLog.error("Unable to close file: " + e);
            return zFile.getAbsolutePath();
        }

        zFile.setReadOnly();

        return zFile.getAbsolutePath();
    }

    public static File decodeBufs(ByteBuffer zStart, ArrayList zList, ByteBuffer zEnd, String zFileName, int iLineSz) throws IOException, InterruptedException
    {
        /* copy encoded data to file */
        File zTmpDir = new File(Constants.BASEPATH);
        File zFileIn = File.createTempFile("ube", null, zTmpDir);
        FileChannel zOutFile = (new FileOutputStream(zFileIn)).getChannel();

        zStart.rewind();
        zOutFile.write(zStart);

        ByteBuffer zWriteLine = ByteBuffer.allocate(iLineSz + 1);
        boolean bCopyMore = false;

        ByteBuffer zLine;
        int iPosition;
        int iLimit;
        int iIdx;
        byte bChar;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zLine = (ByteBuffer) zIter.next();
            iPosition = zLine.position();
            iLimit = zLine.limit();

            /* if CR or LF is present at end of any line,
             * ignore CR or LF
             * (we do not ignore CR or LF that are present within any line)
             */
            for (iIdx = iLimit - 1; 0 < iIdx; iIdx--)
            {
                bChar = zLine.get(iIdx);
                if ((byte) '\r' == bChar || (byte) '\n' == bChar)
                {
                    continue;
                }

                iIdx++;
                break;
            }

            zLine.rewind();
            zLine.limit(iIdx); /* limit copy range */

            while (true)
            {
                bCopyMore = copy(zLine, zWriteLine, iLineSz);
                if (true == bCopyMore)
                {
                    /* done with this buffer; continue to next */
                    break; /* while */
                }

                zWriteLine.rewind();
                zOutFile.write(zWriteLine);

                zWriteLine.clear(); /* reset and recycle */
            }

            /* restore */
            zLine.limit(iLimit);
            zLine.position(iPosition);
        }

        /* write out last line */
        if (true == bCopyMore)
        {
            zWriteLine.put((byte) '\n');

            /* use flip (instead of rewind)
             * since flip sets limit to position
             * before it sets position to 0
             * (zWriteLine may contain partial line so flip is necessary)
             */
            zWriteLine.flip();
            zOutFile.write(zWriteLine);
        }

        zWriteLine = null; /* release; let GC process */

        zEnd.rewind();
        zOutFile.write(zEnd);
        zOutFile.close();

        /* create decoded version of encoded file */
        File zFileOut = File.createTempFile("ubd", null, zTmpDir);
        //zLog.debug("MIME part encoded file: " + zFileIn.getAbsolutePath());
        Process zPrc = Runtime.getRuntime().exec("uudecode -o " + zFileOut.getAbsolutePath() + " " + zFileIn.getAbsolutePath());

        zPrc.waitFor();
        int iExitCode = zPrc.exitValue();

        zFileIn.delete(); /* delete encoded file */
        /* we will delete decoded file later */

        switch(iExitCode)
        {
        case PRC_SUCCESS:
            return zFileOut;

        default:
        case PRC_FAILURE:
            zFileOut.delete(); /* delete decoded file */
            throw new IOException("Unable to decode encoded data located in this MIME part: " + zFileName + ", uudecode exit code: " + iExitCode);
        }
    }

    /* private methods */
    /* copy (at most) up to iCopySz */
    private static boolean copy(ByteBuffer zSrcLine, ByteBuffer zDstLine, int iCopySz)
    {
        int iAvail = zDstLine.remaining() - 1;
        int iSz = (iAvail < iCopySz) ? iAvail : iCopySz;

        int iIdx;

        for (iIdx = 0; 0 < zSrcLine.remaining() && iIdx < iSz; iIdx++)
        {
            zDstLine.put(zSrcLine.get());
        }

        if (iIdx < iSz)
        {
            return true; /* copy more */
        }
        else
        {
            zDstLine.put((byte) '\n');
            return false; /* copy no more */
        }
    }
}
