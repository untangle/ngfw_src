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

import java.util.*;


/**
 * Implementation of TemplateValues which can chain together several
 * TemplateValues.  This is useful if a given Template references data
 * from many sources.
 */
public class TemplateValuesChain
    implements TemplateValues {

    private List<TemplateValues> m_valueList;

    public TemplateValuesChain() {
        this(new TemplateValues[] {});
    }

    public TemplateValuesChain(TemplateValues v1) {
        this(new TemplateValues[] {v1});
    }

    public TemplateValuesChain(TemplateValues... tvs) {
        m_valueList = Arrays.asList(tvs);
    }

    /**
     * Append a TemplateValues to the chain
     *
     * @param tv the new TemplateValues
     *
     * @return this (useful for method chaining).
     */
    public TemplateValuesChain append(TemplateValues tv) {
        //For thread safety, copy the list and
        //re-assign the reference.
        List<TemplateValues> newList = new ArrayList<TemplateValues>(m_valueList);
        newList.add(tv);
        m_valueList = newList;
        return this;
    }

    public String getTemplateValue(String key) {
        //Assign method-local reference, for thread
        //safety
        List<TemplateValues> list = m_valueList;
        for(TemplateValues values : list) {
            if(values == null) {
                continue;
            }
            String ret = values.getTemplateValue(key);
            if(ret != null) {
                return ret;
            }
        }
        return null;
    }

}
