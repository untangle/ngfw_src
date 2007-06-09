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

package com.untangle.node.reporting;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Weekly schedule rule for the Reporting Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_reporting_wk_sched_rule", schema="settings")
public class WeeklyScheduleRule implements Serializable
{
    private static final long serialVersionUID = 2064742840204258978L;

    private Long id;
    private int day; // day of week: SUNDAY, MONDAY, ... SATURDAY

    public WeeklyScheduleRule() { }

    public WeeklyScheduleRule(int day)
    {
        this.day = day;
    }

    @Id
    @Column(name="id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Day of week (if day is specified, then day is live)
     *
     * @return day of week
     */
    @Column(nullable=false)
    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        this.day = day;
    }
}
