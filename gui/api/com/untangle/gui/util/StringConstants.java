/*
 * $HeadURL:$
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

package com.untangle.gui.util;



/**
 * These are constants that are used when there is an empty value assigned to a configuration
 * parameter.  This happens if the parameters is either unknown, unused or unassigned. 
 */
public class StringConstants
{
    private static final StringConstants INSTANCE = new StringConstants();

    public static final String EMPTY_START       = "[";
    public static final String EMPTY_END         = "]";    

    /** 
     * --RBS--
     * The EMPTY constants should match whatever is in Rule.java, they are not
     * imported here so the GUI is not dependent on the UVM.
     */

    /**
     * Description of the row or item.
     */
    public static final String EMPTY_DESCRIPTION = EMPTY_START + "no description" + EMPTY_END;
    public static final String EMPTY_COMMENT = EMPTY_START + "no comment" + EMPTY_END;

    /**
     * Name of the row or item.
     */
    public static final String EMPTY_NAME        = EMPTY_START + "no name" + EMPTY_END;

    /**
     * Category for the row or item.
     */
    public static final String EMPTY_CATEGORY    = EMPTY_START + "no category" + EMPTY_END;

    public static final String TITLE_INDEX       = "#";
    public static final String TITLE_CATEGORY    = "category";
    public static final String TITLE_NAME        = "name";
    public static final String TITLE_STATUS      = "status";
    public static final String TITLE_DESCRIPTION = "description";

    public StringConstants() {}
    
    public static String bold( String col ) {
        return "<html><b><center>" + col + "</center></b></html>";
    }

    public static String html( String col ) {
        return "<html><center>" + col + "</center></html>";
    }

    public static String empty( String def ) {
        return EMPTY_START + def + EMPTY_END;
    }

    public static StringConstants getInstance()
    {
        return INSTANCE;
    }
}
