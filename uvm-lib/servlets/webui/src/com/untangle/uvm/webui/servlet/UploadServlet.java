package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.RemoteSkinManager;
import com.untangle.uvm.client.RemoteUvmContext;

/**
 * A servlet for uploading a skin
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class UploadServlet extends HttpServlet {
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		List<FileItem> items = null; 
		try {
			items = upload.parseRequest(req);
			
			// Process the uploaded items
	        RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();
	        RemoteSkinManager skinManager = uvm.skinManager();
	        
			Iterator iter = items.iterator();
			while (iter.hasNext()) {
			    FileItem item = (FileItem) iter.next();

			    if (!item.isFormField()) {
			        skinManager.uploadSkin(item);
			    }
			}
		} catch (Exception e) {
			createRespose(resp, false, e.getMessage());
			return;
		}		
		
		
		createRespose(resp, true, null);
		
	}

	private void createRespose(HttpServletResponse resp, boolean success, String msg) throws IOException {
		resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
		try {
	        JSONObject obj=new JSONObject();
	        obj.put("success",new Boolean(success));
	        if (msg != null){
		        obj.put("msg",msg);
	        }
	        out.print(obj);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.flush();
		out.close();
	}
	
}
