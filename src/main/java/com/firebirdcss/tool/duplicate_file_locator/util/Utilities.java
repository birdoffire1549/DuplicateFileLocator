package com.firebirdcss.tool.duplicate_file_locator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.firebirdcss.tool.duplicate_file_locator.data.Settings;

/**
 * This class is the Utilities class.
 * Here is where all of the utility methods of the application are kept.
 * All methods are statically accessible and this class is never instantiated.
 * 
 * @author Scott Griffis
 * <p>
 * Date: 03/09/2023
 *
 */
public class Utilities {
    private Utilities() {} // Prevent instantiation
    
    /**
     * The purpose of this utility method is to perform a MD5 hash on a
     * given file and return the hash to the caller.
     * 
     * @param file - The file to hash as {@link File}
     * 
     * @return Returns the file's hash as a {@link String}
     * 
     * @throws FileNotFoundException Indicates the given file is missing and cannot be found.
     * @throws IOException Indicates there was a problem while reading the file.
     * @throws NoSuchAlgorithmException Indicates the JVM doesn't have the ability to perform a hash.
     */
    public static String hashFile(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
        String result = null;
        MessageDigest digest = MessageDigest.getInstance("MD5");
        
        /* Read the bytes of the file and add them to the digest */
        try (FileInputStream fis = new FileInputStream(file);) {
            byte[] bytes = new byte[1024];
            int bytesCount = 0;
            
            while ((bytesCount = fis.read(bytes)) != -1) {
                digest.update(bytes, 0, bytesCount);
            }
        }
        
        /* Obtain the hash bytes from the digest */
        byte[] hashBytes = digest.digest();
        
        /* Convert the hash bytes to a string */
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hashBytes.length; i++) {
            sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        
        result = sb.toString();
        
        return result;
    }
    
    /**
     * This method matches the given file path against the list of exclusion patterns
     * to see if the path is acceptable to be scanned.
     * 
     * @param path - The path to analyze as {@link String}
     * @param exclusionPatterns - The exclusion patterns as a {@link List} of type {@link String}
     * 
     * @return Returns a <code>boolean</code>, true if the path can be scanned and false if it shouldn't be.
     */
    public static boolean allowScan(String path, List<String> exclusionPatterns) {
        for (String p : exclusionPatterns) {  // Iterate all exclusion patterns...
            if (p.startsWith("*") && p.endsWith("*")) { // check if path contains...
                if (p.equals("*")) { // Pattern is only a star so excludes all...
                    
                    return false;
                } else if (path.contains(p.substring(1, p.length() - 1))) { // Path matches contains pattern...
                    
                    return false;
                }
            } else if (p.startsWith("*")) { // check if path ends with...
                if (path.endsWith(p.substring(1))) { // Path matches ends with pattern...
                    
                    return false;
                }
            } else if (p.endsWith("*")) { // check if starts with...
                if (path.startsWith(p.substring(0, p.length() - 1))) { // Path starts with pattern...
                    
                    return false;
                }
            } else { // check if matches...
                if (path.equals(p)) { // Path matches pattern...
                    
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * This method's job is to delete all of the files referenced by
     * a given list of paths.
     * 
     * @param toDelete - A {@link List} of file paths to delete as type {@link String}
     */
    public static void deleteFiles(List<String> toDelete) {
        for (String path : toDelete) { // Iterate the list of file paths to delete...
            File file = new File(path);
            if (!file.delete()) { // File couldn't be deleted...
                System.out.println("\nERROR: File could not be deleted: " + path);
                shortPause();
            }
        }
    }
    
    /**
     * This method is used to perform a short pause 
     * for about 2 seconds in length.
     */
    public static void shortPause() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            // Do nothing...
        }
    }
    
    /**
     * This method is used to prompt the user as to if they are sure
     * about a previously answered question and return a true if the 
     * user was sure or a false if they were not.
     * 
     * @param sc - A scanner instance as {@link Scanner}
     * @return Returns a <code>boolean</code>
     */
    public static boolean askUserIfSure(Scanner sc) {
        do { // Loop forever until user properly answers prompt...
            System.out.print("Are you sure (y/n)? ");
            String opt = sc.nextLine();
            if (opt.equalsIgnoreCase("y") || opt.equalsIgnoreCase("yes")) { // User is sure...
                
                return true;
            } else if (opt.equalsIgnoreCase("n") || opt.equalsIgnoreCase("no")) { // User is not sure...
                
                return false;
            } else { // Input was invalid...
                System.out.println("ERROR: Invalid input; Try again!");
                shortPause();
            }
        } while (true);
    }
    
    /**
     * This method is used to invoke a clearing of the
     * terminal console.
     */
    public static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush(); 
    }
    
    /**
     * Modifies the given number of bytes into something more 
     * humanly readable.
     * 
     * @param bytes - The number of bytes as <code>long</code>
     * @return Returns the value as a human readable {@link String}
     */
    public static String humanReadableSize(long bytes) {
        long result = bytes;
        if (result - 1000L < 0) return "" + result + " B";
        if ((result = result / 1024L) - 1000L < 0) return "" + result + " KB";
        if ((result = result / 1024L) - 1000L < 0) return "" + result + " MB";
        return "" + (result / 1024L) + " GB";
    }
    
    /**
     * This method is used to allow the user to add a new
     * exclusion pattern while the application is running.
     * 
     * @param sc - A scanner as {@link Scanner}
     */
    public static void getNewExclusionPattern(Scanner sc) {
        boolean retry = false;
        do {
            System.out.print("Enter a new exclusion pattern: ");
            String input = sc.nextLine();
            System.out.println("");
            if (input.isEmpty()) { // Input was nothing...
                System.out.println("ERROR: Invalid pattern; Try again!\n");
                retry = true;
            } else { // Input was something...
                Settings.postRunExemptPatterns.add(input);
            }
        } while (retry);
    }
    
    /**
     * This method is used to filter the contents of an {@link Entry} against the
     * postRunExemptPatterns, such that anything matching the pattern is removed from
     * the entry, of course if the removal of items leaves just one path then no paths
     * are returned because a single path cannot be a duplicate of itself.
     * 
     * @param entry - The Entry as an {@link Entry} of key type {@link String} and a value
     * of type {@link ArrayList} of type {@link String}
     * 
     * @return Returns an {@link Entry} with a key type of {@link String} and a value type of
     * {@link ArrayList} of {@link String} type.
     */
    public static Entry<String, ArrayList<String>> filterEntryForExemptions(Entry<String, ArrayList<String>> entry) {
        String key = entry.getKey();
        ArrayList<String> paths = new ArrayList<>();
        
        for (String item : entry.getValue()) { // Iterate items for matching to patterns...
            boolean matched = false;
            for (String pattern : Settings.postRunExemptPatterns) { // Iterate patterns for application...
                if (pattern.startsWith("*") && pattern.endsWith("*")) { // Contains or matched all pattern type...
                    if (pattern.equals("*")) { // Pattern is a matches all...
                        matched = true;
                    } else { // Pattern is a contains type...
                        if (item.contains(pattern.substring(1, pattern.length() - 1))) { // item contains the pattern...
                            matched = true;
                        }
                    }
                } else if (pattern.endsWith("*")) { // pattern is a starts with type... 
                    if (item.startsWith(pattern.substring(0, pattern.length() - 1))) { // item starts with pattern...
                        matched = true;
                    }
                } else if (pattern.startsWith("*")) { // pattern is a ends with type...
                    if (item.endsWith(pattern.substring(1))) {
                        matched = true;
                    }
                } else { // pattern is a equals type pattern...
                    if (item.equals(pattern)) {
                        matched = true;
                    }
                }
                
                if (matched) {
                    
                    break;
                }
            }
            
            if (!matched) {
                paths.add(item);
            }
        }
        
        if (paths.size() == 1) { // There is only one item, so no duplicates...
            Map<String, ArrayList<String>> result = new HashMap<>();
            result.put(key, new ArrayList<String>());
            Set<Entry<String, ArrayList<String>>> set = result.entrySet();
            Iterator<Entry<String, ArrayList<String>>> it = set.iterator();
            
            return it.next();
        } // ELSE: Return whatever paths contains...
        
        Map<String, ArrayList<String>> result = new HashMap<>();
        result.put(key, paths);
        Set<Entry<String, ArrayList<String>>> set = result.entrySet();
        Iterator<Entry<String, ArrayList<String>>> it = set.iterator();
        
        return it.next();
    }
}
