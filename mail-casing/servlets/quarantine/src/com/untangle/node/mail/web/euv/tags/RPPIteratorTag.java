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
package com.untangle.node.mail.web.euv.tags;

import java.util.Arrays;
import java.util.Iterator;


/**
 * Tag which iterates over the possible rows-per-page values,
 * and assigns the "current" to the RPPOptionTag
 */
public final class RPPIteratorTag
    extends IteratingTag<String> {

    private static final String IKEY = "untangle.RPPIteratorTag";

    private static final String[] CHOICES = {
        "25",
        "50",
        "100",
        "150",
        "200"
    };


    @Override
    protected Iterator<String> createIterator() {
        return Arrays.asList(CHOICES).iterator();
    }

    @Override
    protected void setCurrent(String s) {
        RPPCurrentOptionTag.setCurrent(pageContext, s);
    }
}
