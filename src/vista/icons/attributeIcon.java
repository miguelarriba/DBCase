package vista.icons;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

import vista.tema.Theme;

public class attributeIcon extends icon{
  
	public attributeIcon() {
		super();
	}
    public attributeIcon(String tipo) {
		super(tipo);
	}

	@Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    	Graphics2D g2d = (Graphics2D) g;
    	Theme theme = Theme.getInstancia();
    	g2d.setColor(theme.atribute());
    	g2d.fill(new Ellipse2D.Double(x+getIconWidth()*.15, y,getIconWidth()*.7, getIconHeight()*.85));
    	g2d.setColor(theme.labelFontColorDark());
    	g2d.draw(new Ellipse2D.Double(x+getIconWidth()*.15, y,getIconWidth()*.7, getIconHeight()*.85));
    	if(pintarMas()) {
	    	g2d.draw(new Line2D.Double(getIconWidth()*.52,getIconHeight()*.4,getIconWidth()*.62,getIconHeight()*.4));//hor
	        g2d.draw(new Line2D.Double(getIconWidth()*.58,getIconHeight()*.3,getIconWidth()*.58,getIconHeight()*.5));//ver
    	}
    }

}
