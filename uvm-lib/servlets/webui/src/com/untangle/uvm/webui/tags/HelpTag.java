package com.untangle.uvm.webui.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import com.untangle.uvm.Version;

/**
 * Gets help url for a component
 * 
 * @author Catalin Matei
 */
public class HelpTag extends TagSupport {
	/** the component we need help for */
	private String source = null;
	/** the subcomponent we need help for */
	private String focus = null;

	/**
	 * Performs the processing of this tag.
	 */
	public int doStartTag() throws JspException {
		StringBuffer helpUrl = new StringBuffer();
		
        String newSource = source.toLowerCase().replace(" ", "_");
        
		// build up the help URL piece by piece ...
        helpUrl.append("http://www.untangle.com/docs/get.php?");
        helpUrl.append("version="); helpUrl.append(Version.getVersion());
        helpUrl.append("&source="); helpUrl.append(newSource);
        if (focus != null) {
            String newFocus = focus.toLowerCase().replace(" ", "_");
            helpUrl.append("&focus="); helpUrl.append(newFocus);
        }
		
		// ... and write it
		try {
			pageContext.getOut().write(helpUrl.toString());
		} catch (IOException ioe) {
			throw new JspException(ioe.getMessage());
		}
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * Standard JavaBeans style property setter for the source.
	 * 
	 * @param source
	 *            a String representing the source element
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * Standard JavaBeans style property setter for the focus.
	 * 
	 * @param focus
	 *            a String representing the focus element
	 */
	public void setFocus(String focus) {
		this.focus = focus;
	}
}