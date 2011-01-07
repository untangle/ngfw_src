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
package com.untangle.uvm.toolbox;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;


/**
 * Holds information about a Debian package.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class MackageDesc implements Serializable
{

    private static final Logger logger = Logger.getLogger(MackageDesc.class);

    public static final int UNKNOWN_POSITION = -1;

    public enum Type {
        NODE,
        CASING,
        SERVICE,
        LIBRARY,
        BASE,
        LIB_ITEM,
        TRIAL,
        UNKNOWN
    }

    private final String name;
    private final String displayName;
    private final Type type;
    private final String installedVersion;
    private final String availableVersion;
    private final String shortDescription;
    private final String longDescription;
    private final String fullVersion;
    private final int size;
    private final int installedSize;
    private final byte[] descIcon;
    private final int viewPosition;
    private final boolean autoStart;
    private final boolean invisible;
    private final String hide;

    public MackageDesc(Map<String, String> m, String installedVersion)
    {
        name = m.get("package");

        String ut = m.get("untangle-pkg-type");
        Type untangleType = Type.UNKNOWN;
        if (null != ut) {
            try {
                untangleType = Type.valueOf(ut.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown type: " + ut);
                untangleType = Type.UNKNOWN;
            }
        }

        type = untangleType;

        displayName = m.get("display-name");

        // versions
        availableVersion = m.get("version");

        // view position (used for store and toolbox)
        String v = m.get("view-position");
        viewPosition = null == v ? UNKNOWN_POSITION : Integer.parseInt(v);

        v = m.get("auto-start");
        autoStart = (v != null && Boolean.parseBoolean(v));

        v = m.get("invisible");
        invisible = (v != null && Boolean.parseBoolean(v));

        // size
        v = m.get("size");
        size = null == v ? 0 : Integer.parseInt(v);
        v = m.get("installed-size");
        installedSize = null == v ? 0 : Integer.parseInt(v);

        // description
        v = m.get("description");
        int i = v.indexOf('\n');
        if (0 <= i) {
            shortDescription = v.substring(0, i);
            longDescription = v.substring(i + 1);
        } else {
            shortDescription = v;
            longDescription = "";
        }

        // description
        fullVersion = m.get("untangle-full-version");

        // desc icon
        v = m.get("desc-icon");
        if (null != v) {
            descIcon = decode(v);
        } else {
            descIcon = null;
        }

        this.installedVersion = installedVersion;

        // hide 
        v = m.get("untangle-hide");
        String hideOn = m.get("untangle-hide-on"); // obsolete old name - this is for backwards compat
        // any non-null value means true
        // we must do this because old packages used varying values like "iso" and "u4w"
        this.hide = (v == null && hideOn == null ? "" : "true");
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Type getType()
    {
        return type;
    }

    public String getInstalledVersion()
    {
        return installedVersion;
    }

    public String getAvailableVersion()
    {
        return availableVersion;
    }

    public String getShortDescription()
    {
        return shortDescription;
    }

    public String getLongDescription()
    {
        return longDescription;
    }

    public String getFullVersion()
    {
        return fullVersion;
    }

    public int getSize()
    {
        return size;
    }

    public int getInstalledSize()
    {
        return installedSize;
    }

    public byte[] descIcon()
    {
        byte[] retVal = null;

        if (null != descIcon) {
            retVal = new byte[descIcon.length];
            System.arraycopy(descIcon, 0, retVal, 0, descIcon.length);
        }

        return retVal;
    }

    public int getViewPosition()
    {
        return viewPosition;
    }

    public boolean isAutoStart()
    {
        return autoStart;
    }

    public boolean isInvisible()
    {
        return invisible;
    }

    public String getHide()
    {
        return hide;
    }

    // Object methods ----------------------------------------------------------

    public String toString()
    {
        return "Mackage name: " + name;
    }

    // private methods ---------------------------------------------------------

    private byte[] decode(String v)
    {
        v = v.replaceAll("[ \t]", "");

        return Base64.decodeBase64(v.getBytes());
    }
}
