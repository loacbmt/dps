package efrei.refresh.dps;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PDFRenderer;

public class Pdf {
	
	private static PrintService bwPrinter = null;
	private static PrintService colorPrinter = null;
	
	private PDFFile pdfFile = null;
	String name = "";
	
	public static void initPrinters() {
		try {
			BufferedReader printers = new BufferedReader(new FileReader("res/printers.txt"));
			initPrinter(printers.readLine(), false);
			initPrinter(printers.readLine(), true);
			printers.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}
	
	private static void initPrinter(String name, boolean color) {
		PrintService printers[] = PrintServiceLookup.lookupPrintServices(null, null);
		for (PrintService ps : printers) {
			if (name.equals(ps.getName())) {
				if (color) colorPrinter = ps;
				else bwPrinter = ps;
			}
		}
	}
	
	public Pdf(String fileName) throws IOException {
		FileInputStream fis = new FileInputStream(fileName);
		byte[] pdfContent = new byte[fis.available()];
		fis.read(pdfContent, 0, fis.available());
		ByteBuffer bb = ByteBuffer.wrap(pdfContent);
		pdfFile = new PDFFile(bb);
		fis.close();
		name = fileName;
	}
	
	public int getNumPages() {
		return pdfFile.getNumPages();
	}

	public boolean print(boolean color) {
		PrinterJob pjob = PrinterJob.getPrinterJob();
		try {
			pjob.setPrintService(color ? colorPrinter : bwPrinter);
		} catch (PrinterException e) {
			System.out.println(e.getMessage());
			return false;
		}
		pjob.setJobName(name);
		
		PageFormat pf = pjob.defaultPage();
		Paper paper = new Paper();
		paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
		pf.setPaper(paper);
		
		Book book = new Book();
		book.append(new PDFPrintPage(pdfFile), pf, pdfFile.getNumPages());
		pjob.setPageable(book);
		
		try {
			pjob.print();
			return true;
		} catch (PrinterException e) {
			System.out.println(e.getMessage());
			return false;
		}
	}
}

class PDFPrintPage implements Printable {

	private PDFFile file;

	PDFPrintPage(PDFFile file) {
		this.file = file;
	}

	public int print(Graphics g, PageFormat format, int index) throws PrinterException {
		int pagenum = index + 1;
		if ((pagenum >= 1) && (pagenum <= file.getNumPages())) {
			Graphics2D g2 = (Graphics2D) g;
			PDFPage page = file.getPage(pagenum);

			Rectangle imageArea = new Rectangle((int) format.getImageableX(), (int) format.getImageableY(), (int) format.getImageableWidth(), (int) format.getImageableHeight());
			g2.translate(0, 0);
			PDFRenderer pgs = new PDFRenderer(page, g2, imageArea, null, null);
			try {
				page.waitForFinish();
				pgs.run();
			} catch (InterruptedException ie) {
				// nothing to do
			}
			return PAGE_EXISTS;
		} else {
			return NO_SUCH_PAGE;
		}
	}
}
