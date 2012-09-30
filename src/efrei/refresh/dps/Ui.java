package efrei.refresh.dps;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;

public class Ui extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private TrayIcon trayIcon;
	
	public static void askPassword() {
		JFrame window = new JFrame();
		Lock waitValidation = new ReentrantLock();
		Condition passwordValidated = waitValidation.newCondition();
		JPasswordField password = new JPasswordField();
		JButton validate = new JButton("Ok");
		validate.addMouseListener(new passwordListener(window, password, waitValidation, passwordValidated));
		
		window.setTitle("Distant Printing Service - By Refresh'");
		window.setSize(350, 120);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(EXIT_ON_CLOSE);
		window.getContentPane().setLayout(new GridLayout(3, 1));
		window.getContentPane().add(new JLabel("Mot de passe FTP :"));
		window.getContentPane().add(password);
		window.getContentPane().add(validate);
		window.setVisible(true);
		
		waitValidation.lock();
		try {
			passwordValidated.await();
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		}
		waitValidation.unlock();
	}
	
	public Ui(Timer t) {
		try {
			trayIcon = new TrayIcon(ImageIO.read(new File("res/trayicon.png")), "Distant Printing Service");
			trayIcon.addMouseListener(new TrayEventListener(this));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		setTitle("Distant Printing Service - By Refresh'");
		setSize(640, 480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		try {
			setIconImage(ImageIO.read(new File("res/icon.png")));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		addWindowListener(new EventListener(this, t));
		
		update();
		setVisible(true);
	}
	
	public void update() {
		// Known issue: the last column is not displayed the way it is expected
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4126689
		DecimalFormat decimalFormat = (DecimalFormat)DecimalFormat.getInstance();
        decimalFormat.applyPattern("#0.00");
        
        JPanel table = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		
		c.gridy = 0;
		c.gridx = 0;
		c.gridwidth = 6;
		table.add(new JLabel("Login"), c);
		c.gridx += c.gridwidth;
		table.add(new JLabel("Impression"), c);
		c.gridx += c.gridwidth;
		table.add(new JLabel("Pages"), c);
		c.gridx += c.gridwidth;
		table.add(new JLabel("Reliure"), c);
		c.gridx += c.gridwidth;
		table.add(new JLabel("Prix (€)"), c);
		c.gridx += c.gridwidth;
		c.gridwidth = GridBagConstraints.REMAINDER;
		table.add(new JLabel("Actions"), c);
		
		for (PrintedDoc doc : Dps.getDocs()) {
			++c.gridy;
			c.gridx = 0;
			c.gridwidth = 6;
			table.add(new JLabel(doc.getLogin()), c);
			c.gridx += c.gridwidth;
			table.add(new JLabel(doc.isColored() ? "Couleurs" : "N&B"), c);
			c.gridx += c.gridwidth;
			table.add(new JLabel(Integer.toString(doc.getNumPages())), c);
			c.gridx += c.gridwidth;
			table.add(new JLabel(doc.isBinded() ? "Avec" : "Sans"), c);
			c.gridx += c.gridwidth;
			table.add(new JLabel(decimalFormat.format(doc.computePrice())), c);
			if (doc.isWaiting()) {
				c.gridx += c.gridwidth;
				c.gridwidth = 2;
				JButton show = new JButton("Vérifier ");
				show.addMouseListener(new ShowListener(c.gridy - 1));
				table.add(show, c);
				c.gridx += c.gridwidth;
				JButton cancel = new JButton("Supprimer");
				cancel.addMouseListener(new CancelListener(c.gridy - 1));
				table.add(cancel, c);
				c.gridx += c.gridwidth;
				JButton validate = new JButton("Imprimer ");
				validate.addMouseListener(new ValidateListener(c.gridy - 1));
				table.add(validate, c);
			}
			else {
				c.gridx += c.gridwidth;
				c.gridwidth = 3;
				JButton download = new JButton("Télécharger");
				download.addMouseListener(new DownloadListener(c.gridy - 1));
				table.add(download, c);
				c.gridx += c.gridwidth;
				JButton cash = new JButton(" Encaisser ");
				cash.addMouseListener(new CashListener(c.gridy - 1));
				table.add(cash, c);
			}
		}
		
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		++c.gridy;
		c.gridwidth = 36;
		table.add(new JLabel(""), c);
		
		JScrollPane scrollPane = new JScrollPane(table);
		getContentPane().removeAll();
		getContentPane().add(scrollPane);
		getContentPane().validate();
	}
	
	public void toTray() {
		try {
			SystemTray.getSystemTray().add(trayIcon);
			setVisible(false);
		} catch (AWTException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void restore() {
		SystemTray.getSystemTray().remove(trayIcon);
		setVisible(true);
	}
}

class passwordListener extends MouseAdapter {
	
	private JFrame window;
	private JPasswordField password;
	private Lock waitValidation;
	private Condition passwordValidated;
	
	passwordListener(JFrame f, JPasswordField p, Lock l, Condition c) {
		window = f;
		password = p;
		waitValidation = l;
		passwordValidated = c;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		Ftp.setPassword(new String (password.getPassword()));
		Ftp ftp = new Ftp();
		if (ftp.connect()) {
			window.dispose();
			waitValidation.lock();
			passwordValidated.signal();
			waitValidation.unlock();
			ftp.disconnect();
		}
		else JOptionPane.showMessageDialog(null, "Mot de passe incorrect.");
	}
}

class EventListener extends WindowAdapter {
	
	private Ui ui;
	private Timer periodicCheck;
	
	public EventListener (Ui u, Timer t) {
		ui = u;
		periodicCheck = t;
	}
	
	@Override
	public void windowClosing(WindowEvent ev) {
		if (Dps.Working().tryLock()) {
			periodicCheck.cancel();
			System.exit(0);
		}
		else {
			JOptionPane.showMessageDialog(null, "Veuillez attendre la fin du traitement de la file d'attente.");
		}
    }
	
	@Override
	public void windowIconified(WindowEvent ev) {
		ui.toTray();
    }
}

class TrayEventListener extends MouseAdapter {
	
	private Ui ui;
	
	public TrayEventListener(Ui u) {
		ui = u;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		ui.restore();
	    ui.setExtendedState(ui.getExtendedState() & ~Frame.ICONIFIED);
	}
}

class ShowListener extends MouseAdapter {
	
	private int docNum;
	
	public ShowListener(int index) {
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		PrintedDoc doc = Dps.getDocs().get(docNum);
		try {
			Desktop.getDesktop().open(new File(doc.getFileName() + (doc.getFileName().toLowerCase().endsWith(".pdf") ? "" : ".pdf")));
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}
}

class CancelListener extends MouseAdapter {
	
	private int docNum;
	
	public CancelListener(int index) {
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		PrintedDoc doc = Dps.getDocs().get(docNum);
		if (JOptionPane.showConfirmDialog(null, "Annuler l'impression de " + doc.getLogin() + " ?", "Annuler une impression", JOptionPane.YES_NO_OPTION) == 0) {
			Ftp ftp = new Ftp();
			if (ftp.connect()) {
				ftp.moveFile(doc.getFileName(), doc.getNumPages(), Ftp.FileDirectory.VALIDATION, Ftp.FileDirectory.BACKUP);
				ftp.deleteBackUpFile(doc.getFileName(), doc.getNumPages());
				Dps.getDocs().remove(docNum);
				Dps.getUi().update();
				File f = new File(doc.getFileName() + (doc.getFileName().toLowerCase().endsWith(".pdf") ? "" : ".pdf"));
				f.delete();
				ftp.disconnect();
			}
		}
	}
}

class ValidateListener extends MouseAdapter {
	
	private int docNum;
	
	public ValidateListener(int index) {
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		PrintedDoc doc = Dps.getDocs().get(docNum);
		if (JOptionPane.showConfirmDialog(null, "Autoriser l'impression de " + doc.getLogin() + " ?", "Autoriser une impression", JOptionPane.YES_NO_OPTION) == 0) {
			Ftp ftp = new Ftp();
			if (ftp.connect()) {
				String fileToPrint = doc.getFileName() + (doc.getFileName().toLowerCase().endsWith(".pdf") ? "" : ".pdf");
				try {
					Pdf pdf = new Pdf(fileToPrint);
					if (pdf.print(doc.isColored())) {
						ftp.moveFile(doc.getFileName(), doc.getNumPages(), Ftp.FileDirectory.VALIDATION, Ftp.FileDirectory.BACKUP);
						doc.setWaiting(false);
						Dps.getUi().update();
						File f = new File(fileToPrint);
						f.delete();
					}
				} catch (IOException ioe) {
					System.out.println(ioe.getMessage());
				}
				ftp.disconnect();
			}
		}
	}
}

class DownloadListener extends MouseAdapter {
	
	private int docNum;
	
	public DownloadListener(int index) {
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		Ftp ftp = new Ftp();
		if (ftp.connect()) {
			PrintedDoc doc = Dps.getDocs().get(docNum);
			if (ftp.getFile(doc.getFileName(), doc.isWaiting() ? Ftp.FileDirectory.VALIDATION : Ftp.FileDirectory.BACKUP, doc.getNumPages())) {
				JOptionPane.showMessageDialog(null, "Téléchargement de " + doc.getFileName() + " terminé.");
			}
			ftp.disconnect();
		}
	}
}

class CashListener extends MouseAdapter {
	
	private int docNum;
	
	public CashListener(int index) {
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		PrintedDoc doc = Dps.getDocs().get(docNum);
		if (JOptionPane.showConfirmDialog(null, "Encaisser l'impression de " + doc.getLogin() + " ?", "Encaissement pour une impression", JOptionPane.YES_NO_OPTION) == 0) {
			Ftp ftp = new Ftp();
			if (ftp.connect()) {
				ftp.deleteBackUpFile(doc.getFileName(), doc.getNumPages());
				ftp.disconnect();
				Dps.getDocs().remove(docNum);
				Dps.getUi().update();
			}
		}
	}
}
