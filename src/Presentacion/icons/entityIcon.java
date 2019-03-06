package Presentacion.icons;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import Presentacion.Theme.Theme;

public class entityIcon extends icon{
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
    	Graphics2D g2d = (Graphics2D) g;
    	RoundRectangle2D rect = new RoundRectangle2D.Double(x, y,getIconWidth()*.9, getIconHeight()*.8, 10, 10);
    	Theme theme = Theme.getInstancia();
    	g2d.setColor(theme.entity());
    	g2d.fill(rect);
    	g2d.setColor(theme.lines());
    	g2d.draw(rect);
    	g2d.draw(new Line2D.Double(getIconWidth()*.4,getIconHeight()*.4,getIconWidth()*.5,getIconHeight()*.4));
        g2d.draw(new Line2D.Double(getIconWidth()*.45,getIconHeight()*.3,getIconWidth()*.45,getIconHeight()*.5));
    }	
}
