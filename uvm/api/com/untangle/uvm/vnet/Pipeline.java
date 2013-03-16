/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.io.File;
import java.io.IOException;


/**
 * A Pipeline is a chain of <code>ArgonConnector</code>s for one <code>Session</code>.
 */
public interface Pipeline
{
    Long attach(Object o);
    Object getAttachment(Long key);
    Object detach(Long key);
    Fitting getClientFitting(ArgonConnector argonConnector);
    Fitting getServerFitting(ArgonConnector argonConnector);

    // /**
    //  * Makes a temporary file that will be destroyed on Session
    //  * finalization.
    //  *
    //  * @return the temp file.
    //  * @exception IOException the temp file cannot be created.
    //  */
    // File mktemp() throws IOException;

    // /**
    //  * Makes a temporary file that will be destroyed on Session
    //  * finalization.  The file name will start with the given prefix
    //  * (for debugging purposes).
    //  *
    //  * NOTE: the prefix <b>must not</b> come from user data, it should
    //  * be a constant like 'ftp-virus'.
    //  *
    //  * @return the temp file.
    //  * @exception IOException the temp file cannot be created.
    //  */
    // File mktemp(String prefix) throws IOException;
}
