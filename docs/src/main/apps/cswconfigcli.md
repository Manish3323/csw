# csw-config-cli

A command line application that facilitates interaction with the Configuration Service. It accepts various commands to store, retrieve, list and manage configuration files.

## Supported Commands
* login
* logout
* create 
* update 
* get 
* delete 
* list 
* history 
* setActiveVersion 
* resetActiveVersion 
* getActiveVersion 
* getActiveByTime 
* getMetadata 
* exists 
* getActive

## login
`create, update, delete, setActiveVersion & resetActiveVersion` commands are admin protected. In order to use those, you need to login first.
You need to have valid user name and password to login which has the admin role assigned to it.

### Examples
1. 
```
csw-config-cli login
``` 
This opens up default browser on your machine and asks you to provide username and password. 
Once you provide valid credentials, AAS will respond with an access token, refresh token etc. which get stored on the local filesystem.
So next time when you use any of the above admin protected commands, this access token gets retrieved from local filesystem and is implicitly passed in a request sent to 
the Config Server.

2. 
```
csw-config-cli login --consoleLogin
``` 
Instead of opening the default browser on your machine, this will prompt for username and password on the console. (You do not need to leave console in this case.)

## logout
Use this command to logout if you are already logged in or you want to re-login with different credentials.

### Example
```
csw-config-cli logout
```
This command will delete all the tokens stored in local filesystem.

## Admin API
The commands listed below will be used by administrators and maintainers of Configuration Service.

### create
Takes an input source file and creates the configuration in the repository at a specified path.

 * 'relativeRepoPath' is path in the repository
 * `-i`, `--in` is input file path
 * `--annex` is optional parameter. Add this option to specify if the input file must be saved to annex store. This is usually the case if file is binary/large(>10 MiB)
 * `-c, --comment` optional create comment
 
#### Examples
1. 
```
csw-config-cli create /path/hcd/trombone.conf -i /Users/admin/configs/trombone.conf -c "Initial version"
```   
Creates a config file at path `/path/hcd/trombone.conf`, using the local file at `/Users/admin/configs/trombone.conf`, with `Initial version` as a comment.

2. 
```
csw-config-cli create /path/hcd/trombone.conf -i /Users/admin/large-configs/bigBinary.conf --annex
```   
Creates a config file at path `/path/hcd/trombone.conf`, using the local file at `/Users/admin/large-configs/bigBinary.conf`, file will be stored in annex store.
 
### update
Overwrites the file specified in the repository by the input file.

 * 'path' is path in the repository
 * `-i`, `--in` is input file path
 * `-c, --comment` optional create comment
 
#### Example
```
csw-config-cli update /path/hcd/trombone.conf -i /Users/foo/new_trombone.conf -c "new conf for next observation"
```    
Updates repository file `/path/hcd/trombone.conf`, with a local file at `/Users/foo/new_trombone.conf`, using the specified comment.
 
### get
Retrieves a file for a given path and saves it to the output file. The latest file is fetched if neither date nor id is specified.

 * 'relativeRepoPath' is path in the repository.
 * `-o`, `--out` is output file path
 * `--id` optional. if specified this id will be matched
 * `--date` optional. if specified will get the file matching this date. Format: 2017-04-16T16:15:23.503Z
 
#### Examples
1.
```
csw-config-cli get /path/hcd/trombone.conf -o /Users/bar/temp/latest_trombone.conf
```    
Gets repository file `/path/hcd/trombone.conf`, stores at the local disk location `/Users/bar/temp/latest_trombone.conf`

2.
```
csw-config-cli get /path/hcd/trombone.conf -o /Users/bar/temp/old_trombone.conf --id 10
```    
Gets version revision 10 of the repository file `/path/hcd/trombone.conf`, stores at the local disk location `/Users/bar/temp/old_trombone.conf`

### delete
Deletes the file at the specified path in the repository.
 
  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional delete comment
  
#### Example
```
csw-config-cli delete /path/hcd/outdated_trombone.conf -c monthly maintainance activity
```    
Deletes the repository file `/path/hcd/outdated_trombone.conf`, if it exists, using a comment
  
### list
Lists the files in the repository. Can't use '--annex' and '--normal' together.

 * `--annex` optional parameter. List all files that are of annex type. 
 * `--normal` optional parameter. List all files that are of normal type. 
 * `--pattern` optional parameter. List all files whose path matches the given pattern. e.g. "/path/hcd/*.*", "a/b/c/d.*", ".*.conf", ".*hcd.*"
 
### history
Shows the version history of the file in the repository.

* 'relativeRepoPath' is path in the repository
* `--max` optional parameter, indicating the maximum number of files to be retrieved

#### Example
```
csw-config-cli history /path/hcd/trombone.conf --max 25
```    
Prints the history of repository file `/path/hcd/trombone.conf`, with only 25 entries.


### setActiveVersion
Sets the active version of the file in the repository.

 * 'relativeRepoPath' is path in the repository
 * `--id` optional parameter, specifying the version ID of the repository file to set as active.
 * `-c, --comment` optional delete comment
 
#### Example
```
csw-config-cli setActiveVersion /path/hcd/trombone.conf --id 4 -c restoring last successful version.
```
Sets revision 4 to be active for the repository file `/path/hcd/trombone.conf`, using a comment.


### resetActiveVersion
Resets the active version to the latest version for the specified file path.

  * 'relativeRepoPath' is path in the repository
  * `-c, --comment` optional reset comment
  
#### Example
```
csw-config-cli resetActiveVersion /path/hcd/trombone.conf -c testing most recent config
```
Sets latest revision to be active for the repository file `/path/hcd/trombone.conf`, using a comment.
  
### getActiveVersion
Gets the ID of the active version of the file in the repository.

 * 'relativeRepoPath' is path in the repository
 
#### Example
```
csw-config-cli getActiveVersion /path/hcd/trombone.conf
```    
Gets active version ID for the repository file `/path/hcd/trombone.conf`.
 
### getActiveByTime
Gets the file that was active at a specified time.

  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  * `--date` optional. If specified, will get the active file matching this date. Format: 2017-04-16T16:15:23.503Z
  
#### Example
```
csw-config-cli getActiveByTime /path/hcd/trombone.conf -o /usr/tmp/last_week_trombone.conf --date 2017-05-09T07:29:53.242Z
```
Gets version of teh repository file `/path/hcd/trombone.conf`, that was active on `2017-05-09T07:29:53.242Z`, and saves it to local disk.
  
  
### getMetadata
Gets the metadata of the Configuration Service server e.g. repository directory, annex directory, min annex file size, max config file size.

#### Example
```
csw-config-cli getMetadata
```    
Prints the metadata on screen.

## Client API
The following commands are available for component developers.

### exists
Checks if the file exists at specified path in the repository.

 * 'relativeRepoPath' is path in the repository
 
#### Example
```
csw-config-cli exists /path/hcd/trombone.conf
```    
True if repository file `/path/hcd/trombone.conf` exists, false otherwise
 
### getActive
Retrieves active file for a given path from the Configuration Service and writes it to the output file.
  * 'relativeRepoPath' is path in the repository
  * `-o`, `--out` is output file path
  
#### Example
```
csw-config-cli getActive /path/hcd/trombone.conf -o /Users/bar/temp/scheduled_trombone.conf
```
Gets currently active version of the repository file `/path/hcd/trombone.conf`, stores to the local disk location `/Users/bar/temp/scheduled_trombone.conf`
  
## About this application 
 
### --help 
Prints the help message.

### --version 
Prints the version of the application.

@@@ note

All the above examples require that `csw-location-server` is running on local machine at `localhost:7654`.
If `csw-location-server` is running on a remote machine with an IP address or `172.1.1.2`, then you need to pass the additional `--locationHost 172.1.1.2` command line argument.
Example:
`csw-config-cli getMetadata --locationHost 172.1.1.2`

@@@
