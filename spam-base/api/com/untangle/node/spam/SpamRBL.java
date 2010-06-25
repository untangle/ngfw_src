/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.spam;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * spam RBL
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_spam_rbl", schema="settings")
@SuppressWarnings("serial")
public class SpamRBL implements Serializable
{

    // a spam RBL list with a single NO_RBL hostname will deactivate RBL and
    // prevent the list from re-initializing with default hostnames
    // or
    // a spam RBL list with active=false for all entries will deactivate RBL and
    // prevent the list from re-initializing with default values
    public static final String NO_RBL = "127.0.0.1";
    public static final String NO_RBL_DESCRIPTION = "Spam RBL deactivated";

    public static final String EMPTY_DESCRIPTION = "[no description]";

    private Long id;

    private String hostname;
    private String description = EMPTY_DESCRIPTION;
    private boolean active = false;

    // constructors -----------------------------------------------------------

    public SpamRBL() {}

    public SpamRBL(String hostname, String description, boolean active) {
        this.hostname = hostname;
        this.description = description;
        this.active = active;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    public Long getId() {
        return id;
    }

    @SuppressWarnings("unused")
	private void setId(Long id) {
        this.id = id;
    }

    /**
     * hostname of spam RBL
     *
     * @return hostname of spam RBL
     */
    @Column(nullable=false)
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        return;
    }

    /**
     * description of hostname (for display)
     *
     * @return description of hostname
     */
    @Column(nullable=true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        return;
    }

    /**
     * active spam RBL flag
     *
     * @return active spam RBL flag
     */
    @Column(nullable=false)
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        return;
    }

    public void update( SpamRBL newRule )
    {
        this.hostname = newRule.hostname;
        this.description = newRule.description;
        this.active = newRule.active;
    }
}
