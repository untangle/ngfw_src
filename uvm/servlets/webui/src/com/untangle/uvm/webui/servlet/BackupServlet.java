package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;

/**
 * A servlet for backup UVM configuration
 * 
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings("serial")
public class BackupServlet extends HttpServlet {
	
	private static final String DEFAULT_BACKUP_FILENAME = "ung.backup";
	private static final String ACTION_REQUEST_BACKUP = "requestBackup";
	private static final String ACTION_INITIATE_DOWNLOAD = "initiateDownload";
	private static final String ATTR_BACKUP_DATA = "backupData";

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String action = req.getParameter("action");
		if (ACTION_REQUEST_BACKUP.equals(action)) {
			UvmContext uvm = UvmContextFactory.context();
			byte[] backupData = uvm.createBackup();
			req.getSession().setAttribute(ATTR_BACKUP_DATA, backupData);
		} else {
			throw new IllegalArgumentException("Illegal action request!");
		}

	}

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		String action = req.getParameter("action");
		if (ACTION_INITIATE_DOWNLOAD.equals(action)) {
			byte[] backupData = (byte[]) req.getSession().getAttribute(ATTR_BACKUP_DATA);
			req.getSession().removeAttribute("backupData");
			
			if (backupData == null) {
				throw new IllegalArgumentException("Illegal action request - no data to download!");
			}

			// Set the headers.
			resp.setContentType("application/x-download");
			resp.setHeader("Content-Disposition", "attachment; filename=" + DEFAULT_BACKUP_FILENAME);

			// Send to client
			OutputStream out = resp.getOutputStream();
			out.write(backupData, 0, backupData.length);

		} else {
			throw new IllegalArgumentException("Illegal action request!");
		}

	}
}
