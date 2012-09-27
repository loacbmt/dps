package efrei.refresh.dps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Dps {

	private static final long msBetweenChecks = 5*60*1000;
	
	private static float bwPrice;
	private static float colorPrice;
	private static float bindingPrice;
	
	private static Lock working = new ReentrantLock();
	private static SortedDocsList docs = new SortedDocsList();
	private static Ui ui;

	public static void main(String[] args) {
		Timer t = new Timer();
		QueueProcessor qp = new QueueProcessor();
		
		try {
			BufferedReader prices = new BufferedReader(new FileReader("prices.txt"));
			bwPrice = Float.parseFloat(prices.readLine());
			colorPrice = Float.parseFloat(prices.readLine());
			bindingPrice = Float.parseFloat(prices.readLine());
			prices.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}
		// TODO: Get FTP password
		Ftp.setPassword("XXX");
		
		ui = new Ui(t);
		reload();
		ui.update();
		qp.run();
		ui.update();
		t.schedule(qp, (msBetweenChecks) - (System.currentTimeMillis() % (msBetweenChecks)), msBetweenChecks);
	}
	
	public static float getBwPrice() {
		return bwPrice;
	}
	
	public static float getColorPrice() {
		return colorPrice;
	}
	
	public static float getBindingPrice() {
		return bindingPrice;
	}
	
	public static Lock Working() {
		return working;
	}

	public static SortedDocsList getDocs() {
		return docs;
	}
	
	public static Ui getUi() {
		return ui;
	}
	
	private static void reload() {
		Ftp ftp = new Ftp();
		if (ftp.connect()) {
			String[] filesNames = ftp.getFilesNames(Ftp.FileDirectory.BACKUP);
			for (String file : filesNames) {
				Dps.getDocs().add(new PrintedDoc(Integer.parseInt(file.substring(0, file.indexOf('_'))), file.substring(file.indexOf('_') + 1)));
			}
			filesNames = ftp.getFilesNames(Ftp.FileDirectory.VALIDATION);
			for (String file : filesNames) {
				PrintedDoc pdoc = new PrintedDoc(Integer.parseInt(file.substring(0, file.indexOf('_'))), file.substring(file.indexOf('_') + 1));
				pdoc.setWaiting(true);
				Dps.getDocs().add(pdoc);
			}
			ftp.disconnect();
		}
	}
}

class QueueProcessor extends TimerTask {
	
	@Override
	public void run () {
		Dps.Working().lock();
		
		Ftp ftp = new Ftp();
		if (ftp.connect()) {

			String[] filesNames = ftp.getFilesNames(Ftp.FileDirectory.QUEUE);
			for (String file : filesNames) {
				if (ftp.getFile(file, false, 0)) {
					try {
						String fileToPrint = file;
						if (!file.toLowerCase().endsWith(".pdf")) {
							// TODO: conversion from doc/docx to pdf
							File f = new File(file);
							f.delete();
						}
						
						Pdf pdf = new Pdf(fileToPrint);
						PrintedDoc pdoc = new PrintedDoc(pdf.getNumPages(), file);
						if (pdoc.getNumPages() + Dps.getDocs().totalPages(pdoc.getLogin()) >= 30) {
							pdoc.setWaiting(true);
							ftp.addToWaintingList(file, pdoc.getNumPages());
							Dps.getDocs().add(pdoc);
							Dps.getUi().update();
						}
						else if (pdf.print(pdoc.isColored())) {
							ftp.removeFileFromQueue(file, pdf.getNumPages());
							Dps.getDocs().add(pdoc);
							Dps.getUi().update();
						}
						File f = new File(fileToPrint);
						f.delete();
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		
			ftp.disconnect();
		}
		
		Dps.Working().unlock();
	}
}
