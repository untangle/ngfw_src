/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MLMessagePTO.java,v 1.2 2005/01/25 23:33:13 cng Exp $
 */
package com.metavize.tran.email;

import java.util.*;
//import org.apache.log4j.Logger;

import com.metavize.tran.util.PatternTestObject;

public class MLMessagePTO implements PatternTestObject
{
    /* constants */
    //private static final Logger zLog = Logger.getLogger(MLMessagePTO.class.getName());

    /* class variables */

    /* instance variables */
    private MLMessage zMsg;
    private ArrayList zSenders;
    private ArrayList zRecipients;
    private ArrayList zMIMECTypes;
    private ArrayList zMIMECEncodes;

    /* constructors */
    private MLMessagePTO() {}

    public MLMessagePTO(MLMessage zMsg)
    {
        zSenders = getSenders(null, zMsg);
        zRecipients = getRecipients(null, zMsg);
        zMIMECTypes = getMIMECTypes(zMsg);
        zMIMECEncodes = getMIMECEncodes(zMsg);

        this.zMsg = zMsg;
    }

    /* public methods */
    /* for simplicity,
     * we present same object type (ArrayList) for all field types to caller
     * - flatten all information from multiple sources into single list
     * - flattening also allows us to combine related information into one list
     */
    public Object get(int iFieldType)
    {
        switch(iFieldType)
        {
        case Constants.NONE_IDX:
        default:
            return null;

        case Constants.SENDER_IDX:
        case Constants.FROM_IDX:
            return zSenders; /* ArrayList of CBufferWrapper */

        case Constants.RECIPIENT_IDX:
        case Constants.TOLIST_IDX:
        case Constants.CCLIST_IDX:
        case Constants.BCCLIST_IDX:
            return zRecipients; /* ArrayList of CBufferWrapper */

        case Constants.RELAY_IDX:
            return zMsg.getRelay(); /* ArrayList of CBufferWrapper */

        case Constants.ORIGINATOR_IDX:
            return zMsg.getReplyTo(); /* ArrayList of CBufferWrapper */

        case Constants.SUBJECT_IDX:
            return zMsg.getSubject(); /* ArrayList of CBufferWrapper */

        case Constants.CONTENTTYPE_IDX:
            return zMsg.getContentType(); /* ArrayList of CBufferWrapper */

        case Constants.MIMECONTENTTYPE_IDX:
            return zMIMECTypes; /* ArrayList of CBufferWrapper */

        case Constants.MIMECONTENTENCODE_IDX:
            return zMIMECEncodes; /* ArrayList of CBufferWrapper */
        }
    }

    /* underlying message has changed so update references to it */
    public void update()
    {
        zSenders = getSenders(zSenders, zMsg);
        zRecipients = getRecipients(zRecipients, zMsg);
        zMIMECTypes = getMIMECTypes(zMsg);
        zMIMECEncodes = getMIMECEncodes(zMsg);
        return;
    }

    public void reset()
    {
        if (null != zSenders &&
            null != zMsg.getSender())
        {
            /* we only clear sender list if we created it */
            zSenders.clear();
        }

        if (null != zRecipients &&
            null == zMsg.getRcpt())
        {
            /* we only clear recipient list if we created it */
            zRecipients.clear();
        }

        zMIMECTypes = null;
        zMIMECEncodes = null;
        return;
    }

    public void flush()
    {
        reset();

        zMsg = null;
        zSenders = null;
        zRecipients = null;
        return;
    }

    public String toString()
    {
        return "senders: " + zSenders + "\n" + "recipients: " + zRecipients + "\n" + "mime content-types: " + zMIMECTypes + "\n" + "mime content-encodes: " + zMIMECEncodes;
    }

    /* private methods */
    private ArrayList getSenders(ArrayList zList, MLMessage zMsg)
    {
        /* sender should be same as from so if we have sender, skip from */
        CBufferWrapper zCLine = zMsg.getSender();
        if (null != zCLine)
        {
            if (null == zList)
            {
                zList = new ArrayList();
            }
            else
            {
                zList.clear();
            }
            zList.add(zCLine);
        }
        else /* no sender, use from */
        {
            /* else use from */
            zList = zMsg.getFrom();
        }

        return zList;
    }

    private ArrayList getRecipients(ArrayList zList, MLMessage zMsg)
    {
        /* recipient should be same as tolist, cclist, and bcclist so
         * if we have recipient, skip other lists
         */
        ArrayList zTmpList = zMsg.getRcpt();
        if (null != zTmpList)
        {
            zList = zTmpList;
        }
        else /* no recipients, use to, cc, and bcc */
        {
            /* else consolidate tolist, cclist, and bcclist */
            if (null == zList)
            {
                zList = new ArrayList();
            }
            else
            {
                zList.clear();
            }

            /* tolist has highest priority so insert it first */
            zTmpList = zMsg.getToList();
            if (null != zTmpList)
            {
                zList.addAll(zTmpList);
            }

            zTmpList = zMsg.getCcList();
            if (null != zTmpList)
            {
                zList.addAll(zTmpList);
            }

            zTmpList = zMsg.getBccList();
            if (null != zTmpList)
            {
                zList.addAll(zTmpList);
            }
        }

        return zList;
    }

    private ArrayList getMIMECTypes(MLMessage zMsg)
    {
        ArrayList zList;

        MIMEBody zMIMEBody = zMsg.getMIMEBody();
        if (null == zMIMEBody)
        {
            zList = null;
        }
        else
        {
            zList = zMIMEBody.getPTO(Constants.MIMECONTENTTYPE_IDX);
        }

        return zList;
    }

    private ArrayList getMIMECEncodes(MLMessage zMsg)
    {
        ArrayList zList;

        MIMEBody zMIMEBody = zMsg.getMIMEBody();
        if (null == zMIMEBody)
        {
            zList = null;
        }
        else
        {
            zList = zMIMEBody.getPTO(Constants.MIMECONTENTENCODE_IDX);
        }

        return zList;
    }
}
