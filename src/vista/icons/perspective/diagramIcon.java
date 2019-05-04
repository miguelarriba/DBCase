package vista.icons.perspective;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import vista.icons.icon;
import vista.tema.Theme;

public class diagramIcon extends icon{
	public diagramIcon(boolean selected) {
		super("perspective", selected);
	}

	@Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    	Graphics2D g2d = (Graphics2D) g;
    	RoundRectangle2D rect = new RoundRectangle2D.Double(
    			x+getIconWidth()*.05, 
    			y+getIconHeight()*.05,
    			getIconWidth()*.9, 
    			getIconHeight()-getIconHeight()*.2, 15, 15);
    	Theme theme = Theme.getInstancia();
    	g2d.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)); 
    	g2d.setColor(theme.toolBar());
    	g2d.fill(rect);
    	g2d.setStroke(new BasicStroke(3));
    	g2d.setColor(isSelected()?theme.attribute():theme.lines());
    	g2d.draw(rect);
    	g2d.draw(new Line2D.Double(getIconWidth()*.7,getIconHeight()*.05,getIconWidth()*.7,getIconHeight()*.85));
    }	
}