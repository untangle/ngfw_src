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
package com.untangle.uvm.toolbox;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

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
    private final byte[] orgIcon;
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

        // org icon
        v = m.get("org-icon");
        if (null != v) {
            orgIcon = decode(v);
        } else {
            orgIcon = null; // XXX default
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

    public byte[] getOrgIcon()
    {
        byte[] retVal = null;

        if (null != orgIcon) {
            retVal = new byte[orgIcon.length];
            System.arraycopy(orgIcon, 0, retVal, 0, orgIcon.length);
        }

        return retVal;
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
        if( rackType == RACK_TYPE_SERVICE )
            return RACK_TYPE_SERVICE;
        else if( rackType == RACK_TYPE_UTIL )
            return RACK_TYPE_UTIL;
        else if( rackType == RACK_TYPE_SECURITY )
            return RACK_TYPE_SECURITY;
        else if( rackType == RACK_TYPE_CORE )
            return RACK_TYPE_CORE;
        else if( rackType == RACK_TYPE_BUNDLE )
            return RACK_TYPE_BUNDLE;
        else
            return RACK_TYPE_UNKNOWN;
    }

    public String getJarPrefix()
    {
        return jarPrefix;
    }

    // private methods --------------------------------------------------------

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
