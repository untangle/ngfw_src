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
 *M
 * @Author Nick Childers
 */

//TODO: add negation flag - check multiple content options in one signature

public class ContentOption extends IDSOption {

	private int start;
	private int end;

	private Pattern contentPattern;
	
	private byte[] bytePattern;	
	private ByteBuffer binaryBuffer = ByteBuffer.allocate(2048);
	
	private boolean hasBinaryData = false;
	
	public ContentOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		params = params.replaceAll("\"","");
		int index = params.indexOf('|');
		if(index < 0) {
			hasBinaryData = false;
			contentPattern = Pattern.compile(params, Pattern.LITERAL);
		}
		else {
			hasBinaryData = true;
			buildBytePattern(params, index);
			bytePattern = new byte[binaryBuffer.position()];
			binaryBuffer.flip();
			binaryBuffer.get(bytePattern);
			//System.out.println("\n\n:::BYTE PATTERN:::");
			//for(int i = 0; i < bytePattern.length; i++)
			//	System.out.print(Integer.toHexString(bytePattern[i]));
		}	
	}
	
	public void setNoCase() {
		//if(contentPattern != null)
			contentPattern = Pattern.compile(contentPattern.pattern(), contentPattern.flags() | Pattern.CASE_INSENSITIVE);
	}


	public boolean run() {
		ByteBuffer eventData = signature.getSessionInfo().getEvent().data();
		if(hasBinaryData) {
			return binaryFind(eventData.array(), bytePattern);
		}
		else {
			String data = new String(eventData.array());
			return contentPattern.matcher(data).find();
		}
	}

	/**Worst pattern matcher EVAR.
	 * Replace this with something more effective.
	 * Please.
	 * implement byte based Boyer-Moore ?
	 */
	
	private boolean binaryFind(byte[] data, byte[] pattern) {
		int position = 0;
		int returnIndex = 0;
		boolean checkingPattern = false;
		
		for(int i=0; i < data.length; i++) {
			
			if(!checkingPattern)
				returnIndex++;
			
			if(data[i] == pattern[position]) {
				position++;
				if(position == 1) {
					checkingPattern = true;
					returnIndex = i+1;
				}
				if(position == pattern.length)
					return true;
			}
			else {
				checkingPattern = false;
				position = 0;
				i = returnIndex;
			}
		}
		return false;
	}
	
	/**
	 * This must parse strings in the form of "|byte byte byte| string | byte | string" etc...
	 * This class will recursivly call itself to iterate over all the | byte | sections, calling 
	 * a sub function to put those bytes in the objects binaryBuffer. It will parse out the
	 * string sections using a second sub function. That second sub function will also put all 
	 * the bytes of the string onto the byte buffer.
	 *
	 * Note: this means that the nocase option cannot work - no way (that I know of) to use case
	 * insensitivity when the string is converted into bytes 
	 */
	
	private void buildBytePattern(String params, int index) {
		//while(sub
		if(index == 0) {
			index = params.indexOf('|', 1);
			String bytes = params.substring(1,index);
			bytes = bytes.replaceAll(" ", "");
			parseByteStringPattern(bytes);
			String substring = params.substring(index+1);
			index = substring.indexOf('|',1) ;
			if(substring.length() > 0)
				buildBytePattern(substring, index);
		}
		else {
			if(index < 0)
				index = params.length();
			parseCharBytesPattern(params.substring(0,index));
			String substring = params.substring(index);
			index = 0;
			if(substring.length() > 0) 
				buildBytePattern(substring, index);
		}	
	}

	private void parseCharBytesPattern(String params) {
		if(params.length() > binaryBuffer.remaining()) {
			System.out.println("Very larger error");
			return;
		}
		binaryBuffer.put(params.getBytes());
	}

	private void parseByteStringPattern(String bytes) {
		if(bytes.length()%2 != 0 || bytes.length() > binaryBuffer.remaining()) {
			System.out.println("Very larger error"); //throw error
			return;
		}
		for(int i = 0; i < bytes.length()/2; i++) {
			byte head =  Byte.parseByte(""+bytes.charAt(2*i),16);
			byte tail = Byte.parseByte(""+bytes.charAt(2*i+1),16);
			byte tmpByte = head;
			tmpByte *= 16;
			tmpByte = (byte) (tmpByte | tail);
			binaryBuffer.put(tmpByte);
		}
	}
}
