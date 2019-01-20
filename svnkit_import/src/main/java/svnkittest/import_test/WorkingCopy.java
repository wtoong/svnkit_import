package svnkittest.import_test;

import java.io.File;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;

public class WorkingCopy {

    private static SVNClientManager ourClientManager;
    private static ISVNEventHandler myCommitEventHandler;
    private static ISVNEventHandler myUpdateEventHandler;
    private static ISVNEventHandler myWCEventHandler;
    
    // java -jar .\importtest-1.0.0.jar https://localhost:5000/svn/video C:\Users\rlaxo\Videos\181206 test test
    // java -jar .\importtest-1.0.0.jar https://localhost:5000/svn/video C:\Users\rlaxo\Videos\181207 test test
    
    // clean package shade:shade
    
    public static void main(String[] args) {
        /*
         * Initializes the library (it must be done before ever using the library 
         * itself)
         */
        setupLibrary();

        /*
         * Default values:
         */
        
        /*
         * Assuming that 'svn://localhost/testRep' is an existing repository path.
         * SVNURL is a wrapper for URL strings that refer to repository locations.
         */
        SVNURL repositoryURL = null;
        try {
            repositoryURL = SVNURL.parseURIEncoded("https://localhost:5000//svn//video"); // https://localhost:5000/video
        } catch (SVNException e) {
            //
        }
        String name = "test";
        String password = "test";
        String importDir = "C:\\Users\\rlaxo\\Videos\\181206";

        if (args != null) {
            /*
             * Obtains a URL that represents an already existing repository
             */
            try {
                repositoryURL = (args.length >= 1) ? SVNURL.parseURIEncoded(args[0]) : repositoryURL;
            } catch (SVNException e) {
                System.err.println("'" + args[0] + "' is not a valid URL");
                System.exit(1);
            }
            /*
             * Obtains a path to be a working copy root directory
             */
            importDir = (args.length >= 2) ? args[1] : importDir;
            /*
             * Obtains an account name 
             */
            name = (args.length >= 3) ? args[2] : name;
            /*
             * Obtains a password
             */
            password = (args.length >= 4) ? args[3] : password;
        }

        /*
         * That's where a local directory will be imported into.
         * Note that it's not necessary that the '/importDir' directory must already
         * exist - the SVN repository server will take care of creating it. 
         */
        SVNURL importToURL = repositoryURL;
              
        /*
         * Creating custom handlers that will process events
         */
        myCommitEventHandler = new CommitEventHandler();
        
        myUpdateEventHandler = new UpdateEventHandler();
        
        myWCEventHandler = new WCEventHandler();
        
        /*
         * Creates a default run-time configuration options driver. Default options 
         * created in this way use the Subversion run-time configuration area (for 
         * instance, on a Windows platform it can be found in the '%APPDATA%\Subversion' 
         * directory). 
         * 
         * readonly = true - not to save  any configuration changes that can be done 
         * during the program run to a config file (config settings will only 
         * be read to initialize; to enable changes the readonly flag should be set
         * to false).
         * 
         * SVNWCUtil is a utility class that creates a default options driver.
         */
        ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
        
        /*
         * Creates an instance of SVNClientManager providing authentication
         * information (name, password) and an options driver
         */
        ourClientManager = SVNClientManager.newInstance(options, name, password);
        
        /*
         * Sets a custom event handler for operations of an SVNCommitClient 
         * instance
         */
        ourClientManager.getCommitClient().setEventHandler(myCommitEventHandler);
        
        /*
         * Sets a custom event handler for operations of an SVNUpdateClient 
         * instance
         */
        ourClientManager.getUpdateClient().setEventHandler(myUpdateEventHandler);

        /*
         * Sets a custom event handler for operations of an SVNWCClient 
         * instance
         */
        ourClientManager.getWCClient().setEventHandler(myWCEventHandler);

        long committedRevision = -1;
        
        File anImportDir = new File(importDir);
        
        System.out.println("Importing a new directory into '" + importToURL + "'...");
        try{
            /*
             * recursively imports an unversioned directory into a repository 
             * and displays what revision the repository was committed to
             */
            boolean isRecursive = true;
            committedRevision = importDirectory(anImportDir, importToURL, "importing a new directory '" + anImportDir.getAbsolutePath() + "'", isRecursive).getNewRevision();
        }catch(SVNException svne){
            error("error while importing a new directory '" + anImportDir.getAbsolutePath() + "' into '" + importToURL + "'", svne);
        }
        System.out.println("Committed to revision " + committedRevision);
        System.out.println();
    }

    /*
     * Initializes the library to work with a repository via 
     * different protocols.
     */
    private static void setupLibrary() {
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup();
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup();
        
        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup();
    }

    /*
     * Imports an unversioned directory into a repository location denoted by a
     * destination URL (all necessary parent non-existent paths will be created 
     * automatically). This operation commits the repository to a new revision. 
     * Like 'svn import PATH URL (-N) -m "some comment"' command. It's done by 
     * invoking 
     * 
     * SVNCommitClient.doImport(File path, SVNURL dstURL, String commitMessage, boolean recursive) 
     * 
     * which takes the following parameters:
     * 
     * path - a local unversioned directory or singal file that will be imported into a 
     * repository;
     * 
     * dstURL - a repository location where the local unversioned directory/file will be 
     * imported into; this URL path may contain non-existent parent paths that will be 
     * created by the repository server;
     * 
     * commitMessage - a commit log message since the new directory/file are immediately
     * created in the repository;
     * 
     * recursive - if true and path parameter corresponds to a directory then the directory
     * will be added with all its child subdirictories, otherwise the operation will cover
     * only the directory itself (only those files which are located in the directory).  
     */
    private static SVNCommitInfo importDirectory(File localPath, SVNURL dstURL, String commitMessage, boolean isRecursive) throws SVNException{
        /*
         * Returns SVNCommitInfo containing information on the new revision committed 
         * (revision number, etc.) 
         */
        return ourClientManager.getCommitClient().doImport(localPath, dstURL, commitMessage, isRecursive);
        
    }
    /*
     * Displays error information and exits. 
     */
    private static void error(String message, Exception e){
        System.err.println(message+(e!=null ? ": "+e.getMessage() : ""));
        System.exit(1);
    }
}
