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
     * imported here so the GUI is not dependent on the MVVM.
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
