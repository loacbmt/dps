package efrei.refresh.dps;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class Ui extends JFrame {
	
	private static final long serialVersionUID = 1L;
	private TrayIcon trayIcon;
	
	public Ui(Timer t) {
		super();
		try {
			trayIcon = new TrayIcon(ImageIO.read(new File("trayicon.png")), "Distant Printing Service");
			trayIcon.addMouseListener(new TrayEventProcessor(this));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		setTitle("Distant Printing Service - By Refresh'");
		setSize(640, 480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		try {
			setIconImage(ImageIO.read(new File("icon.png")));
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		addWindowListener(new EventProcessor(this, t));
		
		update();
		setVisible(true);
	}
	
	public void update() {
		DecimalFormat decimalFormat = (DecimalFormat)DecimalFormat.getInstance();
        decimalFormat.applyPattern("#0.00");
        
        JPanel table = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		
		c.gridy = 0;
		c.gridx = 0;
		table.add(new JLabel("Login"), c);
		c.gridx = 1;
		table.add(new JLabel("Impression"), c);
		c.gridx = 2;
		table.add(new JLabel("Pages"), c);
		c.gridx = 3;
		table.add(new JLabel("Reliure"), c);
		c.gridx = 4;
		table.add(new JLabel("Prix (€)"), c);
		c.gridx = 5;
		c.gridwidth = 2;
		table.add(new JLabel("Actions"), c);
		
		c.gridwidth = 1;
		for (PrintedDoc doc : Dps.getDocs()) {
			++c.gridy;
			c.gridx = 0;
			table.add(new JLabel(doc.getLogin()), c);
			c.gridx = 1;
			table.add(new JLabel(doc.isColored() ? "Couleurs" : "N&B"), c);
			c.gridx = 2;
			table.add(new JLabel(Integer.toString(doc.getNumPages())), c);
			c.gridx = 3;
			table.add(new JLabel(doc.isBinded() ? "Avec" : "Sans"), c);
			c.gridx = 4;
			table.add(new JLabel(decimalFormat.format(doc.computePrice())), c);
			c.gridx = 5;
			JButton download = new JButton("Télécharger");
			download.addMouseListener(new DownloadProcessor(c.gridy - 1));
			table.add(download, c);
			c.gridx = 6;
			JButton validate = new JButton("  Valider  ");
			validate.addMouseListener(new ValidateProcessor(c.gridy - 1));
			table.add(validate, c);
		}
		
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

class EventProcessor extends WindowAdapter {
	
	private Ui ui;
	private Timer periodicCheck;
	
	public EventProcessor (Ui u, Timer t) {
		super();
		ui = u;
		periodicCheck = t;
	}
	
	@Override
	public void windowClosing(WindowEvent ev) {
		if (Dps.isWorking()) {
			JOptionPane.showMessageDialog(null, "Veuillez attendre la fin du traitement de la file d'attente.");
		}
		else {
			periodicCheck.cancel();
			System.exit(0);
		}
    }
	
	@Override
	public void windowIconified(WindowEvent ev) {
		ui.toTray();
    }
}

class TrayEventProcessor extends MouseAdapter {
	
	private Ui ui;
	
	public TrayEventProcessor(Ui u) {
		super();
		ui = u;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		ui.restore();
	    ui.setExtendedState(ui.getExtendedState() & ~Frame.ICONIFIED);
	}
}

class ValidateProcessor extends MouseAdapter {
	
	private int docNum;
	
	public ValidateProcessor(int index) {
		super();
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		PrintedDoc doc = Dps.getDocs().get(docNum);
		if (JOptionPane.showConfirmDialog(null, "Valider définitivement l'impression de " + doc.getLogin() + " ?", "Encaissement pour une impression", JOptionPane.YES_NO_OPTION) == 0) {
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

class DownloadProcessor extends MouseAdapter {
	
	private int docNum;
	
	public DownloadProcessor(int index) {
		super();
		docNum = index;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		Ftp ftp = new Ftp();
		if (ftp.connect()) {
			PrintedDoc doc = Dps.getDocs().get(docNum);
			if (ftp.getFile(doc.getFileName(), true, doc.getNumPages())) {
				JOptionPane.showMessageDialog(null, "Téléchargement de " + doc.getFileName() + " terminé.");
			}
			ftp.disconnect();
		}
	}
}
