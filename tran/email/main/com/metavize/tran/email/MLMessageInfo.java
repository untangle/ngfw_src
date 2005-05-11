/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.email;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

import com.metavize.tran.util.*;

/**
 * Log e-mail message info.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_MESSAGE_INFO"
 * mutable="false"
 */
public class MLMessageInfo
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(MLMessageInfo.class.getName());

    private final static String TRUNCATED_STR = ",...";
    private final static String COMMA_DELIM = ",";

    //private final static String DIGVAL = "(\\p{Digit}){1,20}+";
    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String MAXSIZE = " SIZE=" + DIGVAL;
    private final static Pattern MAXSIZEP = Pattern.compile(MAXSIZE, Pattern.CASE_INSENSITIVE);

    private final static int COL_SZ = MLHandler.DATASZ;

    /* instance variables */
    private CharsetDecoder zDecoder;

    /* columns */
    private Long zId; /* msg_id */
    private MLHandlerInfo zHdlInfo; /* hdl_id */
    /* SMTP info */
    private String zSender;
    private String zRcpt;
    /* message hdr info */
    private String zFrom;
    private String zToList;
    private String zCcList;
    private String zBccList;
    private String zSubject;

    private int iSize; /* not null */

    private Date zTimeStamp;

    /* constructors */
    public MLMessageInfo() {}

    public MLMessageInfo(MLHandlerInfo zHdlInfo, XMSEnv zEnv, MLMessage zMsg)
    {
        zDecoder = Constants.CHARSET.newDecoder();

        this.zHdlInfo = zHdlInfo;

        if (Constants.SMTP_RID == zHdlInfo.getServerType())
        {
            ArrayList zSenderList = new ArrayList(1);
            zSenderList.add(stripSize(zMsg.getSender()));
            zSender = concatList(zSenderList);
            zRcpt = concatList(zMsg.getRcpt());
        }
        else
        {
            zSender = null;
            zRcpt = null;
        }

        zFrom = concatList(zMsg.getFrom());
        zToList = concatList(zMsg.getToList());
        zCcList = concatList(zMsg.getCcList());
        zBccList = concatList(zMsg.getBccList());

        /* subject fields are rarely folded
         * so rather than use special handling,
         * we'll only log 1st line of each subject field
         */
        ArrayList zList = zMsg.getSubject();
        if (null == zList ||
            true == zList.isEmpty())
        {
            zSubject = null;
        }
        else
        {
            ArrayList zSubjectList = new ArrayList(1);
            zSubjectList.add(zList.get(0));
            zSubject = concatList(zSubjectList);
        }

        iSize = zEnv.getReadDataCt();
        zTimeStamp = new Date();
    }

    /* public methods */
    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return zId;
    }

    private void setId(Long zId)
    {
        this.zId = zId;
        return;
    }

    /**
     * Associate e-mail message info with e-mail handler info.
     *
     * @return e-mail handler info.
     * @hibernate.many-to-one
     * column="HDL_ID"
     * cascade="all"
     */
    public MLHandlerInfo getHdlInfo()
    {
        return zHdlInfo;
    }

    public void setHdlInfo(MLHandlerInfo zHdlInfo)
    {
        this.zHdlInfo = zHdlInfo;
        return;
    }

    /**
     * Size of e-mail message.
     *
     * @return size of e-mail message.
     * @hibernate.property
     * column="SIZE"
     * type="integer"
     * not-null="true"
     */
    public int getSize()
    {
        return iSize;
    }

    public void setSize(int iSize)
    {
        this.iSize = iSize;
    }

    /**
     * Identify SMTP sender.
     * (column length = COL_SIZE)
     *
     * @return SMTP sender.
     * @hibernate.property
     * column="SMTP_SENDER"
     * length="1024"
     */
    public String getSender()
    {
        return zSender;
    }

    public void setSender(String zSender)
    {
        this.zSender = zSender;
        return;
    }

    /**
     * Identify SMTP recipient(s).
     * (column length = COL_SIZE)
     *
     * @return SMTP recipient(s).
     * @hibernate.property
     * column="SMTP_RECIPIENT"
     * length="1024"
     */
    public String getRecipient()
    {
        return zRcpt;
    }

    public void setRecipient(String zRcpt)
    {
        this.zRcpt = zRcpt;
        return;
    }

    /**
     * Identify RFC822 originator.
     * (column length = COL_SIZE)
     *
     * @return RFC822 originator.
     * @hibernate.property
     * column="RFC822_FROM"
     * length="1024"
     */
    public String getFrom()
    {
        return zFrom;
    }

    public void setFrom(String zFrom)
    {
        this.zFrom = zFrom;
        return;
    }

    /**
     * Identify RFC822 To list recipient(s).
     * (column length = COL_SIZE)
     *
     * @return RFC822 To list recipient(s).
     * @hibernate.property
     * column="RFC822_TO_LIST"
     * length="1024"
     */
    public String getToList()
    {
        return zToList;
    }

    public void setToList(String zToList)
    {
        this.zToList = zToList;
        return;
    }

    /**
     * Identify RFC822 Cc list recipient(s).
     * (column length = COL_SIZE)
     *
     * @return RFC822 Cc list recipient(s).
     * @hibernate.property
     * column="RFC822_CC_LIST"
     * length="1024"
     */
    public String getCcList()
    {
        return zCcList;
    }

    public void setCcList(String zCcList)
    {
        this.zCcList = zCcList;
        return;
    }

    /**
     * Identify RFC822 Bcc list recipient(s).
     * (column length = COL_SIZE)
     *
     * @return RFC822 Bcc list recipient(s).
     * @hibernate.property
     * column="RFC822_BCC_LIST"
     * length="1024"
     */
    public String getBccList()
    {
        return zBccList;
    }

    public void setBccList(String zBccList)
    {
        this.zBccList = zBccList;
        return;
    }

    /**
     * Identify RFC822 Subject.
     * (column length = COL_SIZE)
     *
     * @return RFC822 Subject.
     * @hibernate.property
     * column="RFC822_SUBJECT"
     * length="1024"
     */
    public String getSubject()
    {
        return zSubject;
    }

    public void setSubject(String zSubject)
    {
        this.zSubject = zSubject;
        return;
    }

    /**
     * Identify approximate datetime that message was processed.
     *
     * @return datetime of message.
     * @hibernate.property
     * column="TIME_STAMP"
     */
    public Date getTimeStamp()
    {
        return zTimeStamp;
    }

    public void setTimeStamp(Date zTimeStamp)
    {
        this.zTimeStamp = zTimeStamp;
        return;
    }

    /* private methods */
    /* RFC 1870:
     * (4) one optional parameter using the keyword "SIZE" is added to the
     *     MAIL FROM command.  The value associated with this parameter is a
     *     decimal number indicating the size of the message that is to be
     *     transmitted.  The syntax of the value is as follows, using the
     *     augmented BNF notation of [2]:
     *         size-value ::= 1*20DIGIT
     *
     * (5) the maximum length of a MAIL FROM command line is increased by 26
     *     characters by the possible addition of the SIZE keyword and
     *     value
     */
    private CBufferWrapper stripSize(CBufferWrapper zCLine)
    {
        Matcher zMatcher = MAXSIZEP.matcher(zCLine);
        if (true == zMatcher.find())
        {
            ByteBuffer zLine = zCLine.get();

            /* create snapshot of ByteBuffer state */
            int iPosition = zLine.position();
            int iLimit = zLine.limit();

            /* sender only contains one email address followed by SIZE info
             * so we reset limit to truncate SIZE info
             * before we create copy of sender
             */
            zLine.limit(zMatcher.start());
            zLine.rewind();

            ByteBuffer zNewLine = ByteBuffer.allocate(zMatcher.start());
            zNewLine.put(zLine);
            CBufferWrapper zNewCLine = new CBufferWrapper(zNewLine);

            /* restore ByteBuffer state */
            zLine.limit(iLimit);
            zLine.position(iPosition);

            return zNewCLine;
        }

        return zCLine;
    }

    private String concatList(ArrayList zList)
    {
        if (null == zList ||
            true == zList.isEmpty())
        {
            return null;
        }

        String zStr = Constants.EMPTYSTR;

        String zTmp;
        CBufferWrapper zCLine;
        ByteBuffer zLine;
        Matcher zMatcher;
        int iPosition;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zCLine = (CBufferWrapper) zIter.next();

            zMatcher = Constants.ANYCMDP.matcher(zCLine);
            if (true == zMatcher.find())
            {
                zLine = zCLine.get();

                /* create snapshot of ByteBuffer state */
                iPosition = zLine.position();

                /* we found field command so skip over it */
                zLine.position(zMatcher.end());

                try
                {
                    zTmp = MLLine.toString(zDecoder, zLine).trim();
                }
                catch (CharacterCodingException e)
                {
                    zLog.warn("field contains non-ascii characters; stripping non-ascii characters from field: " + zCLine + ", " + e);

                    /* we found field command so skip over it */
                    zLine.position(zMatcher.end());
                    zTmp = MVChar.stripNonASCII(zLine);
                }

                /* restore ByteBuffer state */
                zLine.position(iPosition);
            }
            else
            {
                /* this text is part of folded field
                 * it only contains values (and has no field command)
                 */
                zTmp = (zCLine.toString()).trim();
            }

            if (true == zIter.hasNext())
            {
                zTmp = zTmp.concat(COMMA_DELIM);
            }

            if (COL_SZ < (zTmp.length() + zStr.length()))
            {
                if (COL_SZ >= (TRUNCATED_STR.length() + zStr.length()))
                {
                    /* if we have space for TRUNCATED_STR, add it */
                    zStr = zStr.concat(TRUNCATED_STR);
                }
                zLog.warn("Unable to store all values from this field: " + zStr + " (maximum column size is " + COL_SZ + " chars, truncating to " + zStr.length() + " chars)");
                return zStr;
            }

            zStr = zStr.concat(zTmp);
        }

        return zStr;
    }
}
