package efrei.refresh.dps;

public class PrintedDoc {
	
	private int nPages;
	private String fileName;
	private boolean waiting;
	
	public PrintedDoc (int nP, String fN) {
		nPages = nP;
		fileName = fN;
		setWaiting(false);
	}
	
	public String getLogin() {
		return fileName.substring(0, fileName.indexOf('@'));
	}
	
	public boolean isColored() {
		return fileName.charAt(fileName.indexOf('@') + 1) != 'F';
	}
	
	public int getNumPages() {
		return nPages;
	}
	
	public boolean isBinded() {
		return fileName.charAt(fileName.indexOf('@') + 2) == 'T';
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public float computePrice() {
		return (isColored() ? Dps.getColorPrice() : Dps.getBwPrice()) * nPages + (isBinded() ? Dps.getBindingPrice() : 0);
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean w) {
		waiting = w;
	}
}
