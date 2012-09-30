package efrei.refresh.dps;

import java.io.IOException;
import java.text.ParseException;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FileTransferClient;

/**
 * Needed to execute through Windows Firewall
 * netsh advfirewall set global StatefulFTP disable
 **/

public class Ftp {
	
	public enum FileDirectory {
		QUEUE ("queue"),
		BACKUP ("old"),
		VALIDATION ("waiting");
		
		private String directory;

		FileDirectory(String s) {
			directory = s;
		}
		
		public boolean needPages() {
			return !this.equals(QUEUE);
		}
	}
	
	private static final String host = "admin.assos.efrei.fr";
	private static final String username = "bde";
	private static String password = "";
	
	private FileTransferClient ftp = null;
	
	public static void setPassword(String p) {
		password = p;
	}
	
	public Ftp () {
        ftp = new FileTransferClient();
        try {
            ftp.setRemoteHost(host);
            ftp.setUserName(username);
            ftp.setPassword(password);
        } catch (FTPException e) {
        	System.out.println(e.getMessage());
        }
	}
	
	public boolean connect() {
		try {
			ftp.connect();
			ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
			ftp.changeDirectory("www/dps");
			return true;
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	public void disconnect() {
		try {
			ftp.disconnect();
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public String[] getFilesNames(FileDirectory fd) {
		try {
			FTPFile[] files = ftp.directoryList(fd.directory);
			String[] filesNames = new String[files.length];
			for (int i = 0; i < files.length; i++) {
				filesNames[i] = files[i].getName();
	        }
			return filesNames;
		} catch (FTPException | IOException | ParseException e) {
			System.out.println(e.getMessage());
			return new String[0];
		}
	}
	
	public boolean getFile(String FileName, FileDirectory fd, int numPages) {
		try {
			ftp.downloadFile(FileName, fd.directory + '/' + (fd.needPages() ? numPages + "_"  : "") + FileName);
			return true;
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	public void moveFile(String FileName, int numPages, FileDirectory from, FileDirectory to) {
		try {
			ftp.rename(from.directory + '/' + (from.needPages() ? numPages + "_"  : "") + FileName, to.directory + '/' + (to.needPages() ? numPages + "_"  : "") + FileName);
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void deleteBackUpFile(String FileName, int numPages) {
		try {
			ftp.deleteFile("old/" + numPages + "_" + FileName);
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
		}
	}
}
