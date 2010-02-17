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

package com.untangle.node.webfilter;


/**
 * Action that was taken.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public enum Action
{

    PASS('P', "pass"),
    BLOCK('B', "block");
    
    private static final long serialVersionUID = -1388743204136725990L;

    public static char PASS_KEY = 'P';
    public static char BLOCK_KEY = 'B';

    private final char key;
    private final String name;

    private Action(char key, String name)
    {
        this.key = key;
        this.name = name;
    }

    public char getKey()
    {
        return key;
    }

    public String getName()
    {
        return name;
    }
    
    public static Action getInstance(char key)
    {
    	Action[] values = values();
    	for (int i = 0; i < values.length; i++) {
    		if (values[i].getKey() == key){
    			return values[i];
    		}
		}
    	return null;
    }
}
