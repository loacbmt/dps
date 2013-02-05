package efrei.refresh.dps;

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

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
	
	public Ui() {
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
	}
	
	public void run(Timer t)
	{
		update();
		addWindowListener(new EventListener(this, t));
		setVisible(true);
	}
	
	public void update() {
		DecimalFormat decimalFormat = (DecimalFormat)DecimalFormat.getInstance();
        decimalFormat.applyPattern("#0.00");
        
        JPanel table = new JPanel(new GridLayout(Dps.getDocs().size() + 1, 6));
		table.add(new JLabel("Login"));
		table.add(new JLabel("Impression"));
		table.add(new JLabel("Reliure"));
		table.add(new JLabel("Pages"));
		table.add(new JLabel("Actions"));
		table.add(new JLabel(""));
		
		for (PrintedDoc doc : Dps.getDocs()) {
			table.add(new JLabel(doc.getLogin()));
			table.add(new JLabel(doc.isColored() ? "Couleurs" : "N&B"));
			table.add(new JLabel(doc.isBinded() ? "Avec" : "Sans"));
			table.add(new JLabel(Integer.toString(doc.getNumPages())));
			
			JButton download = new JButton("Télécharger");
			download.addMouseListener(new DownloadListener(doc));
			table.add(download);
			JButton validate = new JButton("  Valider  ");
			validate.addMouseListener(new ValidateListener(doc));
			table.add(validate);
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

class passwordListener extends MouseAdapter {
	
	private static final String cipheredFTPPassword = new String("EotaVJ2Y9KaMoOJra3ZkJQ==");
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
		String ftpPassword = "";
		try {
			PBEKeySpec keySpec = new PBEKeySpec(password.getPassword().clone(), new byte[8], 1);
			PBEParameterSpec paramSpec = new PBEParameterSpec(keySpec.getSalt(), keySpec.getIterationCount());
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(keySpec);
			Cipher cipher = Cipher.getInstance(key.getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
			ftpPassword = new String(cipher.doFinal(Base64.decode(cipheredFTPPassword.getBytes())));
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException | Base64DecodingException e1) {
			System.out.println(e1.getMessage());
		}
		Ftp.setPassword(ftpPassword);
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

class DownloadListener extends MouseAdapter {
	
	private PrintedDoc pdoc;
	
	public DownloadListener(PrintedDoc doc) {
		pdoc = doc;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		Ftp ftp = new Ftp();
		if (ftp.connect()) {
			if (ftp.getFile(pdoc.getFileName(), true)) JOptionPane.showMessageDialog(null, "Téléchargement de " + pdoc.getFileName() + " terminé.");
			else JOptionPane.showMessageDialog(null, "Une erreur est survenue lors du téléchargement !");
			ftp.disconnect();
		}
	}
}

class ValidateListener extends MouseAdapter {
	
	private PrintedDoc pdoc;
	
	public ValidateListener(PrintedDoc doc) {
		pdoc = doc;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if (JOptionPane.showConfirmDialog(null, "Valider l'impression de " + pdoc.getLogin() + " ?", "Validation d'une impression", JOptionPane.YES_NO_OPTION) == 0) {
			Ftp ftp = new Ftp();
			if (ftp.connect()) {
				ftp.deleteBackUpFile(pdoc.getFileName());
				ftp.disconnect();
				Dps.getDocs().remove(pdoc);
				Dps.getUi().update();
			}
		}
	}
}
