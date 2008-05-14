/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/RemoteLanguageManagerImpl.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LanguageInfo;
import com.untangle.uvm.LanguageSettings;
import com.untangle.uvm.RemoteLanguageManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.util.DeletingDataSaver;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implementation of RemoteLanguageManagerImpl.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
class RemoteLanguageManagerImpl implements RemoteLanguageManager
{
    private static final String LANGUAGES_DIR;
    private static final String LOCALE_DIR;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String BASENAME_PREFIX = "i18n";
    private static final String LANGUAGES_CFG = "lang.cfg";  
    private static final String LC_MESSAGES = "LC_MESSAGES";
	private static final int BUFFER = 2048; 

    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContextImpl uvmContext;
    private LanguageSettings settings;

    RemoteLanguageManagerImpl(UvmContextImpl uvmContext) {
    	this.uvmContext = uvmContext;    	
    	
        TransactionWork tw = new TransactionWork()
        {
            public boolean doWork(Session s)
            {
                Query q = s.createQuery("from LanguageSettings");
                settings = (LanguageSettings)q.uniqueResult();

                if (null == settings) {
                	settings = new LanguageSettings();
                	settings.setLanguage(DEFAULT_LANGUAGE);
                    s.save(settings);
                }
                
                return true;
            }
        };
        uvmContext.runTransaction(tw);
    }

    // public methods ---------------------------------------------------------

	public LanguageSettings getLanguageSettings() {
		return settings;
	}

	public void setLanguageSettings(LanguageSettings settings) {
        /* delete whatever is in the db, and just make a fresh settings object */
		LanguageSettings copy = new LanguageSettings();
        settings.copy(copy);
        saveSettings(copy);
        this.settings = copy;
	}
	
    public void uploadLanguagePack(FileItem item) throws UvmException {
	    try {
	        BufferedOutputStream dest = null;
			ZipEntry entry = null;
			
			//validate language pack
		    if (!item.getName().endsWith(".zip")) {
				throw new UvmException("Invalid Language Pack");
		    }
			
	        // Open the ZIP file
		    InputStream uploadedStream = item.getInputStream();
		    ZipInputStream zis = new ZipInputStream(uploadedStream);
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					File dir = new File(LANGUAGES_DIR + File.separator + entry.getName());
					if (!dir.exists()) {
						dir.mkdir();
					}
				} else {
					int count;
					byte data[] = new byte[BUFFER];
					
					// write the files to the disk
//					String lang = entry.getName().substring(entry.getName().lastIndexOf(".")+1);
//					String destDir = LANGUAGES_DIR + File.separator + lang;
//					File destDirFile = new File(destDir);
//					if (!destDirFile.exists()) {
//						destDirFile.mkdir();
//					}
					
					FileOutputStream fos = new FileOutputStream(LANGUAGES_DIR + File.separator + entry.getName());
					dest = new BufferedOutputStream(fos, BUFFER);
					while ((count = zis.read(data, 0, BUFFER)) != -1) {
						dest.write(data, 0, count);
					}
					dest.flush();
					dest.close();
					
					//compile to .mo file and install it in the appropriate place (LOCALE_DIR)
					compileMoFile(entry);
					
					//compile the java properties version & install it in the classpath (LANGUAGES_DIR)
					compileResourceBundle(entry);
				}
			}
			zis.close();		    	
		    uploadedStream.close();
	    } catch (IOException e) {
	    	logger.error(e);
			throw new UvmException("Upload Language Pack Failed");
	    }
    }

	private void compileResourceBundle(ZipEntry entry) throws IOException,
			UvmException {
		boolean success = true;
		try {
			String tokens[] = entry.getName().split(File.separator);
			String lang = tokens[0];
			String moduleName = tokens[1].substring(0, tokens[1].lastIndexOf("."));
			
			String cmd[] = { "msgfmt", "--java2", 
					"-d", LANGUAGES_DIR,
					"-r", BASENAME_PREFIX + "." + moduleName,
					"-l", lang,
					LANGUAGES_DIR + File.separator + entry.getName()};
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			if (p.exitValue() != 0) {
				success = false;
			}
			
		} catch (Exception err) {
			success = false;
		}
		if (!success) {
			throw new UvmException("Error compiling to resource bundle");
		}
	}

	private void compileMoFile(ZipEntry entry) throws IOException, UvmException {
		boolean success = true;
		try {
			String tokens[] = entry.getName().split(File.separator);
			String lang = tokens[0];
			String moduleName = tokens[1].substring(0, tokens[1].lastIndexOf("."));
			
			String cmd[] = { "msgfmt",
					"-o", LOCALE_DIR + File.separator + lang + File.separator + LC_MESSAGES + File.separator + moduleName  + ".mo",
					LANGUAGES_DIR + File.separator + entry.getName()};
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			if (p.exitValue() != 0) {
				success = false;
			}
			
		} catch (InterruptedException err) {
			success = false;
		}				
		if (!success) {
			throw new UvmException("Error compiling to mo file");
		}
	}
	
    public List<LanguageInfo> getLanguagesList() {
        List<LanguageInfo> languages = new ArrayList<LanguageInfo>();
        
    	//get available languages
    	File dir = new File(LANGUAGES_DIR);
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.startsWith(BASENAME_PREFIX) 
					&& !name.equals(LANGUAGES_CFG);
			}
		};
		List<String> availableLanguages = Arrays.asList(dir.list(filter));
    	
		// Reading all languages from config file and keep only the one which we have translations for
		try {
			BufferedReader in = new BufferedReader(new FileReader(LANGUAGES_DIR + File.separator + LANGUAGES_CFG));
			String s = new String();
			while((s = in.readLine())!= null) {
				if (s.trim().length() > 0){
					String[] tokens = s.split("\\s");
					if (tokens.length >= 2) {
						String langCode = tokens[0];
						String langName = tokens[1];
						if (DEFAULT_LANGUAGE.equals(langCode) || 
								availableLanguages.contains(langCode)){
							languages.add(new LanguageInfo(tokens[0], tokens[1]));
						}
					}
				}
			}
			in.close();
		} catch (IOException e) {
//			e.printStackTrace();
		}
    		
    	return languages;
    }
    
    public Map<String, String> getTranslations(String module){
		Map<String, String> map = new HashMap<String, String>();
/*		
    	try {
    		I18n i18n = I18nFactory.getI18n("i18n."+module, module, Thread
    				.currentThread().getContextClassLoader(), new Locale(settings
    				.getLanguage()), I18nFactory.DEFAULT);
    		
    		if (i18n != null) {
    			for (Enumeration<String> enumeration = i18n.getResources().getKeys(); enumeration
    					.hasMoreElements();) {
    				String key = enumeration.nextElement();
    				map.put(key, i18n.tr(key));
    			}
    		}
		} catch (MissingResourceException e) {
			// Do nothing - Fall back to a default that returns the passed text if no resource bundle can be located
			// is done in client side
		}
*/		
	// We will use this approuch instead of the one above, because of a bug in ResouceBundle in jdk 1.5, related with cache:
	// I18N is using ResourceBundle.getBundle(baseName, locale, loader); to load a resouce bundle which caches the ResourceBundles, 
	// and if we tried once to load a resource bundle, second time will return not found (MissingResourceException), evean if 
	// the resource is available now; this prevent dynamic loading, which we need.
	try {
		Class clazz = Thread.currentThread().getContextClassLoader().loadClass(BASENAME_PREFIX + "." + module + "_" + settings.getLanguage());
		ResourceBundle resourceBundle = (ResourceBundle)clazz.newInstance();
		if (resourceBundle != null) {
			for (Enumeration<String> enumeration = resourceBundle.getKeys(); enumeration.hasMoreElements();) {
				String key = enumeration.nextElement();
				map.put(key, resourceBundle.getString(key));
			}
		}
	} catch (Exception e) {
		// Do nothing - Fall back to a default that returns the passed text if no resource bundle can be located
		// is done in client side
	}
		
		

		return map;
    }
    
    // private methods --------------------------------------------------------
    private void saveSettings(LanguageSettings settings) {
        DeletingDataSaver<LanguageSettings> saver = 
            new DeletingDataSaver<LanguageSettings>(uvmContext,"LanguageSettings");
        this.settings = saver.saveData(settings);
    }
    
    static {
    	LANGUAGES_DIR = System.getProperty("bunnicula.lang.dir"); //place for languages resources files
    	LOCALE_DIR = "/usr/share/locale"; // place for .mo files
    }
}
