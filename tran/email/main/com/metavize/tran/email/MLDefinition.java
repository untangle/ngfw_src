/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MLDefinition.java,v 1.9 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;

/**
 * Control information (main settings of email transform)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_ML_DEFINITION"
 */
public class MLDefinition implements Serializable
{
    private static final long serialVersionUID = 1246757615954845684L;

    private Long id;

    public static final String NO_NOTES = "no description";

    public static final Action PASS = Action.PASS;
    public static final Action BLOCK = Action.BLOCK;
    public static final Action EXCHANGE = Action.EXCHANGE;

    /* settings */
    private Action action = PASS;
    private FieldType field = FieldType.SUBJECT;
    private String value = null; // is required.
    private String exchValue = null;
    private boolean copyOnBlock = false;
    private String notes = NO_NOTES;
    // private Alerts alerts = new Alerts();

    // constructor ------------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public MLDefinition() { }

    public MLDefinition(Action action, FieldType field, String value, String exchValue, boolean copyOnBlock, String notes)
    {
        this.action = action;
        this.field = field;
        this.value = value;
        this.exchValue = exchValue;
        this.copyOnBlock = copyOnBlock;
        this.notes = notes;
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
     * - action: a string containing a response to events that match this rule (defaults to PASS_ACTION)
     * one of BLOCK_ACTION, PASS_ACTION, EXCHANGE_ACTION
     *
     * @return the action to take if a message matches this filter rule
     * @hibernate.property
     * column="ACTION"
     * type="com.metavize.tran.email.ActionUserType"
     * not-null="true"
     */
    public Action getAction()
    {
        return action;
    }

    public void setAction(Action action)
    {
        // Guard XXX
        this.action = action;
    }

    public String[] getActionEnumeration()
    {
        Action[] actions = Action.customValues();
        String[] result = new String[actions.length];
        for (int i = 0; i < actions.length; i++)
            result[i] = actions[i].toString();
        return result;
    }

    public static Action getActionInstance(String zName)
    {
        return Action.getInstance(zName);
    }

    /**
     * - field: a string specifying the mail/MIME header field (defaults to SUBJECT_FIELD)
     * one of CONTENT_TYPE_FIELD, MIME_CONTENT_TYPE_FIELD, MIME_CONTENT_ENCODE_FIELD,
     * ORIGINATOR_FIELD, RECIPIENT_FIELD, RELAY_FIELD, SENDER_FIELD, SUBJECT_FIELD;
     *
     * @return the field type to match for this filter rule
     * @hibernate.property
     * column="FIELD_TYPE"
     * type="com.metavize.tran.email.FieldTypeUserType"
     * not-null="true"
     */
    public FieldType getField()
    {
        return field;
    }

    public void setField(FieldType field)
    {
        // Guard XXX
        this.field = field;
    }

    public String[] getFieldEnumeration()
    {
        FieldType[] zFieldTypes = FieldType.values();
        String[] result = new String[zFieldTypes.length];
        for (int i = 0; i < zFieldTypes.length; i++)
            result[i] = zFieldTypes[i].toString();
        return result;
    }

    public static FieldType getFieldInstance(String zName)
    {
        return FieldType.getInstance(zName);
    }

    /**
     * - value: a string containing the mail field value to match (no default)
     *
     * @return the value to match against
     * @hibernate.property
     * column="VALUE"
     * not-null="true"
     */
    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * - exchValue: for exchange action, this exchValue String replaces the value within this mlfield (e.g., strip value within mlfield and insert exchValue String) (this exchValue String may be null for block and pass actions but may not be null for exchange action) (no default)
     *
     * @return the replacement object for exchange action
     * @hibernate.property
     * column="EXCH_VALUE"
     */
    public String getExchValue()
    {
        return exchValue;
    }

    public void setExchValue(String exchValue)
    {
        this.exchValue = exchValue;
    }

    /**
     * - copyOnBlock: a boolean specifying whether or not to save a copy of message (e.g., quarantine message) when a filter definition blocks the message (defaults to false)
     *
     * @return whether or not to save a copy of message when block occurs
     * @hibernate.property
     * column="COPY_ON_BLOCK"
     * not-null="true"
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

        /*
<?xml version="1.0"?>

<!--
Copyright (c) 2004 Metavize Inc.
All rights reserved.

This software is the confidential and proprietary information of
Metavize Inc. ("Confidential Information").  You shall
not disclose such Confidential Information.

$Id: MLDefinition.java,v 1.9 2005/02/25 02:45:29 amread Exp $
-->

<!--
MLDefinition specifies:
- action: a string containing a response to events that match mlfield (defaults to pass)
- type: a string specifying the mail/MIME header field (defaults to literal null)
  null = invalid
  sender = From
  recipient = To, CC, BCC
  originator = Reply-To
  subject = Subject
  relay = Received
  contenttype = Content-Type (within message header, includes charset, etc)
  mimecontenttype = Content-Type (within MIME header, includes charset, etc)
  mimecontentencode = Content-transfer-encoding (quoted-printable, base64, uuencode, etc)
- value: a string containing the mail field object (no default)
- exchvalue: for exchange action, this exchvalue String replaces the value within this mlfield (e.g., strip value within mlfield and insert exchvalue String) (this exchvalue String may be null for block and pass actions but may not be null for exchange action) (no default)
-->

<define-schema-type name="MLDefinition"
            package="com.metavize.tran.xmailscanner" version="1.1">

  <attr name="action" type="String"> <default>pass</default>
    <enumeration>
      <eval name="PASS">pass</eval>
      <eval name="BLOCK">block</eval>
      <eval name="EXCHANGE">exchange</eval>
    </enumeration>
  </attr>

  <attr name="type" type="String"> <default>Subject</default>
    <enumeration>
      <!-- <eval name="NULL">(Please select type)</eval> -->
      <eval name="CONTENTTYPE">Content-Type</eval>
      <eval name="MIMECONTENTTYPE">MIME Content-Type</eval>
      <eval name="MIMECONTENTENCODE">MIME Content-Encode</eval>
      <eval name="ORIGINATOR">Originator</eval>
      <eval name="RECIPIENT">Recipient</eval>
      <eval name="RELAY">Relay</eval>
      <eval name="SENDER">Sender</eval>
      <eval name="SUBJECT">Subject</eval>
    </enumeration>
  </attr>

  <attr name="value" type="String" isRequired="true"/>
  <attr name="exchvalue" type="String" isRequired="false"/>

  <attr name="copyOnBlock" type="boolean" isRequired="false" default="false"/>
  <attr name="notes" type="String" isRequired="false" default="no description"/>
  <child name="alerts" type="com.metavize.mvvm.schema.node.Alerts"/>

</define-schema-type>

    */
}
