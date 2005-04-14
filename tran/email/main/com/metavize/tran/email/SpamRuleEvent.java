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

import java.lang.String;
import java.nio.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * Log e-mail message spam rule (message contains spam) event.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_SPAM_EVT"
 * mutable="false"
 */
public class SpamRuleEvent extends LogEvent
{
    /* constants */
    private static final Logger zLog = Logger.getLogger(SpamRuleEvent.class.getName());

    private final static String ZERO_SCORE = "0.0";
    private final static String TRUNCATED_STR = ",...";

    private final static String HITS = " (hits|score)=";
    private final static String REQUIRED = " required=";
    private final static String TESTS = " tests=";
    private final static Pattern HITSP = Pattern.compile(HITS, Pattern.CASE_INSENSITIVE);
    private final static Pattern REQUIREDP = Pattern.compile(REQUIRED, Pattern.CASE_INSENSITIVE);
    private final static Pattern TESTSP = Pattern.compile(TESTS, Pattern.CASE_INSENSITIVE);

    private final static int COL_SZ = MLHandler.READSZ;

    /* class variables */

    /* instance variables */
    private MLMessageInfo zMsgInfo;
    private char zAction;
    private String zScore;
    private String zTests;
    private boolean bIsSpam;

    /* constructors */
    public SpamRuleEvent() {}

    public SpamRuleEvent(MLMessageInfo zMsgInfo, Action zAction, MLMessage zMsg, boolean bIsSpam)
    {
        this.zMsgInfo = zMsgInfo;
        this.zAction = zAction.getKey();
        parseSpamStatus(zMsg.getXSpamStatus());
        this.bIsSpam = bIsSpam;
    }

    /* public methods */
    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="all"
     */
    public MLMessageInfo getMLMessageInfo()
    {
        return zMsgInfo;
    }

    public void setMLMessageInfo(MLMessageInfo zMsgInfo)
    {
        this.zMsgInfo = zMsgInfo;
        return;
    }

    /**
     * Identify result of spam rule action.
     * type="com.metavize.tran.email.ActionUserType"
     *
     * @return result of spam rule action.
     * @hibernate.property
     * column="ACTION"
     * not-null="true"
     */
    public char getAction()
    {
        return zAction;
    }

    public void setAction(char zAction)
    {
        this.zAction = zAction;
        return;
    }

    /**
     * Identify spam score.
     *
     * @return virus.
     * @hibernate.property
     * column="SCORE"
     * not-null="true"
     */
    public String getScore()
    {
        return zScore;
    }

    public void setScore(String zScore)
    {
        this.zScore = zScore;
        return;
    }

    /**
     * Identify list of spam tests.
     * (column length = COL_SIZE)
     *
     * @return spam tests.
     * @hibernate.property
     * column="TESTS"
     * length="1024"
     */
    public String getTests()
    {
        return zTests;
    }

    public void setTests(String zTests)
    {
        this.zTests = zTests;
        return;
    }

    /**
     * Identify (original) e-mail message spam status.
     *
     * @return (original) e-mail message spam status.
     * @hibernate.property
     * column="IS_SPAM"
     * not-null="true"
     */
    public boolean getIsSpam()
    {
        return bIsSpam;
    }

    public void setIsSpam(boolean bIsSpam)
    {
        this.bIsSpam = bIsSpam;
        return;
    }

    /* private methods */
    /* customized search for SpamAssassin 2.64/3.0.2 X-Spam-Status field
     * - format is not guaranteed for other SpamAssassin versions
     */
    private void parseSpamStatus(ArrayList zList)
    {
        if (null == zList ||
            true == zList.isEmpty())
        {
            zScore = ZERO_SCORE;
            zTests = null;
            return;
        }

        String zStr = ((CBufferWrapper) zList.get(0)).toString();

        Matcher zMatcher = HITSP.matcher(zStr);
        if (false == zMatcher.find())
        {
            zLog.error("Unable to locate Spam score in X-Spam-Status field: " + zStr);
            zScore = ZERO_SCORE;
            zTests = null;
            return;
        }

        int iStart = zMatcher.end();

        int iEnd;

        /* if present, "required" immediately follows "hits"
         * if not present, we assume that "required" is on another line
         */
        zMatcher = REQUIREDP.matcher(zStr);
        if (false == zMatcher.find())
        {
            iEnd = zStr.length();
        }
        else
        {
            iEnd = zMatcher.start();
        }

        zScore = zStr.substring(iStart, iEnd);
        zTests = Constants.EMPTYSTR;

        boolean bFoundTests = false;

        String zTmp;

        for (Iterator zIter = zList.iterator(); true == zIter.hasNext(); )
        {
            zStr = ((CBufferWrapper) zIter.next()).toString();
            if (false == bFoundTests)
            {
                zMatcher = TESTSP.matcher(zStr);
                if (false == zMatcher.find())
                {
                    continue; /* check next text line */
                }

                bFoundTests = true;

                iStart = zMatcher.end();
                /* must be <= COL_SZ so we don't need to validate */
                zTests = zStr.substring(iStart).trim();
            }
            else
            {
                zTmp = zStr.trim();
                if (COL_SZ < (zTmp.length() + zTests.length()))
                {
                    if (COL_SZ >= (TRUNCATED_STR.length() + zTests.length()))
                    {
                        /* if we have space for TRUNCATED_STR, add it */
                        zTests = zTests.concat(TRUNCATED_STR);
                    }
                    zLog.warn("Unable to store all Spam tests from X-Spam-Status field: " + zStr + " (maximum column size is " + COL_SZ + " chars, truncating to " + zTests.length() + " chars)");
                    return;
                }

                zTests = zTests.concat(zTmp);
            }
        }

        return;
    }
}
