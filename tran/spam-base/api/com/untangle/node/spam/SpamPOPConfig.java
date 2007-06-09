/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.spam;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_spam_pop_config", schema="settings")
public class SpamPOPConfig extends SpamProtoConfig
{
    private static final long serialVersionUID = 7520156745253589117L;

    /* settings */

    private SpamMessageAction zMsgAction = SpamMessageAction.MARK;


    // constructor ------------------------------------------------------------

    public SpamPOPConfig() {}

    public SpamPOPConfig(boolean bScan,
                         SpamMessageAction zMsgAction,
                         int strength,
                         String zNotes,
                         String subjectTemplate,
                         String bodyTemplate,
                         String headerName,
                         String isSpamHeaderValue,
                         String isHamHeaderValue)
    {
        super(bScan,
              strength,
              zNotes,
              subjectTemplate,
              bodyTemplate,
              headerName,
              isSpamHeaderValue,
              isHamHeaderValue);
        this.zMsgAction = zMsgAction;
    }

    // business methods ------------------------------------------------------

    /*
      public String render(String site, String category)
      {
      String message = BLOCK_TEMPLATE.replace("@HEADER@", header);
      message = message.replace("@SITE@", site);
      message = message.replace("@CATEGORY@", category);
      message = message.replace("@CONTACT@", contact);

      return message;
      }
    */

    // accessors --------------------------------------------------------------

    /**
     * messageAction: a string specifying a response if a message
     * contains spam (defaults to MARK) one of MARK or PASS
     *
     * @return the action to take if a message is judged to be spam.
     */
    @Column(name="msg_action", nullable=false)
    @Type(type="com.untangle.node.spam.SpamMessageActionUserType")
    public SpamMessageAction getMsgAction()
    {
        return zMsgAction;
    }

    public void setMsgAction(SpamMessageAction zMsgAction)
    {
        // Guard XXX
        this.zMsgAction = zMsgAction;
        return;
    }

    /* for GUI */
    @Transient
    public String[] getMsgActionEnumeration()
    {
        SpamMessageAction[] azMsgAction = SpamMessageAction.getValues();
        String[] azStr = new String[azMsgAction.length];

        for (int i = 0; i < azMsgAction.length; i++)
            azStr[i] = azMsgAction[i].toString();

        return azStr;
    }
}
