/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SSCTLDefinition.java,v 1.15 2005/03/11 03:34:57 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;


/**
 * Spam control: Definition of spam control settings (either direction)
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_SSCTL_DEFINITION"
 */
public class SSCTLDefinition implements Serializable
{
    private static final long serialVersionUID = 3953973747231332517L;

    private Long id;

    public static final String VHIGH_STRENGTH  = "very high";
    public static final String HIGH_STRENGTH   = "high";
    public static final String MEDIUM_STRENGTH = "medium";
    public static final String LOW_STRENGTH    = "low";
    public static final String VLOW_STRENGTH   = "very low";
    public static final String DEF_STRENGTH    = MEDIUM_STRENGTH;

    public static final String NO_NOTES = "no description";

    public static final String[] scanStrengthEnumeration = { VLOW_STRENGTH,
                                                             LOW_STRENGTH,
                                                             MEDIUM_STRENGTH,
                                                             HIGH_STRENGTH,
                                                             VHIGH_STRENGTH };
    /**
     * This is the value that is set for the required score for spam assassin.
     * Mail that get a score above this value are marked as spam.  Lower values
     * are more aggressive( this must be the same length as scanStrengthEnumeration
     */
    public static final double[] scanStrengthValue = new double[] { 7.0, 6.0, 5.0, 4.0, 3.0 };

    /* settings */
    private String scanStrength = DEF_STRENGTH;
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
    public SSCTLDefinition() { }

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
     * - scanStrength: defines the strength of spam scanning to use.  One of very high, high,
     *   medium, low or very low
     *
     * @return the strength of spam scanning to use.
     * @hibernate.property
     * column="SCAN_STRENGTH"
     */
    public String getScanStrength()
    {
        return scanStrength;
    }

    public void setScanStrength(String scanStrength)
    {
        for ( int c = 0 ; c < scanStrengthEnumeration.length ; c++ ) {
            if ( scanStrength.equalsIgnoreCase( scanStrengthEnumeration[c] )) {
                this.scanStrength = scanStrength;
                return;
            }
        }

        this.scanStrength = DEF_STRENGTH;
    }

    public double getScanStrengthValue()
    {
        for ( int c = 0 ; c < scanStrengthEnumeration.length ; c++ ) {
            if ( scanStrength.equalsIgnoreCase( scanStrengthEnumeration[c] )) {
                return scanStrengthValue[c];
            }
        }

        return scanStrengthValue[4];
    }

    public String[] getScanStrengthEnumeration()
    {
        return this.scanStrengthEnumeration;

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
        Action[] actions = Action.spamValues();
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
      <!--
      SSCTLDefinition specifies:
      - scan: a boolean specifying whether or not to always scan a message for spam before applying other rules and if spam, to skip other rules (defaults to false)

      - actionOnDetect: a string specifying a response to events if a message containing spam (defaults to forward and warn sender)

      - copyOnBlock: a boolean specifying whether or not to save a copy of message (e.g., quarantine message) when a filter definition blocks the message (defaults to false)

      - alert: an Alerts schema specifying whether or not to send an alert/log if a message containing spam is detected
      -->
      <define-schema-type name="SSCTLDefinition"
      package="com.metavize.tran.xmailscanner" version="1.1">

      <attr name="scanStrength" type="String"> <default>high</default>
      <enumeration>
      <eval name="HIGH">high</eval>
      <eval name="MEDIUM">medium</eval>
      <eval name="LOW">low</eval>
      </enumeration>
      </attr>

      <attr name="actionOnDetect" type="String"> <default>block, warn sender</default>
      <enumeration>
      <eval name="BLOCK">block</eval>
      <eval name="FORWARDANDWARNSENDER">block, warn sender</eval>
      <eval name="FORWARDANDWARNRECEIVER">block, warn receiver</eval>
      <eval name="FORWARDANDWARNBOTH">block, warn sender &amp; receiver</eval>
      <eval name="PASS">mark &amp; pass</eval>
      </enumeration>
      </attr>

      <attr name="scan" type="boolean" isRequired="false" default="true"/>

      <attr name="copyOnBlock" type="boolean" isRequired="false" default="false"/>
      <attr name="notes" type="String" isRequired="false" default="no description"/>
      <child name="alerts" type="com.metavize.mvvm.schema.node.Alerts" />

      </define-schema-type>
    */
}
