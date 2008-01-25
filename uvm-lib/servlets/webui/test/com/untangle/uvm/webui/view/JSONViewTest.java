package com.untangle.uvm.webui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import com.untangle.uvm.webui.view.JSONView;

/**
 * @author Catalin Matei
 */
public class JSONViewTest extends TestCase {
	public static void main(String[] args) {
		junit.textui.TestRunner.run(JSONViewTest.class);
	}

	private MockServletContext servletContext;
	private MockHttpServletRequest servletRequest;
	private MockHttpServletResponse servletResponse;

	public JSONViewTest(String testName) {
		super(testName);
	}

	protected void setUp() throws Exception {
		servletContext = new MockServletContext();
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(servletContext);
		servletRequest = new MockHttpServletRequest(servletContext);
		servletResponse = new MockHttpServletResponse();
	}

	public void testRenderObjectGraph() throws Exception {
		JSONView view = new JSONView();
		Map model = new HashMap();
		model.put("bool", Boolean.TRUE);
		model.put("integer", new Integer(1));
		model.put("str", "string");
		Map bean = new HashMap();
		bean.put("name", "mybean");
		bean.put("bools", new boolean[] { true, false });
		model.put("bean", bean);
		
//		List list  = new ArrayList<String>();
//		list.add("aaa");
//		list.add("bbb");
//		model.put("list", list);

		view.render(model, servletRequest, servletResponse);
//		System.out.println(servletResponse.getContentAsString());
		assertEquals("{\"integer\":1,\"bean\":{\"bools\":[true,false],\"name\":\"mybean\"},\"bool\":true,\"str\":\"string\"}", 
				servletResponse.getContentAsString());
	}

	public void testRenderSimpleProperties() throws Exception {
		JSONView view = new JSONView();
		Map model = new HashMap();
		model.put("bool", Boolean.TRUE);
		model.put("integer", new Integer(1));
		model.put("str", "string");

		view.render(model, servletRequest, servletResponse);
		assertEquals("{\"integer\":1,\"bool\":true,\"str\":\"string\"}", 
				servletResponse.getContentAsString());
	}

	protected void tearDown() throws Exception {
	}

}