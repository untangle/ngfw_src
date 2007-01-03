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

package com.untangle.mvvm.tran;

/**
 * A TemplateValues instance is used in conjunction with a
 * {@link com.untangle.tran.util.Template Template}.  The TemplateValues
 * provides Strings which are mapped to the keys found in a template.
 * <br>
 */
public interface TemplateValues {

    /**
     * Access the value for the given key.  Null
     * is returned if the key cannot be mapped
     * to a value.
     *
     * @param key the key
     * @return the value, or null.
     */
    public String getTemplateValue(String key);
}
