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
	
	private static final String host = "XXX";
	private static final String username = "XXX";
	private static final String password = "XXX";
	
	private FileTransferClient ftp = null;
	
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
	
	public String[] getFilesNames(boolean backedUp) {
		try {
			FTPFile[] files = ftp.directoryList(backedUp ? "old" : "queue");
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
	
	public boolean getFile(String FileName, boolean backedUp, int numPages) {
		try {
			ftp.downloadFile(FileName, (backedUp ? "old/" + numPages + "_" : "queue/") + FileName);
			return true;
		} catch (FTPException | IOException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
	
	public void removeFileFromQueue(String FileName, int numPages) {
		try {
			ftp.rename("queue/" + FileName, "old/" + numPages + "_" + FileName);
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
