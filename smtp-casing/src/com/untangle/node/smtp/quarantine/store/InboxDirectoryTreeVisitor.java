/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.File;

/**
 * Callback interface for Objects
 * which wish to visit the contents of
 * a {com.untangle.node.smtp.quarantine.store.InboxDirectoryTree InboxDirectoryTree}.
 *
 */
interface InboxDirectoryTreeVisitor
{
    /**
     * Visit the given directory within the
     * InboxDirectoryTree.
     *
     * @param f the relative file representing
     *        a directory.  Note that this
     *        may not be a terminal (inbox)
     *        directory.
     */
    void visit(File f);
}
