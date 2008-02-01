package com.untangle.uvm.webui.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Gets web context for a node
 * 
 * @author Catalin Matei
 */
public class WebContextTag extends TagSupport {
	/** the node we need the web context for */
	private String node = null;

	/**
	 * Performs the processing of this tag.
	 */
	public int doStartTag() throws JspException {
		// TODO we should have this on the api
		String[] tokens = node.split("-");
		String webContext = tokens[tokens.length - 1];
		try {
			pageContext.getOut().write(webContext);
		} catch (IOException ioe) {
			throw new JspException(ioe.getMessage());
		}
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Standard JavaBeans style property setter for the source.
	 * 
	 * @param node
	 *            a String representing the node name
	 */
	public void setNode(String node) {
		this.node = node;
	}
}