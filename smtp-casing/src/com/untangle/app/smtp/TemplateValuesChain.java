/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of TemplateValues which can chain together several
 * TemplateValues.  This is useful if a given Template references data
 * from many sources.
 */
public class TemplateValuesChain
    implements TemplateValues {

    private List<TemplateValues> m_valueList;

    /**
     * Initialize instance of TemplateValuesChain.
     * @return Instance of TemplateValuesChain.
     */
    public TemplateValuesChain() {
        this(new TemplateValues[] {});
    }

    /**
     * Initialize instance of TemplateValuesChain with template values.
     * @param v1 TemplateValues to use.
     * @return Instance of TemplateValuesChain.
     */
    public TemplateValuesChain(TemplateValues v1) {
        this(new TemplateValues[] {v1});
    }

    /**
     * Initialize instance of TemplateValuesChain with template values.
     * @param tvs Variable list of TemplateValues to use.
     * @return Instance of TemplateValuesChain.
     */
    public TemplateValuesChain(TemplateValues... tvs) {
        m_valueList = Arrays.asList(tvs);
    }

    /**
     * Append a TemplateValues to the chain
     *
     * @param tv the new TemplateValues
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

    /**
     * Return string of template value.
     * @param  key TemplateValue key to find.
     * @return     String of TemplateValue value.
     */
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
