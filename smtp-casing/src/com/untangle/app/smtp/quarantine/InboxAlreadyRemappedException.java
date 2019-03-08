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

    /**
     * Initialize instance of InboxAlreadyRemappedException description.
     * @param  toRemap         Source address.
     * @param  alreadyMappedTo Remapped adress.
     * @return                 Exception.
     */
    public InboxAlreadyRemappedException(String toRemap, String alreadyMappedTo) {
        super(toRemap + " already remapped to " + alreadyMappedTo);
        m_toRemap = toRemap;
        m_alreadyMappedTo = alreadyMappedTo;
    }

    /**
     * Return to remap address.
     * @return String of remapped address.
     */
    public String getAccountToRemap()
    {
        return m_toRemap;
    }

    /**
     * Return aleady mapped to.
     * @return String of mapped address.
     */
    public String getAlreadyMappedTo()
    {
        return m_alreadyMappedTo;
    }

}
