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

package com.metavize.tran.virus;

import java.io.Serializable;

/**
 * Virus control: Definition of virus control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_VIRUS_POP_CONFIG"
 */
public class VirusPOPConfig
  extends VirusMailConfig
  implements Serializable
{
    private static final long serialVersionUID = 7520156745253589017L;

    /* settings */
    private VirusMessageAction zMsgAction = VirusMessageAction.REMOVE;

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VirusPOPConfig() {}

    public VirusPOPConfig(boolean bScan,
      VirusMessageAction zMsgAction,
      String zNotes,
      String subjectTemplate,
      String bodyTemplate)
    {
        super(bScan, zNotes, subjectTemplate, bodyTemplate);    
        this.zMsgAction = zMsgAction;
    }

    /**
     * messageAction: a string specifying a response if a message contains virus (defaults to CLEAN)
     * one of CLEAN or PASS
     *
     * @return the action to take if a message is judged to be virus.
     * @hibernate.property
     * column="ACTION"
     * type="com.metavize.tran.virus.VirusMessageActionUserType"
     * not-null="true"
     */
    public VirusMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(VirusMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    public String[] getMsgActionEnumeration()
    {
        VirusMessageAction[] azMsgAction = VirusMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }

}
