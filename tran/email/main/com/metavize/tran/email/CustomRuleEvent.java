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

import java.nio.*;
import java.util.*;
import java.util.regex.*;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.util.*;

/**
 * Log e-mail message custom rule event.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_CUSTOM_EVT"
 * mutable="false"
 */
public class CustomRuleEvent extends LogEvent
{
    /* constants */

    /* instance variables */
    private MLMessageInfo zMsgInfo;
    private char zAction;
    private char zField;
    private String zPattern;
    private String zExchValue; /* may be null */

    /* constructors */
    public CustomRuleEvent() {}

    public CustomRuleEvent(MLMessageInfo zMsgInfo, MatchAction zMatchAction)
    {
        this.zMsgInfo = zMsgInfo;

        switch(zMatchAction.getPatternAction().getAction())
        {
        case Constants.COPYONBLOCK_ACTION:
        case Constants.BLOCK_ACTION:
            zAction = Action.BLOCK.getKey();
            break;

        case Constants.EXCHANGE_ACTION:
            zAction = Action.EXCHANGE.getKey();
            break;

        default:
        case Constants.PASS_ACTION:
            zAction = Action.PASS.getKey();
            break;
        }

        Matcher zMatcher = zMatchAction.getMatcher();
        if (null != zMatcher)
        {
            zPattern = zMatcher.pattern().pattern();
            zExchValue = zMatchAction.getPatternAction().getValue();
        }
        else
        {
            zPattern = Constants.EMPTYSTR;
            zExchValue = null;
        }

        zField = Constants.convertType(zMatchAction.getType()).getKey();
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
     * Identify custom rule action.
     * type="com.metavize.tran.email.ActionUserType"
     *
     * @return custom rule action.
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
     * Identify custom rule field type.
     * type="com.metavize.tran.email.FieldTypeUserType"
     *
     * @return custom rule field type.
     * @hibernate.property
     * column="FIELD_TYPE"
     * not-null="true"
     */
    public char getField()
    {
        return zField;
    }

    public void setField(char zField)
    {
        this.zField = zField;
        return;
    }

    /**
     * Identify custom rule field match pattern.
     *
     * @return custom rule field match pattern.
     * @hibernate.property
     * column="PATTERN"
     * not-null="true"
     */
    public String getPattern()
    {
        return zPattern;
    }

    public void setPattern(String zPattern)
    {
        this.zPattern = zPattern;
        return;
    }

    /**
     * Identify custom rule field exchange value.
     *
     * @return custom rule field exchange value.
     * @hibernate.property
     * column="EXCH_VALUE"
     * not-null="false"
     */
    public String getExchangeValue()
    {
        return zExchValue;
    }

    public void setExchangeValue(String zExchValue)
    {
        this.zExchValue = zExchValue;
        return;
    }

    /* private methods */
}
