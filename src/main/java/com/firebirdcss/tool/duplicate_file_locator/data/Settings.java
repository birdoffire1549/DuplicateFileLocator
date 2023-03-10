package com.firebirdcss.tool.duplicate_file_locator.data;

import java.util.HashSet;

/**
 * This class contains information regarding settings or data that is
 * to be universally accessible throughout the application.
 * All information in this class is statically accessible and this class
 * cannot be instantiated.
 * 
 * @author Scott Griffis
 * <p>
 * Date: 03/09/2023
 *
 */
public class Settings {
    private Settings() {} // Prevents instantiation...
    
    /**
     * This is a {@link HashSet} of type {@link String} which is used
     * to store paths of directories which are to be fully scanned
     * including their sub-directories.
     * The choice of a HashSet is so that it cannot contain duplicate entries.
     */
    public static final HashSet<String> fullScanPaths = new HashSet<>();
    
    /**
     * This is a {@link HashSet} of type {@link String} which is used
     * to store the paths of directories which are to be locally scanned
     * such that their sub-directories are not included.
     * The choice of a HashSet is so that it cannot contain duplicate entries.
     */
    public static final HashSet<String> localScanPaths = new HashSet<>();
    
    /**
     * This is a {@link HashSet} of type {@link String} which is used
     * to store the patterns for paths which are exempt from being scanned.
     * The choice of a HashSet is so that it cannot contain duplicate entries.
     */
    public static final HashSet<String> exemptPatterns = new HashSet<>();
    
    /**
     * This is a {@link HashSet} of type {@link String} which is used 
     * to store the patterns for paths which are to be exempt from processing
     * as added during the application's runtime through the appropriate prompt.
     *  The choice of a HashSet is so that it cannot contain duplicate entries.
     *  Also, this HashSet is separate from the exemptPatterns HashSet because 
     *  that one was processed and taken into account prior to scanning, so there
     *  is no point to looking at each of those patterns after the scan has completed.
     */
    public static final HashSet<String> postRunExemptPatterns = new HashSet<>();
}
