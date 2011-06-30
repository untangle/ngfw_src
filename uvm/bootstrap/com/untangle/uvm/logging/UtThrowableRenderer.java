/*
 * $HeadURL$
 * Copyright (c) 2003-2011 Untangle, Inc.
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

package com.untangle.uvm.logging;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.spi.ThrowableRenderer;

/**
 * 
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
public class UtThrowableRenderer implements ThrowableRenderer {

    private String prefix;

    public UtThrowableRenderer(String prefix) {
        this.prefix = prefix;
    }

    public String[] doRender(Throwable t) {
        List<String> l = new LinkedList<String>();
        l.add(this.prefix + "      " + t.toString());
        for (StackTraceElement ste: t.getStackTrace()) {
            l.add(this.prefix + "      " + ste.toString());
        }
        return l.toArray(new String[0]);
    }

}
