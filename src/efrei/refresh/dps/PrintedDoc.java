package efrei.refresh.dps;

public class PrintedDoc {
	
	private String login;
	private boolean colored;
	private boolean binded;
	private int nbPages;
	private int timestamp;
	private String extension;
	
	public PrintedDoc (String fileName) {
		login = fileName.substring(0, fileName.indexOf('@'));
		colored = fileName.charAt(fileName.indexOf('@') + 1) != 'F';
		binded = fileName.charAt(fileName.indexOf('@') + 2) == 'T';
		nbPages = Integer.parseInt(fileName.substring(fileName.indexOf('@') + 3, fileName.indexOf('_')));
		timestamp = Integer.parseInt(fileName.substring(fileName.indexOf('_') + 1, fileName.indexOf('.')));
		extension = fileName.substring(fileName.indexOf('.'));
	}
	
	public String getLogin() {
		return login;
	}
	
	public boolean isColored() {
		return colored;
	}
	
	public boolean isBinded() {
		return binded;
	}
	
	public int getNumPages() {
		return nbPages;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
	
	public String getExtension() {
		return extension;
	}
	
	public String getFileName() {
		return login + (colored ? "@T" : "@F") + (binded ? 'T' : 'F') + nbPages + '_' + timestamp + extension;
	}
}
