package efrei.refresh.dps;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Dps {

	private static final float bwPrice = 0.05f;
	private static final float colorPrice = 0.15f;
	private static final float bindingPrice = 0.60f;
	private static final long msBetweenChecks = 5*60*1000;
	
	private static boolean working = false;
	private static SortedDocsList docs = new SortedDocsList();
	private static Ui ui;

	public static void main(String[] args) {
		Timer t = new Timer();
		t.schedule(new QueueProcessor(), (msBetweenChecks) - (System.currentTimeMillis() % (msBetweenChecks)), msBetweenChecks);
		ui = new Ui(t);
		reload();
		ui.update();
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
	
	public static boolean isWorking() {
		return working;
	}
	
	public static void setWorking(boolean working) {
		Dps.working = working;
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
			String[] filesNames = ftp.getFilesNames(true);
			for (String file : filesNames) {
				Dps.getDocs().add(new PrintedDoc(Integer.parseInt(file.substring(0, file.indexOf('_'))), file.substring(file.indexOf('_') + 1)));
			}
			ftp.disconnect();
		}
	}
}

class QueueProcessor extends TimerTask {
	
	@Override
	public void run () {
		Dps.setWorking(true);
		
		Ftp ftp = new Ftp();
		if (ftp.connect()) {

			String[] filesNames = ftp.getFilesNames(false);
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
						if (pdf.print(PrintedDoc.isColored(file))) {
							ftp.removeFileFromQueue(file, pdf.getNumPages());
							File f = new File(fileToPrint);
							f.delete();
							Dps.getDocs().add(new PrintedDoc(pdf.getNumPages(), file));
							Dps.getUi().update();
						}
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
				}
			}
		
			ftp.disconnect();
		}
		
		Dps.setWorking(false);
	}
}
