/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Metadata class to represent a file directory
 */
public class FileDirectoryMetadata {

    // File directory
    private File directory;
    // Pattern to match the files in the directory
    private String fileMatchPattern;

    /**
     * Initialize instance of FileDirectoryMetadata.
     * @param directory directory.
     * @param fileMatchPattern Pattern to match the files in the directory.
     * @return instance of FileDirectoryMetadata.
     */
    public FileDirectoryMetadata(String directory, String fileMatchPattern) {
        this.directory = new File(directory);
        this.fileMatchPattern = fileMatchPattern;
    }

    /**
     * Get the directory
     * @return the directory
     */
    public File getDirectory() {
        return directory;
    }

    /**
     * Sets the directory
     * @param directory
     */
    public void setDirectory(String directory) {
        this.directory = new File(directory);
    }

    /**
     * Get the fileMatchPattern
     * @return the fileMatchPattern
     */
    public String getFileMatchPattern() {
        return fileMatchPattern;
    }

    /**
     * Sets the file match pattern for the directory
     * @param fileMatchPattern
     */
    public void setFileMatchPattern(String fileMatchPattern) {
        this.fileMatchPattern = fileMatchPattern;
    }

    /**
    * Returns FileNameFilter lambda expression by regex
    * If matchRegEx is present and file name matches the regex then returns true, or else returns true
    * @param matchRegEx Regex to match the file name
    * @return
    */
    public static FilenameFilter getFileNameFilter(String matchRegEx) {
        /**
        * Accept matcher for file search
        * 
        * @param directory
        *        The file directory
        * @param name
        *        The file name
        * @return True to accept the file, false to reject
        */
        return (directory, name) -> StringUtil.isEmpty(matchRegEx) ? true : name.matches(matchRegEx);
    }    

    /**
     *  @return string representation of FileDirectoryMetadata instance
     */
    @Override
    public String toString() {
        return "FileDirectoryMetadata [directory=" + directory + ", fileMatchPattern=" + fileMatchPattern + "]";
    }

}
