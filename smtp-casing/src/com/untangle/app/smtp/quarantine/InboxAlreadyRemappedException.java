/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.Serializable;

/**
 * ...name says it all...
 */
@SuppressWarnings("serial")
public class InboxAlreadyRemappedException extends Exception implements Serializable
{

    private final String m_alreadyMappedTo;
    private final String m_toRemap;

    public InboxAlreadyRemappedException(String toRemap, String alreadyMappedTo) {
        super(toRemap + " already remapped to " + alreadyMappedTo);
        m_toRemap = toRemap;
        m_alreadyMappedTo = alreadyMappedTo;
    }

    public String getAccountToRemap()
    {
        return m_toRemap;
    }

    public String getAlreadyMappedTo()
    {
        return m_alreadyMappedTo;
    }

}
