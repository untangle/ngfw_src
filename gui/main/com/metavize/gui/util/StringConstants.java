/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: StringConstants.java,v 1.3 2005/02/05 07:16:26 rbscott Exp $
 */

package com.metavize.gui.util;



/**
 * These are constants that are used when there is an empty value assigned to a configuration
 * parameter.  This happens if the parameters is either unknown, unused or unassigned. 
 */
public class StringConstants
{
    private static final StringConstants INSTANCE = new StringConstants();

    public final String EMPTY_START       = "[";
    public final String EMPTY_END         = "]";    

    /** 
     * --RBS--
     * The EMPTY constants should match whatever is in Rule.java, they are not
     * imported here so the GUI is not dependent on the MVVM.
     */

    /**
     * Description of the row or item.
     */
    public final String EMPTY_DESCRIPTION = EMPTY_START + "no description" + EMPTY_END;

    /**
     * Name of the row or item.
     */
    public final String EMPTY_NAME        = EMPTY_START + "no name" + EMPTY_END;

    /**
     * Category for the row or item.
     */
    public final String EMPTY_CATEGORY    = EMPTY_START + "no category" + EMPTY_END;

    public final String TITLE_INDEX       = "#";
    public final String TITLE_CATEGORY    = "category";
    public final String TITLE_LOG         = "log";
    public final String TITLE_NAME        = "name";
    public final String TITLE_STATUS      = "status";
    public final String TITLE_DESCRIPTION = "description";
    public final String TITLE_BLOCK       = "block";

    private StringConstants()
    {
    }
    
    public String bold( String col ) {
        return "<html><b><center>" + col + "</center></b></html>";
    }

    public String html( String col ) {
        return "<html><center>" + col + "</center></html>";
    }

    public String empty( String def ) {
        return EMPTY_START + def + EMPTY_END;
    }

    public static StringConstants getInstance()
    {
        return INSTANCE;
    }
}
