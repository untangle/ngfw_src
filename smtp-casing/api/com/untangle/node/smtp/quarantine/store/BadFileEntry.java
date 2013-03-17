/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

/**
 * Exception used by {@link com.untangle.node.smtp.quarantine.store.AbstractDriver}
 * to convey that something read from a file was in a bad format.
 */
@SuppressWarnings("serial")
class BadFileEntry extends Exception
{
    BadFileEntry()
    {}

    BadFileEntry(String s)
    {
        super(s);
    }
}
