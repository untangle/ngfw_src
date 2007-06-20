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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

/**
 * Holds information about a Debian package.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MackageDesc implements Serializable
{
    private static final long serialVersionUID = 3662589795455307379L;

    private static final Logger logger = Logger.getLogger(MackageDesc.class);

    public static final int UNKNOWN_POSITION = -1;

    /* XXX change to enum in 1.5 */
    public static final int NODE_BASE_TYPE = 3;

    public enum Type {
        NODE,
        CASING,
        LIBRARY,
        BASE,
        LIB_ITEM,
        TRIAL
    }

    public static final int RACK_TYPE_BUNDLE   = 0; // used for store positioning, not the actual rack
    public static final int RACK_TYPE_SERVICE  = 1;
    public static final int RACK_TYPE_UTIL     = 2;
    public static final int RACK_TYPE_SECURITY = 3;
    public static final int RACK_TYPE_CORE     = 4;
    public static final int RACK_TYPE_UNKNOWN  = 5;

    private final String name;
    private final String displayName;
    private final Type type;
    private final String installedVersion;
    private final String availableVersion;
    private final String shortDescription;
    private final String longDescription;
    private final String website;
    private final int size;
    private final int installedSize;
    private final String price;
    private final byte[] descIcon;
    private final int viewPosition;
    private final int rackType;
    private final boolean isBundle;
    private final boolean isService;
    private final boolean isUtil;
    private final boolean isSecurity;
    private final boolean isCore;
    private final String extraName;
    private final String jarPrefix;

    public MackageDesc(Map<String, String> m, String installedVersion,
                       String extraName)
    {
        this.extraName = extraName;

        // XXX hack, use Mackage field instead.
        name = m.get("package");
        // XXX hack

        String ut = m.get("untangle-type");
        if (null == ut) {
            type = null;
        } else {
            type = Type.valueOf(ut.toUpperCase());
        }

        displayName = m.get("display-name");

        // versions
        availableVersion = m.get("version");

        // price
        price = m.get("price");

        // view position (used for store and toolbox)
        String v = m.get("view-position");
        viewPosition = null == v ? UNKNOWN_POSITION : Integer.parseInt(v);

        // rack type init
        int rt = RACK_TYPE_UNKNOWN;

        // bundle or not
        v = m.get("is-bundle");
        isBundle = (v != null && Boolean.parseBoolean(v));
        if (isBundle) {
            rt = RACK_TYPE_BUNDLE;
        }

        // service or not
        if (Type.CASING == type) {
            isService = true;
        } else {
            v = m.get("is-service");
            isService = (v != null && Boolean.parseBoolean(v));
        }
        if (isService) {
            rt = RACK_TYPE_SERVICE;
        }

        // util or not
        v = m.get("is-util");
        isUtil = (v != null && Boolean.parseBoolean(v));
        if (isUtil) {
            rt = RACK_TYPE_UTIL;
        }

        // security or not
        v = m.get("is-security");
        isSecurity = (v != null && Boolean.parseBoolean(v));
        if (isSecurity) {
            rt = RACK_TYPE_SECURITY;
        }

        // core or not
        v = m.get("is-core");
        isCore = (v != null && Boolean.parseBoolean(v));
        if (isCore) {
            rt = RACK_TYPE_CORE;
        }

        this.rackType = rt;

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

        // desc icon
        v = m.get("desc-icon");
        if (null != v) {
            descIcon = decode(v);
        } else {
            descIcon = null; // XXX default
        }

        // website
        website = m.get("website");

        jarPrefix = m.get("untangle-jar-prefix");

        this.installedVersion = installedVersion;
    }

    public String getName()
    {
        return name;
    }

    public String getPrice()
    {
        return price;
    }

    public String getExtraName()
    {
        return extraName;
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

    public String getWebsite()
    {
        return website;
    }

    public int getSize()
    {
        return size;
    }

    public int getInstalledSize()
    {
        return installedSize;
    }

    public byte[] getDescIcon()
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

    public boolean isService()
    {
        return isService;
    }

    public boolean isUtil()
    {
        return isUtil;
    }

    public boolean isSecurity()
    {
        return isSecurity;
    }

    public boolean isCore()
    {
        return isCore;
    }

    public boolean isBundle()
    {
        return isBundle;
    }

    public int getRackType()
    {
        if (rackType == RACK_TYPE_SERVICE) {
            return RACK_TYPE_SERVICE;
        } else if (rackType == RACK_TYPE_UTIL) {
            return RACK_TYPE_UTIL;
        } else if (rackType == RACK_TYPE_SECURITY) {
            return RACK_TYPE_SECURITY;
        } else if (rackType == RACK_TYPE_CORE) {
            return RACK_TYPE_CORE;
        } else if (rackType == RACK_TYPE_BUNDLE) {
            return RACK_TYPE_BUNDLE;
        } else {
            return RACK_TYPE_UNKNOWN;
        }
    }

    public String getJarPrefix()
    {
        return jarPrefix;
    }

    // Object methods ----------------------------------------------------------

    public String toString()
    {
        return "Mackage name: " + name + "type: " + type;
    }

    // private methods ---------------------------------------------------------

    private byte[] decode(String v)
    {
        v = v.replaceAll("[ \t]", "");

        byte[] icon = null;

        try {
            icon = new BASE64Decoder().decodeBuffer(v);
        } catch (IOException exn) {
            logger.warn("could not decode icon", exn);
        }

        return icon;
    }
}
