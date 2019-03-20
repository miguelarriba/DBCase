package vista.GUIPanels;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import controlador.Controlador;
import controlador.TC;
import modelo.persistencia.DAOEntidades;
import modelo.persistencia.DAORelaciones;
import modelo.transfers.Transfer;
import vista.Theme.Theme;
import vista.icons.attributeIcon;
import vista.icons.entityIcon;
import vista.icons.isaIcon;
import vista.icons.relationIcon;
import vista.lenguaje.Lenguaje;

@SuppressWarnings("serial")
public class addTransfersPanel extends JPanel{
	private Controlador controlador;
	private Vector<Transfer> listaTransfers;
	private int[] coords;
	public addTransfersPanel(Controlador c, Vector<Transfer> lisTra){
		super();
		coords = new int[2];
		coords[0]=70;
		coords[1]=50;
		Theme theme = Theme.getInstancia();
		this.controlador = c;
		this.listaTransfers = lisTra;
		JLabel anadirEntidad = new JLabel(new entityIcon());
		anadirEntidad.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		anadirEntidad.setText(Lenguaje.getMensaje(Lenguaje.ENTITY));
		anadirEntidad.setVerticalTextPosition(JLabel.BOTTOM);
		anadirEntidad.setHorizontalTextPosition(JLabel.CENTER);
		JLabel anadirRelacion = new JLabel(new relationIcon());
		anadirRelacion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		anadirRelacion.setText(Lenguaje.getMensaje(Lenguaje.RELATION));
		anadirRelacion.setVerticalTextPosition(JLabel.BOTTOM);
		anadirRelacion.setHorizontalTextPosition(JLabel.CENTER);
		JLabel anadirIsa = new JLabel(new isaIcon());
		anadirIsa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		anadirIsa.setText(Lenguaje.getMensaje(Lenguaje.ISA_RELATION));
		anadirIsa.setVerticalTextPosition(JLabel.BOTTOM);
		anadirIsa.setHorizontalTextPosition(JLabel.CENTER);
		JLabel anadirAttribute = new JLabel(new attributeIcon());
		anadirAttribute.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		anadirAttribute.setText(Lenguaje.getMensaje(Lenguaje.ATTRIBUTE));
		anadirAttribute.setVerticalTextPosition(JLabel.BOTTOM);
		anadirAttribute.setHorizontalTextPosition(JLabel.CENTER);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(Box.createVerticalGlue());
		this.add(anadirEntidad);
		this.add(Box.createRigidArea(new Dimension(0,30)));
		this.add(anadirRelacion);
		this.add(Box.createRigidArea(new Dimension(0,30)));
		this.add(anadirIsa);
		this.add(Box.createRigidArea(new Dimension(0,30)));
		this.add(anadirAttribute);
		this.add(Box.createVerticalGlue());
		this.setBackground(theme.toolBar());
		this.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
		
		anadirEntidad.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	Point2D p = new Point2D.Double(coords[0],coords[1]);
				controlador.mensajeDesde_PanelDiseno(TC.PanelDiseno_Click_InsertarEntidad,p);
				aumentaCoords();
            }
        });	
		anadirRelacion.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	Point2D p = new Point2D.Double(coords[0],coords[1]);
				controlador.mensajeDesde_PanelDiseno(TC.PanelDiseno_Click_InsertarRelacionNormal,p);
				aumentaCoords();
            }

        });
		anadirIsa.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	Point2D p = new Point2D.Double(coords[0],coords[1]);
				controlador.mensajeDesde_PanelDiseno(TC.PanelDiseno_Click_InsertarRelacionIsA,p);
				aumentaCoords();
            }
        });
		anadirAttribute.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	listaTransfers = new Vector<Transfer>();
            	DAORelaciones daoRelaciones = new DAORelaciones(controlador.getPath());
            	listaTransfers.addAll(daoRelaciones.ListaDeRelaciones());
        		DAOEntidades daoEntidades = new DAOEntidades(controlador.getPath());
        		listaTransfers.addAll(daoEntidades.ListaDeEntidades());
				controlador.mensajeDesde_PanelDiseno(TC.PanelDiseno_Click_InsertarAtributo, listaTransfers);
            }
        });		
	}
	
	private void aumentaCoords() {
		coords[0]=coords[0]<700?coords[0]+150:70;
		coords[1]=coords[0]==70?coords[1]+70:coords[1];
	}
	
}
