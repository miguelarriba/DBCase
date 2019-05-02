package vista.components.GUIPanels;

import javax.swing.JTable;
import vista.components.CustomCellEditor;
import vista.components.MyTableModel;

@SuppressWarnings("serial")
public class TablaVolumenes extends JTable{
	
	public TablaVolumenes(){
		super(new MyTableModel());
		setRowHeight(30);
		getTableHeader().setReorderingAllowed(false);//columnas fijadas
		setDefaultEditor(Object.class, CustomCellEditor.make());
		putClientProperty("terminateEditOnFocusLost", false);
	}
	
	public void refresh(String[][] s){
		((MyTableModel) this.getModel()).refresh(s);
		((MyTableModel) this.getModel()).fireTableDataChanged();
	}
}