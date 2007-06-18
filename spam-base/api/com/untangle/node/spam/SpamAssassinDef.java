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
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

/**
 * SpamAssassin System Default configuration
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="n_spamassassin_def", schema="settings")
public class SpamAssassinDef implements Serializable
{
    private static final long serialVersionUID = -7246008133224043456L;

    private static final String DEF_COMMENT_SETTING = "## Auto-modified by Untangle Server, do not manually edit. ##";
    private static final String DEF_COMMENT_VAL = null;
    private static final String DEF_COMMENT_DESC = null;

    public static final SpamAssassinDef DEF_COMMENT_DEF = new SpamAssassinDef(DEF_COMMENT_SETTING, DEF_COMMENT_VAL, DEF_COMMENT_DESC, false);

    // required option settings:
    // ENABLED=1
    // (these option settings should never be changed, de-activated, or removed
    //  - SpamAssassinClient depends on them)
    private static final String ENABLED_SETTING = "ENABLED";
    private static final String ENABLED_VAL = "1";
    private static final String ENABLED_DESC = "Enable spamd (required).";

    public static final SpamAssassinDef ENABLED_DEF = new SpamAssassinDef(ENABLED_SETTING, ENABLED_VAL, ENABLED_DESC, true);

    // default option settings:
    // OPTIONS="--create-prefs --round-robin --max-children 5 --helper-home-dir"
    // -> spamd can create user preferences file when necessary
    // -> spamd can equally distribute load to all children
    // -> spamd can spawn max of 5 children
    // -> spamd can use the spamc caller's home directory
    // (these options settings may be changed, de-activated, or removed)
    public static final String OPTIONS_SETTING = "OPTIONS";
    public static final String OPTIONS_VAL = "\"--create-prefs --round-robin --max-children 5 --helper-home-dir\"";
    public static final String OPTIONS_DESC = "Define spamd run-time options.";

    public static final SpamAssassinDef OPTIONS_DEF = new SpamAssassinDef(OPTIONS_SETTING, OPTIONS_VAL, OPTIONS_DESC, true);

    private static final String EMPTY_SETTING = "#";
    private static final String EMPTY_VAL = null;
    private static final String EMPTY_DESC = null;
    public static final SpamAssassinDef EMPTY_DEF = new SpamAssassinDef(EMPTY_SETTING, EMPTY_VAL, EMPTY_DESC, false);

    public static final String EMPTY_DESCRIPTION = EMPTY_DESC;

    private Long id;

    private String optName;
    private String optValue;
    private String description = EMPTY_DESCRIPTION;
    private boolean active = false;

    // constructors -----------------------------------------------------------

    public SpamAssassinDef() {}

    public SpamAssassinDef(String optName, String optValue, String description, boolean active) {
        this.optName = optName;
        this.optValue = optValue;
        this.description = description;
        this.active = active;
    }

    // accessors --------------------------------------------------------------

    @Id
    @Column(name="id")
    @GeneratedValue
    private Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    /**
     * name of SpamAssassin System Default option
     *
     * @return name of SpamAssassin System Default option
     */
    @Column(nullable=false)
    public String getOptName() {
        return optName;
    }

    public void setOptName(String optName) {
        this.optName = optName;
        return;
    }

    /**
     * value of SpamAssassin System Default option
     *
     * @return value of SpamAssassin System Default option
     */
    @Column(nullable=true)
    public String getOptValue() {
        return optValue;
    }

    public void setOptValue(String optValue) {
        this.optValue = optValue;
        return;
    }

    /**
     * description of optName (for display)
     *
     * @return description of optName
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
     * active option flag
     * - if not active, option is commented out
     *   (line with option starts with '#' char)
     *
     * @return active option flag
     */
    @Column(nullable=false)
    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        return;
    }

    public boolean equalsOptName(SpamAssassinDef testDef) {
        return optName.equalsIgnoreCase(testDef.getOptName());
    }

    public boolean equalsOptValue(SpamAssassinDef testDef) {
        String testOptValue = testDef.getOptValue();
        return (null == optValue && null != testOptValue ? false :
                null == optValue && null == testOptValue ? true :
                optValue.equalsIgnoreCase(testOptValue));
    }

    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        if (false == active) {
            if (0 == optName.length())
                strBuilder = strBuilder.append("");
            else
                strBuilder = strBuilder.append("# ").append(optName);
        } else {
            if (null == optValue)
                strBuilder.append(optName);
            else
                strBuilder.append(optName).append("=").append(optValue);
        }

        return strBuilder.toString();
    }
}
