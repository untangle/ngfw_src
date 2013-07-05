/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.File;

import com.untangle.node.util.IOUtil;

/**
 * Responsible for creating/destroying directories for the
 * Quarantine.  Current implementation is "dumb", but may be
 * enhanced to have nested folders for quicker access times.
 */
class InboxDirectoryTree
{

    private static final String DATA_DIR_NAME = "inboxes";
    private static final int MAX_TRIES = 10000;//Just to avoid the infinite loop

    private Object m_lock = new Object();

    private File m_inboxRootDir;
    private File m_quarantineRootDir;

    InboxDirectoryTree(File quarantineRootDir)
    {

        m_quarantineRootDir = quarantineRootDir;

        m_inboxRootDir = new File(m_quarantineRootDir, DATA_DIR_NAME);
        if(!m_inboxRootDir.exists()) {
            m_inboxRootDir.mkdirs();
        }
    }

    /**
     * Pass a visitor through this
     * tree's managed directory structure.
     * <br><br>
     * This is a depth-first tree walk, so the visitor
     * <b>can</b> delete files.
     *
     * @param visitor the visitor
     */
    void visitInboxes(InboxDirectoryTreeVisitor visitor)
    {
        visitInboxesImpl(m_inboxRootDir, visitor, DATA_DIR_NAME);
    }

    private void visitInboxesImpl(File dir, InboxDirectoryTreeVisitor visitor, String relativePathAsString)
    {
        File[] kids = dir.listFiles();
        for(File kid : kids) {
            if(kid.isDirectory()) {
                visitInboxesImpl(kid,
                                 visitor,
                                 relativePathAsString + File.separator + kid.getName());
            }
        }
        visitor.visit(dir);
    }

    /**
     * Create a new inbox directory.
     * This method *does* create the directory file
     *
     * @return the new directory, or null if there
     *         is some terrible problem with the
     *         underlying store.
     */
//    File createInboxDir()
//    {
//        return createInboxDirImpl();
//    }

    /**
     * Delete the inbox directory.  Note that this
     * method is "dumb" in that it (a) does not update
     * any counts and (b) does not check if the folder
     * is empty.
     *
     * @param dir the doomed directory.
     */
    void deleteInboxDir(String dir)
    {
        IOUtil.rmDir(new File(m_quarantineRootDir, dir));
    }

//    private File createInboxDirImpl()
//    {
//        long num = System.currentTimeMillis();
//        File f = null;
//        for(int i = 0; i<MAX_TRIES; i++) {
//            f = new File(m_inboxRootDir, Long.toString(num + i));
//            synchronized(m_lock) {
//                if(!f.exists()) {
//                    f.mkdirs();
//                }
//                else {
//                    f = null;
//                }
//            }
//            if(f != null) {
//                break;
//            }
//        }
//        return f==null?
//            null:new RelativeFile(
//                                  (DATA_DIR_NAME + File.separator + f.getName()),
//                                  f);
//    }
}
