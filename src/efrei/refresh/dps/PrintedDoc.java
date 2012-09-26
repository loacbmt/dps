package efrei.refresh.dps;

public class PrintedDoc {
	
	private String login;
	private boolean color;
	private int nPages;
	private boolean binding;
	private String fileName;
	
	public static String getLogin(String fN) {
		return fN.substring(0, fN.indexOf('@'));
	}
	
	public static boolean isColored(String fN) {
		return fN.charAt(fN.indexOf('@') + 1) != 'F';
	}
	
	public static boolean isBinded(String fN) {
		return fN.charAt(fN.indexOf('@') + 2) == 'T';
	}
	
	public PrintedDoc (int nP, String fN) {
		login = getLogin(fN);
		color = isColored(fN);
		nPages = nP;
		binding = isBinded(fN);
		fileName = fN;
	}
	
	public PrintedDoc (String l, boolean c, int nP, boolean b, String fN) {
		login = l;
		color = c;
		nPages = nP;
		binding = b;
		fileName = fN;
	}
	
	public String getLogin() {
		return login;
	}
	
	public boolean isColored() {
		return color;
	}
	
	public int getNumPages() {
		return nPages;
	}
	
	public boolean isBinded() {
		return binding;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public float computePrice() {
		return (color ? Dps.getColorPrice() : Dps.getBwPrice()) * nPages + (binding ? Dps.getBindingPrice() : 0);
	}
}
