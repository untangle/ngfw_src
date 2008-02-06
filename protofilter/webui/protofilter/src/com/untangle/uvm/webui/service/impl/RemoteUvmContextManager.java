package protofilter.src.com.untangle.uvm.webui.service.impl;

import javax.security.auth.login.FailedLoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.client.LoginExpiredException;
import com.untangle.uvm.client.RemoteUvmContext;
import com.untangle.uvm.client.RemoteUvmContextFactory;
import com.untangle.uvm.client.UvmConnectException;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.policy.RemotePolicyManager;
import com.untangle.uvm.toolbox.RemoteToolboxManager;

public class RemoteUvmContextManager {
	protected final Log logger = LogFactory.getLog(getClass());
    protected final int DEFAULT_TIMEOUT = 60000000;
    protected final String TYPE_LOGIN_LOCAL = "local";
    protected final String TYPE_LOGIN_SYSTEM = "system";
    protected final String TYPE_LOGIN_INTERACTIVE = "interactive";
    
    private RemoteUvmContextFactory factory = null;
    private RemoteUvmContext remoteUvmContext = null;
    
    private int timeout = DEFAULT_TIMEOUT;
	private String loginType = TYPE_LOGIN_LOCAL;
	private String host = null;
	private int port = 0;
	private String username = null;
	private String password = null;
	private boolean secure = false;
	private boolean force = false;
	
    
    synchronized public void init() {
        logger.info("Initializing UVMRemoteApp...");
        
        try {
        	if (factory == null) {
                factory = RemoteUvmContextFactory.factory();
    			connect();
        	}
		} catch (Exception e) {
			logger.error("Error: unable to connect to Remote UVM Context Factory; UVM server may not be running -- ", e);
			throw new RuntimeException(e);
		}
		
        logger.info("Done initializing UVMRemoteApp...");
    	
    }
    
    synchronized public void destroy() {
		// destruction work
        logger.info("Disconnecting...");
		disconnect();
	}
    
    
    synchronized private boolean connect() throws FailedLoginException, UvmConnectException {
	    // Just in case
    	try {
  	      factory.logout(); 
		} catch (Exception e) {
			// ignore errors
		}
		
	    // login
		if (TYPE_LOGIN_LOCAL.equals(loginType)) {
			remoteUvmContext = LocalUvmContextFactory.context().remoteContext();
		} else if (TYPE_LOGIN_SYSTEM.equals(loginType)){
			remoteUvmContext = factory.systemLogin(timeout);
		} else {
			//interactive login
			remoteUvmContext = 
				factory.interactiveLogin(host, port, username, password, timeout, null, secure, force);
		}
	
	    return true;
		
	}
    
    synchronized private boolean disconnect() {
        if (remoteUvmContext != null) {
        	try {
        	      factory.logout(); 
      		} catch (Exception e) {
      			// ignore errors
      		}
            remoteUvmContext = null;
        }
        return true;
    }
    
	/*
	 * 	Caller MUST have obtained remoteUvmContextManager lock before using the remoteUvmContext
	 */
	public RemoteUvmContext remoteContext() {
		if (remoteUvmContext == null) {
			init();
		}
		return remoteUvmContext;
	}
	
	public RemoteToolboxManager toolboxManager() {
		try {
			return remoteContext().toolboxManager();
		} catch (LoginExpiredException lee) {
	        logger.info("Login expired - logging back on and trying one more time");
			try {
				connect();
				return remoteContext().toolboxManager();
			} catch (Exception e) {
				logger.error("Error: unable to reconnect to Remote UVM Context Factory!", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	public RemotePolicyManager policyManager() {
		try {
			return remoteContext().policyManager();
		} catch (LoginExpiredException lee) {
	        logger.info("Login expired - logging back on and trying one more time");
			try {
				connect();
				return remoteContext().policyManager();
			} catch (Exception e) {
				logger.error("Error: unable to reconnect to Remote UVM Context Factory!", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	public RemoteNodeManager nodeManager() {
		try {
			return remoteContext().nodeManager();
		} catch (LoginExpiredException lee) {
	        logger.info("Login expired - logging back on and trying one more time");
			try {
				connect();
				return remoteContext().nodeManager();
			} catch (Exception e) {
				logger.error("Error: unable to reconnect to Remote UVM Context Factory!", e);
				throw new RuntimeException(e);
			}
		}
	}
	
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setLoginType(String loginType) {
		this.loginType = loginType;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
