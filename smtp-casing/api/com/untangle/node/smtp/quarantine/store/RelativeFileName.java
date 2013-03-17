/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

/**
 * Name of a file, relative to some other root.
 * <br><br>
 * Typed just to keep track of when we're dealing
 * with a relative path
 */
class RelativeFileName
{
    /**
     * The relative path
     */
    final String relativePath;

    RelativeFileName(String path)
    {
        this.relativePath = path;
    }

    @Override
    public boolean equals(Object obj)
    {
        return relativePath.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return relativePath.hashCode();
    }
}
