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

package com.metavize.tran.boxbackup;

import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.security.Tid;

/**
 * Settings for the BoxBackup transform.
 *
 * @hibernate.class
 * table="TR_BOXBACKUP_SETTINGS"
 */
public class BoxBackupSettings implements java.io.Serializable
{
    private static final long serialVersionUID = 6441984722150846433L;

    private Long id;
    private Tid tid;
    private int hourInDay;
    private int minuteInHour;
    private String backupURL;


    /**
     * Hibernate constructor.
     */
    private BoxBackupSettings() {}

    /**
     * Real constructor
     */
    public BoxBackupSettings(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId() {return id;}
    private void setId(Long id) {this.id = id;}

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid() {return tid;}
    public void setTid(Tid tid) {this.tid = tid;}

    /**
     *
     * @return the Hour of the day
     * @hibernate.property
     * column="HOUR_IN_DAY"
     */ 
    public int getHourInDay() {return hourInDay;}
    public void setHourInDay(int hour) {this.hourInDay = hour;}

  /**
    *
    * @return the Minute of the day when digest emails should be sent.
    * @hibernate.property
    * column="MINUTE_IN_DAY"
    */
    public int getMinuteInHour() { return minuteInHour; }
    public void setMinuteInHour(int mih) {this.minuteInHour = mih;}

  /**
    *
    * @return email address.
    * @hibernate.property
    * column="BACKUP_URL"
    */
  public String getBackupURL() { return backupURL; }
  public void setBackupURL(String url) { this.backupURL = url; }
    
}
