/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.reporting;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

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
