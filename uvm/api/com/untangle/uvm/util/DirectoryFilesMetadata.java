/**
 * $Id$
 */
package com.untangle.uvm.util;

/**
 * Metadata class to represent files from a given directory with a specified match pattern
 */
public class DirectoryFilesMetadata {

    // Files directory
    private String directory;
    // Pattern to match the files in the directory
    private String fileMatchPattern;

    /**
     * Initialize instance of DirectoryFilesMetadata.
     * @param directory directory of the files.
     * @param fileMatchPattern Pattern to match the files in the directory.
     * @return instance of DirectoryFilesMetadata.
     */
    public DirectoryFilesMetadata(String directory, String fileMatchPattern) {
        this.directory = directory;
        this.fileMatchPattern = fileMatchPattern;
    }

    /**
     * Get the directory
     * @return the directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Sets the directory value
     * @param directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
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
     *  @return string representation of DirectoryFilesMetadata instance
     */
    @Override
    public String toString() {
        return "DirectoryFilesMeta [directory=" + directory + ", fileMatchPattern=" + fileMatchPattern + "]";
    }

}
