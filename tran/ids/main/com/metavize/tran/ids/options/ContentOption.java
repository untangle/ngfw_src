package com.metavize.tran.ids.options;

import java.util.regex.*;
import java.util.Vector;

import java.nio.ByteBuffer;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSDetectionEngine;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;

/**
 * This class matches the content option found in snort based rule signatures.
 *
 * @Author Nick Childers
 */

public class ContentOption extends IDSOption {

	private int start = 0;
	private int end = 0;

	private Pattern contentPattern;
	
	private byte[] bytePattern;	
	private ByteBuffer binaryBuffer = ByteBuffer.allocate(2048);
	
	private boolean hasBinaryData = false;
	
	public ContentOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		//params = params.replaceAll("\"","");
		int index = params.indexOf('|');
		if(index < 0) {
			contentPattern = Pattern.compile(params, Pattern.LITERAL);
		}
		else {
			buildBytePattern(params, index);
			bytePattern = new byte[binaryBuffer.position()];
			binaryBuffer.flip();
			binaryBuffer.get(bytePattern);
			String pattern = new String(bytePattern);
			contentPattern = Pattern.compile(pattern, Pattern.LITERAL);
		}	
	}
	
	public void setNoCase() {
		contentPattern = Pattern.compile(contentPattern.pattern(), contentPattern.flags() | Pattern.CASE_INSENSITIVE);
	}

	public void setOffset(int val) {
		start = val;
	}

	public void setDepth(int val) {
		end = start+val;
	}
		
	public boolean runnable() {
		return true;
	}
	
	public boolean run() {
		ByteBuffer eventData = getSignature().getSessionInfo().getEvent().data();
		String data = new String(eventData.array());
		if(start > data.length() || start < 0)
			return false;
		if(end <= 0 || end > data.length())
			end = data.length();
		return negationFlag() ^ contentPattern.matcher(data.substring(start)).find();
	}

	/**
	 * This must parse strings in the form of "|byte byte byte| string | byte | string" etc...
	 * This class will recursivly call itself to iterate over all the | byte | sections, calling 
	 * a sub function to put those bytes in the objects binaryBuffer. It will parse out the
	 * string sections using a second sub function. That second sub function will also put all 
	 * the bytes of the string onto the byte buffer.
	 */
	
	private void buildBytePattern(String params, int index) {
		if(index == 0) {
			index = params.indexOf('|', 1);
			String bytes = params.substring(1,index);
			bytes = bytes.replaceAll(" ", "");
			parseBinaryPattern(bytes);
			String substring = params.substring(index+1);
			index = substring.indexOf('|') ;
			
			if(substring.length() > 0)
				buildBytePattern(substring, index);
		}
		else {
			if(index < 0)
				index = params.length();
			parseASCIIPattern(params.substring(0,index));
			String substring = params.substring(index);
			index = 0;
			
			if(substring.length() > 0) 
				buildBytePattern(substring, index);
		}	
	}

	private void parseASCIIPattern(String params) {
		if(params.length() > binaryBuffer.remaining()) {
			System.out.println("Very larger error");
			return;
		}
		binaryBuffer.put(params.getBytes());
	}

	private void parseBinaryPattern(String bytes) {
		if(bytes.length()%2 != 0 || bytes.length() > binaryBuffer.remaining()) {
			System.out.println("Very larger error"); //throw error
			return;
		}
		for(int i = 0; i < bytes.length()/2; i++) {
			byte head =  Byte.parseByte(""+bytes.charAt(2*i),16);
			byte tail = Byte.parseByte(""+bytes.charAt(2*i+1),16);
			byte tmpByte = head;
			tmpByte  = (byte) (tmpByte << 4);
			tmpByte = (byte) (tmpByte | tail);
			binaryBuffer.put(tmpByte);
		}
	}
}
