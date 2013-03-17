/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.File;

/**
 * An association of a File with
 * its "relative" name.
 */
final class RelativeFile extends RelativeFileName
{
    /**
     * The real file represented by this object
     */
    final File file;

    RelativeFile(String path, File file)
    {
        super(path);
        this.file = file;
    }

    @Override
    public boolean equals(Object obj)
    {
        //Don't bother with superclass
        return file.equals(obj);
    }

    @Override
    public int hashCode()
    {
        //Don't bother with superclass
        return file.hashCode();
    }
}
