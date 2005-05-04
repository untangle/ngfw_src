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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
            zLog.error("Unable to create temp file: " + e);
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
                break; /* fall through */
            }
            catch (Exception e)
            {
                zLog.error("Unable to write: " + zCLine + " to file (dir id = " + iExcepDir + "): " + e);
                break; /* fall through */
            }
        }

        try
        {
            zFileW.close();
        }
        catch (IOException e)
        {
            zLog.error("Unable to close temp file: " + zFile.getAbsolutePath() + ", " + e);
            /* fall through */
        }

        zFile.setReadOnly();

        return zFile.getAbsolutePath();
    }

    public static File decodeBufs(ByteBuffer zStart, ArrayList zList, ByteBuffer zEnd, String zFNameStr, int iLineSz) throws IOException, InterruptedException
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

            zLine.rewind();
            if (false == zLine.hasRemaining())
            {
                continue; /* for; line is empty */
            }

            /* if CR or LF is present _at end of_ any line,
             * ignore CR or LF
             * (we do not ignore CR or LF that are present _within_ line)
             */
            for (iIdx = iLimit - 1; 0 < iIdx; iIdx--)
            {
                bChar = zLine.get(iIdx);
                if ((byte) '\r' == bChar || (byte) '\n' == bChar)
                {
                    continue; /* for */
                }

                iIdx++;
                break; /* for */
            }

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
            /* we cannot determine why uudecode failed */
            zFileOut.delete(); /* delete decoded file */
            zLog.error("Unable to decode encoded data located in this message: " + zFNameStr + ", uudecode exit code: " + iExitCode);
            return null;
        }
    }

    /* opens filename,
     * reads file data into ByteBuffers,
     * adds ByteBuffers to list, and
     * returns list
     * - hopefully, file is small in size
     */
    public static ArrayList readFile(CharsetEncoder zEncoder, String zFileName) throws IOException
    {
        FileReader zFileR;

        try
        {
            zFileR = new FileReader(zFileName);
        }
        catch (FileNotFoundException e)
        {
            zLog.error("Unable to find file to read: " + e);
            return null;
        }

        BufferedReader zBufferedR = new BufferedReader(zFileR);
        ArrayList zList = new ArrayList();

        String zReadStr;
        CBufferWrapper zCLine;
        ByteBuffer zLine;

        try
        {
            /* read file data as separate lines of Strings and
             * using default charset,
             * encode each String into ByteBuffer
             */
            while (true == zBufferedR.ready())
            {
                zReadStr = zBufferedR.readLine() + Constants.PCRLF;

                try
                {
                    zLine = toByteBuffer(zEncoder, zReadStr);
                }
                catch (CharacterCodingException e)
                {
                    zLog.error("Unable to encode line: " + zReadStr + ", in file: " + zFileName);
                    zList.clear(); /* release, let GC process */
                    zList = null; /* release, let GC process */
                    break; /* while; fall through */
                }

                zCLine = new CBufferWrapper(zLine);
                zList.add(zCLine);
            }
        }
        catch (IOException e)
        {
            zLog.error("Unable to read file: " + zFileName + ", " + e);
            zList.clear(); /* release, let GC process */
            zList = null; /* release, let GC process */
            /* fall through */
        }

        zBufferedR.close();
        zFileR.close();
        return zList;
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
