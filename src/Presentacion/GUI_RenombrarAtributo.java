package Presentacion;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import Controlador.Controlador;
import Controlador.TC;
import LogicaNegocio.Transfers.TransferAtributo;
import Presentacion.Lenguajes.Lenguaje;
import Utilidades.ImagePath;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class GUI_RenombrarAtributo extends javax.swing.JDialog implements KeyListener, MouseListener{

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}


	private static final long serialVersionUID = 1L;
	// Variables declaration - do not modify
	// End of variables declaration
	private TransferAtributo atributo;
	private JTextPane explicacion;
	private JLabel labelIcono;
	private JTextField cajaNombre;
	private JButton botonRenombrar;
	private JButton botonCancelar;
	private Controlador controlador;


	public GUI_RenombrarAtributo() {
		initComponents();
	}

	private void initComponents() {

		setTitle(Lenguaje.getMensaje(Lenguaje.RENAME_ATTRIBUTE));
		setIconImage(new ImageIcon(getClass().getClassLoader().getResource(ImagePath.LOGODBDT)).getImage());
		setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		setResizable(false);
		setModal(true);
		getContentPane().setLayout(null);
		{
			botonCancelar = new JButton();
			getContentPane().add(botonCancelar);
			botonCancelar.setText(Lenguaje.getMensaje(Lenguaje.CANCEL));
			botonCancelar.setBounds(280, 90, 80, 25);
			botonCancelar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					botonCancelarActionPerformed(evt);
				}
			});
			botonCancelar.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode()==10){botonCancelarActionPerformed(null);}
					else if(e.getKeyCode()==27){botonCancelarActionPerformed(null);}
					else if(e.getKeyCode()==37){botonRenombrar.grabFocus();}
				}
				public void keyReleased(KeyEvent e) {}
				public void keyTyped(KeyEvent e) {}
			});
			botonCancelar.setMnemonic(Lenguaje.getMensaje(Lenguaje.CANCEL).charAt(0));
		}
		{
			botonRenombrar = new JButton();
			getContentPane().add(botonRenombrar);
			botonRenombrar.setText(Lenguaje.getMensaje(Lenguaje.RENAME));
			botonRenombrar.setBounds(195, 90, 80, 25);
			botonRenombrar.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					botonRenombrarActionPerformed(evt);
				}
			});
			botonRenombrar.addKeyListener(new KeyListener() {
				public void keyPressed(KeyEvent e) {
					if(e.getKeyCode()==10){botonRenombrarActionPerformed(null);}
					else if(e.getKeyCode()==27){botonCancelarActionPerformed(null);}
					else if(e.getKeyCode()==39){botonCancelar.grabFocus();}
				}
				public void keyReleased(KeyEvent e) {}
				public void keyTyped(KeyEvent e) {}
			});
			botonRenombrar.setMnemonic(Lenguaje.getMensaje(Lenguaje.RENAME).charAt(0));
		}
		{
			cajaNombre = new JTextField();
			getContentPane().add(cajaNombre);
			cajaNombre.setBounds(124, 40, 236, 20);
			cajaNombre.addKeyListener(general);
		}
		{
			labelIcono = new JLabel();
			getContentPane().add(labelIcono);
			labelIcono.setIcon(new ImageIcon(getClass().getClassLoader().getResource(ImagePath.TECLADO)));
			labelIcono.setBounds(12, 12, 100, 100);
		}
		{
			explicacion = new JTextPane();
			getContentPane().add(explicacion);
			explicacion.setText(Lenguaje.getMensaje(Lenguaje.WRITE_NAME_ATTRIBUTE_SELECTED));
			explicacion.setEditable(false);
			explicacion.setOpaque(false);
			explicacion.setBounds(124, 12, 236, 22);
			explicacion.setFocusable(false);
		}
		this.setSize(380, 171);
		this.addMouseListener(this);
		this.addKeyListener(this);
	}
	
	/*
	 * Activar y desactivar el dialogo
	 */
	public void setActiva(){
		cajaNombre.setText(this.getAtributo().getNombre());
		SwingUtilities.invokeLater(doFocus);
		this.centraEnPantalla();
		this.setVisible(true);
	}
	
	private Runnable doFocus = new Runnable() {
	     public void run() {
	         cajaNombre.grabFocus();
	     }
	 };
	
	public void setInactiva(){
		this.setVisible(false);
	}

	/*
	 * Oyentes de los botones
	 */
	private void botonRenombrarActionPerformed(java.awt.event.ActionEvent evt) {                                               
		// Datos que mandamos al controlador
		Vector<Object> v = new Vector<Object>();
		v.add(this.getAtributo());
		v.add(this.cajaNombre.getText());
		// Mandamos mensaje + datos al controlador
		this.getControlador().mensajeDesde_GUI(TC.GUIRenombrarAtributo_Click_BotonRenombrar, v);
	}                                              

	private void botonCancelarActionPerformed(java.awt.event.ActionEvent evt) {                                              
		this.setVisible(false);
	}
	public void keyPressed( KeyEvent e ) {
		switch (e.getKeyCode()){
			case 27: {
				this.setInactiva();
				break;
			}
			case 10:{
				this.botonRenombrarActionPerformed(null);
				break;
			}
		}
	} 
	public void keyReleased(KeyEvent arg0) {}

	public void keyTyped(KeyEvent arg0) {}

	public void mouseEntered( MouseEvent e ) {} 
	
	public void mouseClicked(MouseEvent arg0) {
		this.requestFocus();
	}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent arg0) {}

	public void mouseReleased(MouseEvent arg0) {}
	
	//Oyente para todos los elementos
	private KeyListener general = new KeyListener() {
		public void keyPressed(KeyEvent e) {
			if(e.getKeyCode()==10){botonRenombrarActionPerformed(null);}
			if(e.getKeyCode()==27){botonCancelarActionPerformed(null);}
		}
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
	};
	
	/*
	 * Utilidades
	 */
	private void centraEnPantalla(){
		// Tamano de la pantalla
		java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		// Alto
		String altoString = String.valueOf(this.getSize().getWidth());
		altoString = altoString.substring(0,altoString.indexOf("."));
		int altoInt = Integer.parseInt(altoString);
		// Ancho
		String anchoString = String.valueOf(this.getSize().getHeight());
		anchoString = anchoString.substring(0,anchoString.indexOf("."));
		int anchoInt = Integer.parseInt(anchoString);

		setBounds((screenSize.width-altoInt)/2, (screenSize.height-anchoInt)/2, altoInt, anchoInt);
	}
	
	/*
	 * Getters y Setters
	 */
	
	public Controlador getControlador() {
		return controlador;
	}

	public void setControlador(Controlador controlador) {
		this.controlador = controlador;
	}

	public TransferAtributo getAtributo() {
		return atributo;
	}

	public void setAtributo(TransferAtributo atributo) {
		this.atributo = atributo;
	}
}
