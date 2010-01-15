package com.untangle.uvm;

import com.untangle.node.util.SimpleExec;

import java.io.IOException;
import org.apache.log4j.Logger;

/**
 * Test RadiusTest.
 *
 *
 * Created: Thu Jan 14 08:52:39 2010
 *
 * @author <a href="mailto:seb@untangle.com">Sébastien Delafond</a>
 * @version 1.0
 */
public class RadiusTest {
    private final static Logger logger = Logger.getLogger(RadiusTest.class);

    private final static int STATUS_ACCEPTED = 0;
    private final static int STATUS_REJECTED = 1;
    private final static int STATUS_UNREACHABLE = 2;
    private final static int STATUS_UNKNOWN_ERROR = 3;

    public static int test(RadiusServerSettings rs, 
			   String user,
			   String passwd) {
        try {
            SimpleExec.SimpleExecResult result = 
		SimpleExec.exec("radtest",
				new String[] {
				    user,
				    passwd,
				    rs.getServer() + ":" + rs.getPort(),
				    "10",
				    rs.getSharedSecret()
				},
				null,//env
				null,//rootDir
				true,//stdout
				true,//stderr
				1000*20);

	    String output = new String(result.stdOut);
	    logger.debug(output);
	    String[] lines = output.split("\n");
	    for (String line : lines) {
		String[] groups = line.split(":");
		if (groups[0].equals("rad_recv")) {
		    String status = groups[1].split(" ")[0];
		    if (status.equals("Access-Accept")) {
			return STATUS_ACCEPTED;
                    } else if (status.equals("Access-Reject")) {
			return STATUS_REJECTED;
		    }
		} else if (groups[0].equals("radclient")) {
		    if (groups[1].startsWith("no response from server")) {
			return STATUS_UNREACHABLE;
		    } else {
			logger.warn("Couldn't parse radtest output.");
			return STATUS_UNKNOWN_ERROR;
		    }
		    
		}
	    }
        } catch (IOException ioe) {
            logger.warn("Exception while testing radius settings", ioe);
	    return STATUS_UNKNOWN_ERROR;
        }

	return STATUS_UNKNOWN_ERROR; // shouldn't reach
    }
}
