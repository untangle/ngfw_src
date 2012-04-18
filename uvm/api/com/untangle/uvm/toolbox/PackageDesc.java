/**
 * $Id$
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONString;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

/**
 * Holds information about a Debian package.
 */
@SuppressWarnings("serial")
public class PackageDesc implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(PackageDesc.class);

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

    public PackageDesc(Map<String, String> m, String installedVersion)
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
        this.hide = m.get("untangle-hide");
    }

    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Type   getType()
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
        return toJSONString();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    // private methods ---------------------------------------------------------

    private byte[] decode(String v)
    {
        v = v.replaceAll("[ \t]", "");

        return Base64.decodeBase64(v.getBytes());
    }
}
