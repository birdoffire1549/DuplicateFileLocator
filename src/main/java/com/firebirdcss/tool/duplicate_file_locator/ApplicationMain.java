package com.firebirdcss.tool.duplicate_file_locator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.firebirdcss.tool.duplicate_file_locator.data.ScanType;
import com.firebirdcss.tool.duplicate_file_locator.data.Settings;
import com.firebirdcss.tool.duplicate_file_locator.util.Utilities;

/**
 * The main class of the application.
 * This is the class which contains the Main Method where all execution begins.
 * 
 * @author Scott Griffis
 * <p>
 * Date: 03/09/2023
 *
 */
public class ApplicationMain {
    private static ArrayList<DirectoryProcessor> scanProcesses = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    
    /**
     * MAIN METHOD: 
     * This is the main method and entry point for the start of the application's runtime.
     * 
     * @param args - The arguments passed into the application as a {@link String} array
     */
    public static void main(String[] args) {
        processApplicationArgs(args);
        displayScanSummary(scanner);
        
        /* Creating and starting local scans for all applicable directories */
        for (String scan : Settings.localScanPaths) { // Iterate localScanPaths...
            DirectoryProcessor dp = new DirectoryProcessor(scan, ScanType.LOCAL_SCAN);
            Thread t = new Thread(dp);
            t.start();
            scanProcesses.add(dp);
        }
        
        /* Creating and starting full scans for all applicable directories */
        for (String scan : Settings.fullScanPaths) { // Iterate fullScanPaths...
            DirectoryProcessor dp = new DirectoryProcessor(scan, ScanType.FULL_SCAN);
            Thread t = new Thread(dp);
            t.start();
            scanProcesses.add(dp);
        }
        
        /* Wait for all scanning processes to complete */
        while (scanProcesses.stream().anyMatch(p -> p.isScanning() == true)) { // At least one process is running...
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // Do nothing...
            }
        }
        
        /* Notify user of scan being complete */
        System.out.println("\n\nScan is complete.\n");
        Utilities.shortPause();
        
        /* Fetch and process the scan results */
        Map<String, ArrayList<String>> results = scanProcesses.get(0).getResults();
        doUserCleanup(scanner, results);
        System.out.println("\nCleanup is complete.");
        
        /* Shutdown and cleanup the application resources */
        scanProcesses.forEach(p -> p.shutdown());
        scanner.close();
        
        /* End of application */
        System.out.println("\n\n~ Application Complete ~");
        System.exit(0);
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method handles walking the user though all of the results of the scans and enables
     * the user to decide what they want to do about each of the items discovered.
     * 
     * @param sc - An instance of {@link Scanner}
     * @param scanResults - A {@link Map} containing the scan results
     */
    private static void doUserCleanup(Scanner sc, Map<String, ArrayList<String>> scanResults) {
        boolean isRunning = true;
        for (Entry<String, ArrayList<String>> e : scanResults.entrySet()) { // Iterate though the scan results...
            boolean fullRepeat;
            do {
                fullRepeat = false;
                e = Utilities.filterEntryForExemptions(e);
                if (!e.getValue().isEmpty()) { // There is something to cleanup...
                    boolean repeat = false;
                    do {
                        repeat = false;
                        if (!isRunning) break;
                        
                        /* Calculate file size if possible */
                        long fileSize = -1;
                        long wastedSpace = -1;
                        try {
                            fileSize = Files.size(Path.of(e.getValue().get(0)));
                            wastedSpace = fileSize * (e.getValue().size() - 1);
                            
                        } catch (IOException e1) {
                            // Just move on...
                        }
                        
                        /* Display file information header */
                        Utilities.clearConsole();
                        if (fileSize > -1) { // File size was able to be determined...
                            System.out.println("\nDuplicates for file hash '" + e.getKey() + "'\n    File Size '" + Utilities.humanReadableSize(fileSize) + "'; Potentially wasted space '" + Utilities.humanReadableSize(wastedSpace) + "':");
                        } else { // File size could not be determined...
                            System.out.println("\nDuplicates for file hash '" + e.getKey() + "':");
                        }
                        
                        /* Display paths with choice selection bullet */
                        for (int i = 1; i <= e.getValue().size(); i++) { 
                            String dir = e.getValue().get(i - 1);
                            System.out.println("\t" + i + ") " + dir);
                        }
                        
                        /* Display options for how the user wishes to proceed */
                        System.out.println("\n\nOptions:");
                        System.out.println("\t(K)eep item, trash others");
                        System.out.println("\t(S)kip this item");
                        System.out.println("\tRemove (a)ll items");
                        System.out.println("\t(E)xclude new patttern");
                        System.out.println("\tE(x)it application");
                        
                        /* Prompt user for the desired option */
                        System.out.print("\nEnter letter for desired option: ");
                        String option = sc.nextLine();
                        
                        /* Process the user's input */
                        switch (option.toLowerCase()) { // Selected option converted to lower-case for acting on...
                            case "exclude":
                            case "e":
                                Utilities.getNewExclusionPattern(sc);
                                fullRepeat = true;
                                break;
                            case "keep":
                            case "k":
                                while (true) { // Infinite loop that must be broken out of to continue...
                                    System.out.print("Enter number for file you wish to keep: ");
                                    String num = sc.nextLine();
                                    if (!Utilities.askUserIfSure(sc)) { // User was not sure...
                                        
                                        break; // <-- Exit the While (true) loop...
                                    }
                                    try {
                                        int n = Integer.parseInt(num);
                                        n = n - 1;
                                        if (n <= 0 && n > e.getValue().size()) { // Value given is outside of valid reange...
                                            
                                            throw new NumberFormatException();
                                        }
                                        ArrayList<String> toDelete = new ArrayList<>();
                                        for (int i = 0; i < e.getValue().size(); i++) { // Iterate all paths...
                                            if (i != n) { // Index of path is not the one to keep...
                                                toDelete.add(e.getValue().get(i));
                                            }
                                        }
                                        Utilities.deleteFiles(toDelete);
                                        break; // <-- Exit the While (true) loop...
                                    } catch (NumberFormatException er) { // Input was non-numeric or out of range...
                                        System.out.println("\nERROR: Input value was invalid; Try again!");
                                        Utilities.shortPause();
                                    }
                                }
                                break;
                            case "skip":
                            case "s":
                                continue; // <-- Continue the for loop...
                            case "all":
                            case "a":
                                if (Utilities.askUserIfSure(sc)) {
                                    Utilities.deleteFiles(e.getValue());
                                }
                                break;
                            case "exit":
                            case "x":
                                if (Utilities.askUserIfSure(sc)) {
                                    isRunning = false;
                                } // ELSE: Continue on with the application...
                                break;
                            default:
                                System.out.println("\nERROR: Invalid input; Try again!");
                                Utilities.shortPause();
                                repeat = true;
                                break;
                        }
                        
                        Utilities.shortPause();
                    } while (repeat);
                }
            } while(fullRepeat);
        }
        
        if (scanResults.isEmpty()) {
            System.out.println("\nThere were no duplicate items to cleanup.");
            Utilities.shortPause();
        }
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method is used to display a summary of the upcoming scan.
     * 
     * @param sc - This is a {@link Scanner} to be used for gathering input.
     */
    private static void displayScanSummary(Scanner sc) {
        String list;
        String message = 
            "Welcome to the Duplicate File Locator applicaiton!" +
            "\n\n" +
            "==== Summary of scan that will be running ====\n\n" +
            "Full scan directories:\n" + 
                ((list = Settings.fullScanPaths.stream().reduce("", (a,b) -> a + "\t" + b + "\n")).isBlank() ? "\tNone\n" : list) +
            "\n" +
            "Local scan directories:\n" + 
                ((list = Settings.localScanPaths.stream().reduce("", (a,b) -> a + "\t" + b + "\n")).isBlank() ? "\tNone\n" : list) +
            "\n" +
            "Exclusion patterns:\n" + 
                ((list = Settings.exemptPatterns.stream().reduce("", (a,b) -> a + "\t" + b + "\n")).isBlank() ? "\tNone\n" : list)
        ;
        Utilities.clearConsole();
        System.out.println(message);
        System.out.print("\n\nPress Enter to continue: ");
        sc.nextLine();
        System.out.println("\n");
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method is where the start of the processing for the application's
     * arguments begins, as such the arguments are passed into this method for processing.
     * 
     * @param args - The application arguments as an array of {@link String}.
     */
    private static void processApplicationArgs(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) { // Iterate though arguments...
                switch (args[i].toLowerCase()) { // Handle current lower-cased argument...
                    case "-s":
                    case "--scan":
                        i++;
                        Settings.fullScanPaths.add(args[i]);
                        break;
                    case "-e":
                    case "--exempt":
                        i++;
                        Settings.exemptPatterns.add(args[i]);
                        break;
                    case "-l":
                    case "--local-scan":
                        i++;
                        Settings.localScanPaths.add(args[i]);
                        break;
                    case "-?":
                    case "-h":
                    case "--help":
                        showHelp(); // <-- Application exit happens in here.
                        break;
                    default:
                        System.out.println("ERROR: Missing or invalid application argument(s) given!\n\n");
                        showHelp(); // <-- Application exit happens in here.
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage() + "\n\n");
            showHelp();
        }
        
        reconcileScanPaths();
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method is in charge of delegating the work to the appropriate 
     * methods to reconcileScanPaths such that no files will be reported
     * as duplicates of themselves due to overlapping scans. 
     */
    private static void reconcileScanPaths() {
        if (Settings.fullScanPaths.isEmpty() && Settings.localScanPaths.isEmpty()) { // Nothing to scan...
            System.out.println("ERROR: At least one directory must be listed to scan!\n\n");
            showHelp(); // <-- Application exit happens in here.
        } else { // Something to scan...
            pruneFullScansThatAreCoveredByMoreInclusiveScans();
            pruneLocalScanPathsCoveredByFullScans();
        }
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method is used to prune directories from the {@link Settings.fullScanPaths} set which are 
     * covered by other scans also listed in the fullScanPaths set, such that files won't be reported
     * to duplicate themselves.
     */
    private static void pruneFullScansThatAreCoveredByMoreInclusiveScans() {
        String[] aFullScanPaths = (String[]) Settings.fullScanPaths.toArray(new String[] {});
        ArrayList<String> pruneList = new ArrayList<>();
        ArrayList<Integer> pMovedToPruneList = new ArrayList<>();
        
        for (int i = 0; i < aFullScanPaths.length; i++) {
            for (int p = 0; p < aFullScanPaths.length; p++) {
                if (p == i || pMovedToPruneList.contains(Integer.valueOf(p))) { // Item is itself or was already moved to pruneList...
                    continue;
                } else { // Item not itself and not pruned so analyze it...
                    String iPath = aFullScanPaths[i];
                    String pPath = aFullScanPaths[p];
                    
                    if (iPath.startsWith(pPath)) { // pPath is more inclusive...
                        pruneList.add(iPath);
                    } else if (pPath.startsWith(iPath)) { // iPath is more inclusive...
                        pruneList.add(pPath);
                        pMovedToPruneList.add(Integer.valueOf(p));
                    }
                }
            }
        }
        
        /* Act on the pruneList */
        for (String item : pruneList) {
            Settings.fullScanPaths.remove(item);
        }
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * This method is used to prune the {@link Settings.LocalScanPaths} set 
     * such that any references which would scan a directory that is already
     * scanned by a full scan, is removed to prevent files from being reported
     * as duplicates of themselves.
     */
    private static void pruneLocalScanPathsCoveredByFullScans() {
        ArrayList<String> pruneList = new ArrayList<>();
        pruneList.clear();
        
        /* Build a list of paths to prune from localScanPaths */
        for (String local : Settings.localScanPaths) {
            for (String full : Settings.fullScanPaths) {
                if (local.startsWith(full)) {
                    pruneList.add(local);
                }
            }
        }
        
        /* Act on the pruneList */
        for (String item : pruneList) {
            Settings.localScanPaths.remove(item);
        }
    }
    
    /**
     * PRIVATE STATIC METHOD:
     * Shows the help message to the console for the user to see.
     */
    private static void showHelp() {
        String message = 
            "Usage: java <commandName> [-options]\n" + 
            "where options include:\n" + 
            "    -s --scan <dir>           Perform a scan of given directory and all sub-directories.\n" +
            "    -l --local-scan <dir>     Perform a scan of only given directory, no sub-directories.\n" +
            "    -e --excempt <pattern>    Exempt a directory or file based on a given pattern.\n" + 
            "        Patterns are as follows:\n" +
            "            - *<path>*\tContains path\n" +
            "            - <path>*\tStarts with path\n" +
            "            - *<path>\tEnds with path\n" +
            "    -? -h --help              Prints this help message\n" +
            "NOTE: At least one directory to scan must be specified.\n\n"
        ;
        System.out.println(message);
        
        System.exit(0);
    }
}
