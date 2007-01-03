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

package com.untangle.tran.boxbackup;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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

import com.untangle.mvvm.security.Tid;

/**
 * Settings for the BoxBackup transform.
 */
@Entity
@Table(name="tr_boxbackup_settings", schema="settings")
public class BoxBackupSettings implements Serializable
{
    private static final long serialVersionUID = 6441984722150846433L;

    private Long id;
    private Tid tid;
    private int hourInDay;
    private int minuteInHour;
    private String backupURL;

    private BoxBackupSettings() { }

    /**
     * Real constructor
     */
    public BoxBackupSettings(Tid tid)
    {
        this.tid = tid;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    private Long getId() { return id; }
    private void setId(Long id) { this.id = id; }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     */
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="tid", nullable=false)
    public Tid getTid() { return tid; }
    public void setTid(Tid tid) { this.tid = tid; }

    /**
     * @return the Hour of the day
     */
    @Column(name="hour_in_day")
    public int getHourInDay() { return hourInDay; }
    public void setHourInDay(int hour) { this.hourInDay = hour; }

    /**
     * @return the Minute of the day when digest emails should be sent.
     */
    @Column(name="minute_in_day")
    public int getMinuteInHour() { return minuteInHour; }
    public void setMinuteInHour(int mih) { this.minuteInHour = mih; }

    /**
     * @return email address.
     */
    @Column(name="backup_url")
    public String getBackupURL() { return backupURL; }
    public void setBackupURL(String url) {  this.backupURL = url; }
}
