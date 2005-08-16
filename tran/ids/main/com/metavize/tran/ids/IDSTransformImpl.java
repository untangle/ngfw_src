package com.metavize.tran.ids;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.List;
import java.util.Date;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.Direction;
import com.metavize.tran.token.TokenAdaptor;

public class IDSTransformImpl extends AbstractTransform implements IDSTransform {

	private static final String EVENT_QUERY
		= "SELECT create_date, message, blocked, "
		+ "c_client_addr, c_client_port, "
		+ "s_server_addr, s_server_port, "
		+ "client_intf, server_intf "
		+ "FROM pl_endp endp "
		+ "JOIN tr_ids_evt USING (session_id) "
		+ "ORDER BY create_date DESC LIMIT ?";
	
	private static final Logger log = Logger.getLogger(IDSTransformImpl.class);
	static {                    
		log.setLevel(Level.INFO);
	}   
			 
	//private IDSRules rules = new IDSRules();	
	private IDSSettings settings = null;

	private final EventHandler handler;
	private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
	private final PipeSpec[] pipeSpecs;
	
	private List ruleList = new ArrayList();
	
	public IDSTransformImpl() {
		handler = new EventHandler();
	 	octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
		httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
		pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };
	}

	protected PipeSpec[] getPipeSpecs() {
		log.debug("Getting PipeSpec");
		return pipeSpecs;
	}

	public List<IDSLog> getLogs(int limit) {
		List<IDSLog> l = new LinkedList<IDSLog>();
		
		Session s = TransformContextFactory.context().openSession();
		try {
			Connection c = s.connection();
			PreparedStatement ps = c.prepareStatement(EVENT_QUERY);
			ps.setInt(1, limit);
			long l0 = System.currentTimeMillis();
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				long cd = rs.getTimestamp("create_date").getTime();
				Date createDate = new Date(cd);
				String message = rs.getString("message");
				boolean blocked = rs.getBoolean("blocked");
				String clientAddr = rs.getString("c_client_addr");
				int clientPort = rs.getInt("c_client_port");
				String serverAddr = rs.getString("s_server_addr");
				int serverPort = rs.getInt("s_server_port");
				byte clientIntf = rs.getByte("client_intf");
				byte serverIntf = rs.getByte("server_intf");
				
				Direction d = Direction.getDirection(clientIntf, serverIntf);
				IDSLog rl = new IDSLog(createDate, message, blocked, clientAddr, clientPort, serverAddr, serverPort, d);
				l.add(0, rl);
			}
			long l1 = System.currentTimeMillis();
			log.debug("getAccessLogs() in: " + (l1 - l0));
			
		} catch (SQLException exn) {
			log.warn("could not get events", exn);
		} catch (HibernateException exn) {
			log.warn("could not get events", exn);
		} finally {
			try {
				s.close();
			} catch (HibernateException exn) {
				log.warn("could not close Hibernate session", exn);
			}
		}
		return l;
	}
				
	public IDSSettings getIDSSettings() {
		return this.settings;
	}
	
	public void setIDSSettings(IDSSettings settings) {
		Session s = TransformContextFactory.context().openSession();
		try {
			Transaction tx = s.beginTransaction();
			s.saveOrUpdateCopy(settings);
			this.settings = settings;
			tx.commit();
		}catch(HibernateException e) {
			log.warn("Could not set IDSSettings", e);
		} finally {
			try {
				s.close();
			}
			catch(HibernateException e) {
				log.warn("Could not close hibernate session",e);
			}
		}
//		try {
//			reconfigure();
//		}
//		catch(TransformException e) {
//			log.error("Could not save IDS settings",e);
//		}
	}
	
	protected void initializeSettings() {
		log.info("\n*******************Loading Rules**********************");
		String path =  System.getProperty("bunnicula.home");
		File file = new File(path+"/schema/ids-transform/rules");
		visitAllFiles(file); // */
								
		IDSSettings settings = new IDSSettings(getTid());
		settings.setRules(ruleList);
		setIDSSettings(settings);
		log.info(ruleList.size() + " rules loaded");
	}
	private void visitAllFiles(File file) {
		if (file.isDirectory()) {
			String[] children = file.list();
			for (int i=0; i<children.length; i++) 
				visitAllFiles(new File(file, children[i]));
		} 
		else 
			processFile(file);
	}
	
	private void processFile(File file) {
		IDSRuleManager testManager = new IDSRuleManager();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String str;
			while ((str = in.readLine()) != null) {
				if(testManager.addRule(str.trim()))
					ruleList.add(new IDSRule(str));
			}
			in.close();
		} catch (IOException e) { 
			e.printStackTrace(); 
		} catch (ParseException e) {}
	}

	protected void postInit(String args[]) {
		Session s = TransformContextFactory.context().openSession();
		try {
			Transaction tx = s.beginTransaction();
			
			Query q = s.createQuery("from IDSSettings ids where ids.tid = :tid");
			q.setParameter("tid", getTid());
			this.settings = (IDSSettings)q.uniqueResult();
			//updateToCurrent(this.settings);
			try {
				reconfigure();
			}
			catch (Exception e) {e.printStackTrace(); }
			tx.commit();
		} catch (HibernateException exn) {
			//logger.warn("Could not get Intrusion Detection settings", exn);
		} finally {
			try {
				s.close();
			} catch (HibernateException exn) {
				//logger.warn("could not close Hibernate session", exn);
			}
		}
	}

	protected void preStart() throws TransformStartException {
		IDSTest test = new IDSTest();
		if(!test.runTest())
		  throw new TransformStartException("IDS Test failed"); // */
	}
	
	public void reconfigure() throws TransformException {
		IDSSettings currentSettings = getIDSSettings();
		log.debug("reconfigure()");
		if(currentSettings == null)
			throw new TransformException("Failed to get IDS settings: " + currentSettings);

		List<IDSRule> rules = (List<IDSRule>) currentSettings.getRules();
		Iterator<IDSRule> it = rules.iterator();
		while(it.hasNext()) {
			IDSRule rule = it.next();
			IDSDetectionEngine.instance().addRule(rule.getRule());
		}
	}

	//XXX soon to be deprecated ------------------------------------------
	
	public Object getSettings() {
		return null;
	}

	public void setSettings(Object obj) {
	}
}
