/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.mvvm;

import java.io.Serializable;
import java.util.Map;

public class MackageDesc implements Serializable
{
    private static final long serialVersionUID = 3662589795455307379L;

    /* XXX change to enum in 1.5 */
    public static final int SYSTEM_TYPE = 0;
    public static final int TRANSFORM_TYPE = 1;
    public static final int CASING_TYPE = 2;

    private final String name;
    private final String displayName;
    private final int type;
    private final String installedVersion;
    private final String availableVersion;
    private final String shortDescription;
    private final String longDescription;
    private final int size;
    private final int installedSize;
    private final double price;
    private final byte[] orgIcon;
    private final byte[] descIcon;
    private final int rackPosition;

    public MackageDesc(Map m, String installedVersion, byte[] orgIcon,
                       byte[] descIcon)
    {
        // XXX hack, use Mackage field instead.
        name = (String)m.get("package");
        // XXX hack
        boolean isTransform = name.endsWith("-transform");
        boolean isCasing = name.endsWith("-casing");

        displayName = (String)m.get("display-name");

        // XXX type
        if (isTransform) {
            type = TRANSFORM_TYPE;
        } else if (isCasing) {
            type = CASING_TYPE;
        } else {
            type = SYSTEM_TYPE;
        }

        // versions
        availableVersion = (String)m.get("version");

        // price
        String v = (String)m.get("price");
        price = null == v ? 0 : Double.parseDouble(v);

        // rack position
        v = (String)m.get("rack-position");
        rackPosition = null == v ? -1 : Integer.parseInt(v);

        // size
        v = (String)m.get("size");
        size = null == v ? 0 : Integer.parseInt(v);
        v = (String)m.get("installed-size");
        installedSize = null == v ? 0 : Integer.parseInt(v);

        // description
        v = (String)m.get("description");
        int i = v.indexOf('\n');
        if (0 <= i) {
            shortDescription = v.substring(0, i);
            longDescription = v.substring(i + 1);
        } else {
            shortDescription = v;
            longDescription = "";
        }

        this.installedVersion = installedVersion;

        if (null == orgIcon) {
            this.orgIcon = null;
        } else {
            this.orgIcon = new byte[orgIcon.length];
            System.arraycopy(orgIcon, 0, this.orgIcon, 0, orgIcon.length);
        }

        if (null == descIcon) {
            this.descIcon = descIcon;
        } else {
            this.descIcon = new byte[descIcon.length];
            System.arraycopy(descIcon, 0, this.descIcon, 0, descIcon.length);
        }
    }


    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public int getType()
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

    public int getSize()
    {
        return size;
    }

    public int getInstalledSize()
    {
        return installedSize;
    }

    public double getPrice()
    {
        return price;
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

    public int getRackPosition()
    {
        return rackPosition;
    }
}
