/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VSCTLDefinition.java,v 1.10 2005/03/11 03:34:57 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;

/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_VSCTL_DEFINITION"
 */
public class VSCTLDefinition implements Serializable
{
    private static final long serialVersionUID = 7520156745253589327L;

    private Long id;

    public static final String NO_NOTES = "no description";

    /* settings */
    private Action actionOnDetect = Action.BLOCK_AND_WARN_SENDER;
    private boolean scan = true;
    private boolean copyOnBlock = false;
    private String notes = NO_NOTES;
    private String copyOnBlockDetails = NO_NOTES;
    // private Alerts alerts = new Alerts();

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public VSCTLDefinition() { }


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
     * @hibernate.id
     * column="DEF_ID"
     * generator-class="native"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * - actionOnDetect: a string specifying a response to events if a message containing spam (defaults to forward and warn sender)
     * one of BLOCK_ACTION, BLOCK_AND_WARN_SENDER_ACTION, BLOCK_AND_WARN_RECEIVER_ACTION,
     * BLOCK_AND_WARN_BOTH_ACTION, or PASS_ACTION
     *
     * @return the action to take if a message is judged to be spam.
     * @hibernate.property
     * column="ACTION_ON_DETECT"
     * type="com.metavize.tran.email.ActionUserType"
     * not-null="true"
     */
    public Action getActionOnDetect()
    {
        return actionOnDetect;
    }

    public void setActionOnDetect(Action actionOnDetect)
    {
        // Guard XXX
        this.actionOnDetect = actionOnDetect;
    }

    public String[] getActionOnDetectEnumeration()
    {
        Action[] actions = Action.virusValues();
        String[] result = new String[actions.length];
        for (int i = 0; i < actions.length; i++)
            result[i] = actions[i].toString();
        return result;
    }

    /**
     * - scan: a boolean specifying whether or not to always scan a message for spam before applying other rules and if spam, to skip other rules (defaults to true)
     *
     * @return whether or not to save a copy of message when exception occurs
     * @hibernate.property
     * column="SCAN"
     */
    public boolean isScan()
    {
        return scan;
    }

    public void setScan(boolean scan)
    {
        this.scan = scan;
    }

    /**
     * - copyOnBlock: a boolean specifying whether or not to save a copy of message (e.g., quarantine message) when a filter definition blocks the message (defaults to false)
     *
     * @return whether or not to save a copy of message when block occurs
     * @hibernate.property
     * column="COPY_ON_BLOCK"
     */
    public boolean isCopyOnBlock()
    {
        return copyOnBlock;
    }

    public void setCopyOnBlock(boolean copyOnBlock)
    {
        this.copyOnBlock = copyOnBlock;
    }

    /**
     * - notes: a string containing notes (defaults to NO_NOTES)
     *
     * @return the notes for this spam definition
     * @hibernate.property
     * column="NOTES"
     */
    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    /**
     * - copyOnBlockDetails: a string containing information about the copyOnBlock setting
     *
     * @return the copyOnBlock notes for this spam definition
     * @hibernate.property
     * column="COPY_ON_BLOCK_DETAILS"
     */
    public String getCopyOnBlockDetails()
    {
        return copyOnBlockDetails;
    }

    public void setCopyOnBlockDetails(String copyOnBlockDetails)
    {
        this.copyOnBlockDetails = copyOnBlockDetails;
    }

    /*
<?xml version="1.0"?>

<!--
Copyright (c) 2004 Metavize Inc.
All rights reserved.

This software is the confidential and proprietary information of
Metavize Inc. ("Confidential Information").  You shall
not disclose such Confidential Information.

$Id: VSCTLDefinition.java,v 1.10 2005/03/11 03:34:57 cng Exp $
-->

<!--
VSCTLDefinition specifies:
- scan: a boolean specifying whether or not to always scan a message for viruses before applying other rules and if viruses are detected, to skip other rules (defaults to true)

- actionOnDetect: a string specifying a response to events if a message containing virus (defaults to forward and warn sender)

- copyOnBlock: a boolean specifying whether or not to save a copy of message (e.g., quarantine message) when a filter definition blocks the message (defaults to false)

- alert: an Alerts schema specifying whether or not to send an alert/log if a message containing a virus is detected
-->
<define-schema-type name="VSCTLDefinition"
            package="com.metavize.tran.xmailscanner" version="1.1">

  <attr name="actionOnDetect" type="String"> <default>block, warn sender</default>
    <enumeration>
      <eval name="BLOCK">block</eval>
      <eval name="FORWARDANDWARNSENDER">block, warn sender</eval>
      <eval name="FORWARDANDWARNRECEIVER">block, warn receiver</eval>
      <eval name="FORWARDANDWARNBOTH">block, warn sender &amp; receiver</eval>
      <eval name="PASS">pass (use carefully)</eval>
    </enumeration>
  </attr>

  <attr name="scan" type="boolean" isRequired="false" default="true"/>

  <attr name="copyOnBlock" type="boolean" isRequired="false" default="false"/>
  <attr name="notes" type="String" isRequired="false" default="no description"/>
  <child name="alerts" type="com.metavize.mvvm.schema.node.Alerts" />

</define-schema-type>
    */
}
