package ak.goobi.genios;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

public class FtpUpload {

	/**
	 * Uploads a file to an FTP-Server.
	 * 
	 * @param localFile		String: full path to a local file (e. g. /home/username/myfile.zip)
	 * @param remoteFile	String: filename (or path + filename) of the remote file (the one that should be created on the FTP-Server)
	 * @param host			String: the host name of the FTP-Server (e. g. my.ftpserver.com)
	 * @param port			int: the port of the FTP-Server (is probably 21)
	 * @param user			String: the username of the FTP-Account
	 * @param password		String: the password of the FTP-Account
	 * @param isBinary		boolean: is the file to transfer binary or ascii (use binary .zip, use ascii for .txt, .pdf, etc.)
	 * @param showMessages	boolean: show verbose messages
	 * @return				boolean
	 * @throws IOException
	 */
	public static boolean uploadFile( String localFile, String remotePath, String remoteFile, String host, int port, String user, String password, boolean isBinary, boolean showMessages ) throws IOException
	   {
	      FTPClient       ftpClient = new FTPClient();
	      FileInputStream fis = null;
	      boolean         ftpOk = true;    

	      try {
	         ftpClient.connect(host, port);
	         if (isBinary == true) {
		    	  ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		     }
	         if( showMessages ) { System.out.println( ftpClient.getReplyString() ); }
	         ftpOk &= ftpClient.login( user, password );
	         if( showMessages ) { System.out.println( ftpClient.getReplyString() ); }
	         ftpOk &= ftpClient.changeWorkingDirectory(remotePath);
	         if( showMessages ) { System.out.println( ftpClient.getReplyString() ); }
	         fis = new FileInputStream( localFile );
	         ftpOk &= ftpClient.storeFile( remoteFile, fis );
	         if( showMessages ) { System.out.println( ftpClient.getReplyString() ); }
	         ftpOk &= ftpClient.logout();
	         if( showMessages ) { System.out.println( ftpClient.getReplyString() ); }
	      } finally {
	         try { if( fis != null ) { fis.close(); } } catch( IOException e ) {/* nothing */}
	         ftpClient.disconnect();
	      }

	      return ftpOk;
	   }
}
