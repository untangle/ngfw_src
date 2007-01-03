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

package com.untangle.tran.portal.rdp;

import com.untangle.mvvm.portal.*;
import com.untangle.mvvm.util.FormUtil;
import java.util.Hashtable;

public class RdpBookmark extends Bookmark
{
    private static final String SIZE_KEY = "size";
    private static final String CONSOLE_KEY = "console";
    private static final String COMMAND_KEY = "command";
    private static final String HOST_KEY = "host";

    private static final String SIZE_640  = "640x480";
    private static final String SIZE_800  = "800x600";
    private static final String SIZE_1024 = "1024x768";
    private static final String SIZE_1280 = "1280x1024";

    public static final String[] SIZE_ENUMERATION = { SIZE_640, SIZE_800, SIZE_1024, SIZE_1280 };

    public static final String SIZE_DEFAULT = SIZE_800;
    public static final String CONSOLE_DEFAULT = "false";

    // Cache
    transient Hashtable values = null;

    public RdpBookmark(){  // temporarily for GUI compatibility
        super();
        setTarget("");
    }

    public RdpBookmark(Bookmark parent)
    {
        setId(parent.getId());
        setName(parent.getName());
        setApplicationName(parent.getApplicationName());
        setTarget(parent.getTarget());
    }

    public String getSize() {
        return strGetter(SIZE_KEY, SIZE_DEFAULT);
    }

    public void setSize(String size) {
        strSetter(SIZE_KEY, size);
    }

    public String getHost() {
        return strGetter(HOST_KEY, null);
    }

    public void setHost(String host) {
        strSetter(HOST_KEY, host);
    }

    public String getCommand() {
        return strGetter(COMMAND_KEY, "");
    }

    public void setCommand(String command) {
        strSetter(COMMAND_KEY, command);
    }

    public boolean getConsole() {
        String val = strGetter(CONSOLE_KEY, CONSOLE_DEFAULT);
        return Boolean.parseBoolean(val);
    }

    public void setConsole(boolean console) {
        strSetter(CONSOLE_KEY, Boolean.toString(console));
    }

    private String strGetter(String key, String def) {
        if (values == null)
            values = FormUtil.parseQueryString(getTarget());
        String[] v = (String[]) values.get(key);
        if (v == null || v.length == 0)
            return def;
        return v[0];
    }

    private void strSetter(String key, String val) {
        if (values == null) {
            String target = getTarget();
            if (target != null && !target.equals(""))
                values = FormUtil.parseQueryString(getTarget());
            else
                values = new Hashtable();
        }
        if (val == null || val == "")
            values.remove(key);
        else
            values.put(key, new String[] { val });
        setTarget(FormUtil.unparseQueryString(values));
    }
}
