/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EmailSettings.java,v 1.4 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Email transform settings.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_SETTINGS"
 */
public class EmailSettings implements Serializable
{
    private static final long serialVersionUID = -908564012835838443L;

    private Long id;
    private Tid tid;

    private CTLDefinition control = new CTLDefinition();

    private SSCTLDefinition spamInboundCtl;
    private SSCTLDefinition spamOutboundCtl;

    private VSCTLDefinition virusInboundCtl;
    private VSCTLDefinition virusOutboundCtl;

    private List filters = new ArrayList();

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public EmailSettings() { }

    public EmailSettings(Tid tid, SSCTLDefinition spamInboundCtl, SSCTLDefinition spamOutboundCtl,
                         VSCTLDefinition virusInboundCtl, VSCTLDefinition virusOutboundCtl)
    {
        this.tid = tid;
        this.spamOutboundCtl = spamOutboundCtl;
        this.spamInboundCtl = spamInboundCtl;
        this.virusInboundCtl = virusInboundCtl;
        this.virusOutboundCtl = virusOutboundCtl;
    }

    // business methods ------------------------------------------------------

    public void addFilter(MLDefinition filter)
    {
        filters.add(filter);
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings.
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Control information (main settings of transform)
     *
     * @return the control definition
     * @hibernate.many-to-one
     * column="CONTROL"
     * cascade="all"
     * not-null="true"
     */
    public CTLDefinition getControl()
    {
        return control;
    }

    public void setControl(CTLDefinition control)
    {
        this.control = control;
    }

    /**
     * Inbound Spam control: an inbound session SSCTLDefinition
     *
     * @return inbound spam control defintion
     * @hibernate.many-to-one
     * column="SPAM_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SSCTLDefinition getSpamInboundCtl()
    {
        return spamInboundCtl;
    }

    public void setSpamInboundCtl(SSCTLDefinition spamInboundCtl)
    {
        this.spamInboundCtl = spamInboundCtl;
    }

    /**
     * Outbound Spam control: an outbound session SSCTLDefinition
     *
     * @return outbound spam control defintion
     * @hibernate.many-to-one
     * column="SPAM_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public SSCTLDefinition getSpamOutboundCtl()
    {
        return spamOutboundCtl;
    }

    public void setSpamOutboundCtl(SSCTLDefinition spamOutboundCtl)
    {
        this.spamOutboundCtl = spamOutboundCtl;
    }

    /**
     * Inbound Virus control: an inbound session VSCTLDefinition
     *
     * @return inbound virus control defintion
     * @hibernate.many-to-one
     * column="VIRUS_INBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VSCTLDefinition getVirusInboundCtl()
    {
        return virusInboundCtl;
    }

    public void setVirusInboundCtl(VSCTLDefinition virusInboundCtl)
    {
        this.virusInboundCtl = virusInboundCtl;
    }

    /**
     * Outbound Virus control: an outbound session VSCTLDefinition
     *
     * @return outbound virus control defintion
     * @hibernate.many-to-one
     * column="VIRUS_OUTBOUND"
     * cascade="all"
     * not-null="true"
     */
    public VSCTLDefinition getVirusOutboundCtl()
    {
        return virusOutboundCtl;
    }

    public void setVirusOutboundCtl(VSCTLDefinition virusOutboundCtl)
    {
        this.virusOutboundCtl = virusOutboundCtl;
    }

    /**
     * filter: a list of filter definitions (1st match invokes accompanying action)
     *
     * @return the list of email filters
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.email.MLDefinition"
     */
    public List getFilters()
    {
        return filters;
    }

    public void setFilters(List filters)
    {
        this.filters = filters;
    }

    /*
<?xml version="1.0"?>

<!--
Copyright (c) 2004 Metavize Inc.
All rights reserved.

This software is the confidential and proprietary information of
Metavize Inc. ("Confidential Information").  You shall
not disclose such Confidential Information.

$Id: EmailSettings.java,v 1.4 2005/02/25 02:45:29 amread Exp $
-->

<!--
see "java/com/metavize/mvvm/schema/xml/schematype.xsd" for more info

XMailScannerTransformDesc specifies:
- control: a CTLDefinition (no default)
- spamInboundCtl: an inbound session SSCTLDefinition (no default)
- spamOutboundCtl: an outbound session SSCTLDefinition (no default)
- virusInboundCtl: an inbound session VSCTLDefinition (no default)
- virusOutboundCtl: an outbound session VSCTLDefinition (no default)
- filter: a list of filter definitions (1st match invokes accompanying action)
-->
<define-schema-type name="XMailScannerTransformDesc"
            package="com.metavize.tran.xmailscanner" version="1.1">
  <child name="control" type="com.metavize.tran.xmailscanner.CTLDefinition" isRequired="false"/>
  <child name="spamInboundCtl" type="com.metavize.tran.xmailscanner.SSCTLDefinition" isRequired="false"/>
  <child name="spamOutboundCtl" type="com.metavize.tran.xmailscanner.SSCTLDefinition" isRequired="false"/>
  <child name="virusInboundCtl" type="com.metavize.tran.xmailscanner.VSCTLDefinition" isRequired="false"/>
  <child name="virusOutboundCtl" type="com.metavize.tran.xmailscanner.VSCTLDefinition" isRequired="false"/>
  <child-list name="filter" type="com.metavize.tran.xmailscanner.MLDefinition"/>
</define-schema-type>

<?xml version="1.0" encoding="UTF-8"?>

<mvvm-transform>
  <transform-name>xmailscanner-transform</transform-name>
  <display-name>Email Scanner</display-name>
  <classname>com.metavize.tran.xmailscanner.XMailScannerTransform</classname>
  <gui-classname>com.metavize.tran.xmailscanner.gui.MTransformJPanel</gui-classname>

  <log-settings>
    <loggableTypes typeName="com.metavize.tran.xmailscanner.SpamLogEvent"
                   displayName="Spam Mail Event"/>
    <loggableTypes typeName="com.metavize.tran.xmailscanner.VirusLogEvent"
                   displayName="Virus Mail Event"/>
    <loggableTypes typeName="com.metavize.tran.xmailscanner.CustomLogEvent"
                   displayName="Custom Mail Event"/>
    <loggableTypes typeName="com.metavize.tran.xmailscanner.SizeLimitLogEvent"
                   displayName="SizeLimit Mail Event"/>
    <loggedTypes value="com.metavize.tran.xmailscanner.SpamLogEvent"/>
    <loggedTypes value="com.metavize.tran.xmailscanner.VirusLogEvent"/>
    <loggedTypes value="com.metavize.tran.xmailscanner.CustomLogEvent"/>
    <loggedTypes value="com.metavize.tran.xmailscanner.SizeLimitLogEvent"/>
  </log-settings>

  <transform-desc/>


  <settings node-type="com.metavize.tran.xmailscanner.XMailScannerTransformDesc">
    <control virusScanner="F-Prot Antivirus" spamScanner="SpamAssassin" spamMsgSzLimit="262144"/>

    <spamInboundCtl scan="true"
                    notes="Default configuration for inbound SPAM control">
        <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>
    </spamInboundCtl>

    <spamOutboundCtl scan="true"
                    notes="Default configuration for outbound SPAM control">
        <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>
    </spamOutboundCtl>

    <virusInboundCtl scan="true" actionOnDetect="pass (use carefully)" copyOnBlock="false"
                    notes="Default configuration for inbound virus control">
        <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>
    </virusInboundCtl>

    <virusOutboundCtl scan="true" actionOnDetect="pass (use carefully)" copyOnBlock="false"
                      notes="Default configuration for outbound virus control">
        <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>
    </virusOutboundCtl>
  </settings>

</mvvm-transform>

    */
}
