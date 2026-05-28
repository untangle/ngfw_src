/**
 * $Id$
 */

package com.untangle.uvm;

import com.untangle.uvm.util.SafeCheckValidationException;
import com.untangle.uvm.util.SafeCheckValidator;
import com.untangle.uvm.util.SafeType;

/**
 * Implementation of {@link SafeCheckTool}. Bridges to the production
 * {@link SafeCheckValidator} so external sweep tooling validates against
 * the exact same code path that Jabsorb {@code preInvokeCallback} uses.
 */
public class SafeCheckToolImpl implements SafeCheckTool
{
    private static final String OK = "OK";
    private static final String CONTEXT_LABEL = "safeCheckTool.validate";

    /**
     * Constructor
     */
    public SafeCheckToolImpl()
    {
    }

    /**
     * Validate a single value against the supplied SafeType array and
     * optional allow-list, mirroring
     * {@link com.untangle.uvm.util.SafeCheckValidator#validateValue}
     * semantics. Never mutates settings; never echoes the offending value
     * in the response.
     *
     * @param value         the value to validate; null or empty is accepted by every SafeType
     * @param safeTypeNames array of SafeType enum constant names; null or empty falls back to SIMPLE_TEXT
     * @param allow         optional literal allow-list applied before the SafeType check (case-sensitive equals)
     * @return {@code "OK"} on acceptance, {@code "REJECTED: <reason>"} on rejection
     */
    @Override
    public String validate(String value, String[] safeTypeNames, String[] allow)
    {
        try {
            if (value != null && allow != null) {
                for (String literal : allow) {
                    if (literal != null && literal.equals(value)) {
                        return OK;
                    }
                }
            }

            SafeType[] types;
            if (safeTypeNames == null || safeTypeNames.length == 0) {
                types = new SafeType[]{ SafeType.SIMPLE_TEXT };
            } else {
                types = new SafeType[safeTypeNames.length];
                for (int i = 0; i < safeTypeNames.length; i++) {
                    String name = safeTypeNames[i];
                    try {
                        types[i] = SafeType.valueOf(name);
                    } catch (IllegalArgumentException ex) {
                        return "REJECTED: unknown SafeType " + name;
                    }
                }
            }

            SafeCheckValidator.validate(value, types, null, CONTEXT_LABEL);
            return OK;

        } catch (SafeCheckValidationException ex) {
            return "REJECTED: " + ex.getMessage();
        } catch (RuntimeException ex) {
            return "REJECTED: validator error: " + ex.getMessage();
        }
    }
}
