package com.firebirdcss.tool.duplicate_file_locator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.firebirdcss.tool.duplicate_file_locator.data.ScanType;
import com.firebirdcss.tool.duplicate_file_locator.data.Settings;
import com.firebirdcss.tool.duplicate_file_locator.util.Utilities;

/**
 * This class is the Directory Processor object.
 * Its job is to start a scan of a given scanPath and to start more instances of itself to
 * handle processing sub-directories when one is found in the path that it is processing.
 * 
 * @author Scott Griffis
 * <p>
 * Date: 03/09/2023
 *
 */
public class DirectoryProcessor implements Runnable {
    private static final ExecutorService execService = Executors.newFixedThreadPool(100);
    private static final Map<String/*FileHash*/, ArrayList<String/*FilePath*/>> processedFiles = new HashMap<>();;
    private static List<String> exclusionPatterns = null;
    
    private final ArrayList<DirectoryProcessor> subTasks = new ArrayList<>();
    
    private final String scanPath;
    private final ScanType scanType;
    
    private volatile boolean isRunning = true;
    private Thread myThread = null;
    
    /**
     * CONSTRUCTOR: 
     * This is the class constructor which is used to initialize the class during
     * its instantiation.
     * 
     * @param scanPath - The path to scan as {@link String}
     * @param scanType - The type of scan as {@link ScanType}
     */
    public DirectoryProcessor (String scanPath, ScanType scanType) {
        this.scanPath = scanPath;
        this.scanType = scanType;
        
        if (exclusionPatterns == null) {
            exclusionPatterns = Arrays.asList(Settings.exemptPatterns.toArray(new String[] {}));
        }
    }
    
    /**
     * This method returns the scanning state of the {@link DirectoryProcessor} and all
     * of it's sub-tasks such that if any are still processing then the returned result is
     * a <code>boolean</code> true.
     * 
     * @return Returns the scanning status as <code>boolean</code>
     */
    public boolean isScanning() {
        if (isRunning) { // This process is still running...
            
            return true;
        }
        
        /* Check for a sub-process that is still scanning */
        return subTasks.stream().anyMatch(t -> t.isScanning() == true);
    }
    
    /**
     * This method is used to shutdown the scanning process of this {@link DirectoryProcessor} instance
     * and all of its sub-tasks.
     */
    public void shutdown() {
        /* Shutdown this process */
        this.isRunning = false;
        if (this.myThread != null && this.myThread.isAlive()) {
            this.myThread.interrupt();
        }
        
        execService.shutdown();
        
        /* Shutdown all sub-processes */
        for (DirectoryProcessor t : this.subTasks) {
            t.shutdown();
        }
        
        /* Wait for all processes to stop working */
        while (isRunning || this.subTasks.stream().anyMatch(t -> t.isRunning == true)) { // Current process and/or a sub-process is running...
            try { // Try to sleep...
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // Carry on...
            }
        }
    }
    
    /**
     * This method can be used to fetch the scanning results at any-time during or 
     * after the scanning process. One should check the {@link isScanning} method to 
     * ensure that the scanning has stopped if they want the final results of the scan.
     * 
     * @return Returns a {@link Map} that is keyed by the File Hash as a {@link String}, where 
     * the value of the Map is an {@link ArrayList} of type {@link String} and the Strings are 
     * the absolute paths of files which are duplicates.
     */
    public Map<String/*FileHash*/, ArrayList<String/*FilePath*/>> getResults() {
        Map<String, ArrayList<String>> results = new HashMap<>();
        
        for (Entry<String, ArrayList<String>> e : processedFiles.entrySet()) { // Iterate processed file scan results...
            if (e.getValue().size() > 1) { // Processed file has duplicates...
                results.put(e.getKey(), e.getValue());
            }
        }
        
        return results;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        this.myThread = Thread.currentThread();
        System.out.println("Scanning directory: " + this.scanPath);
        File rootDir = new File(this.scanPath);
        if (isRunning && rootDir.exists() && rootDir.canRead() && rootDir.isDirectory()) { // Application is running and the root path is a directory that can be processed...
            File[] fileList = rootDir.listFiles();
            if (isRunning && fileList != null) { // Application is running and there is a file list to process...
                for (File item : fileList) { // Iterate the list of files and directories in current path...
                    if (isRunning) {
                        if (Utilities.allowScan(item.getAbsolutePath(), exclusionPatterns)) {
                            if (item.isDirectory() && this.scanType == ScanType.FULL_SCAN && item.canRead()) { // Item is a directory, can be read and scan type is Full Scan...
                                DirectoryProcessor dp = new DirectoryProcessor(item.getAbsolutePath(), this.scanType);
                                execService.execute(dp);
                                this.subTasks.add(dp);
                            } else if (item.isFile() && item.canRead()) { // Item is a file and can be read...
                                try { // Try to process the given file...
                                    processFile(item);
                                } catch (NoSuchAlgorithmException | IOException e) {
                                    System.out.println("ERROR: An error occurred while processing the file '" + item.getAbsolutePath() + "'");
                                }
                            }
                        }
                    } else { // The application is shutting-down...
                        
                        break;
                    }
                }
            }
            if (isRunning) {
                System.out.println("Scanning of directory complete: " + this.scanPath);
            }
        } else { // The application is shutting-down or the directory to process is not a directory or cannot be accessed...
            if (isRunning) {
                System.out.println("ERROR: The supplied directory is not valid; Check to ensure it exists and that it is readable by the process!\n\tDirectory: '" + rootDir.getAbsolutePath() + "'");
            }
        }
        isRunning = false;
    }
    
    /**
     * PRIVATE METHOD:
     * This method is used to process and store the information for the given file.
     * 
     * @param file - The file to process as {@link File}
     * 
     * @throws FileNotFoundException Indicates that the file is missing and cannot be found to process.
     * @throws NoSuchAlgorithmException Indicates that the JVM doesn't have the ability to hash the file.
     * @throws IOException Indicates a problem while reading the file for processing.
     */
    private void processFile(File file) throws FileNotFoundException, NoSuchAlgorithmException, IOException {
        String fileHash = null;
        
        /* Attempt to hash the file */
        try {
            fileHash = Utilities.hashFile(file);
        } catch(IOException e) {
            if (file.exists() && file.canRead()) {
                try {
                    fileHash = Utilities.hashFile(file);
                } catch(IOException e1) {
                    System.out.println("ERROR: Tried to process the given file but failed twice; Moving on to next file!\n\tGiven File: '" + file.getAbsolutePath() + "'");
                    
                    return;
                }
            }
        }
        
        /* Log the file path based on its hash */
        if (fileHash != null) {  // There is a file hash...
            if (processedFiles.containsKey(fileHash)) { // Hash has already been seen before...
                if (!processedFiles.get(fileHash).contains(file.getAbsolutePath())) { // The path is unique...
                    processedFiles.get(fileHash).add(file.getAbsolutePath());
                }
            } else { // This is the first time the hash has been seen...
                ArrayList<String> paths = new ArrayList<>();
                paths.add(file.getAbsolutePath());
                processedFiles.put(fileHash, paths);
            }
        }
    }
}
