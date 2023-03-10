# DuplicateFileLocator
This project allows for the user to search for files which are duplicated on their system and decide to delete them or leave them alone.

##Application's terminal usage:
```
Usage: java -jar <jarName.jar> [-options]
where options include:
    -s --scan <dir>           Perform a scan of given directory and all sub-directories.
    -l --local-scan <dir>     Perform a scan of only given directory, no sub-directories.
    -e --excempt <pattern>    Exempt a directory or file based on a given pattern.
        Patterns are as follows:
            - *<path>*  Contains path
            - <path>*   Starts with path
            - *<path>   Ends with path
    -? -h --help              Prints this help message
NOTE: At least one directory to scan must be specified.
```