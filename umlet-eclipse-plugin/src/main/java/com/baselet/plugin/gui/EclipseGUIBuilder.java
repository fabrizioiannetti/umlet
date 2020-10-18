package com.baselet.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Panel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.baselet.gui.BaseGUIBuilder;
import com.baselet.gui.listener.GUIListener;
import com.baselet.gui.pane.OwnSyntaxPane;

public class EclipseGUIBuilder extends BaseGUIBuilder {

	// private final FocusListener eclipseCustomCodePaneListener = new FocusListener() {
	//
	// @Override
	// public void focusGained(FocusEvent e) {
	// ((EclipseGUI) CurrentGui.getInstance().getGui()).setPaneFocused(Pane.CUSTOMCODE);
	// }
	//
	// @Override
	// public void focusLost(FocusEvent e) {
	// ((EclipseGUI) CurrentGui.getInstance().getGui()).setPaneFocused(Pane.DIAGRAM);
	// }
	//
	// };

	// private final FocusListener eclipseTextPaneListener = new FocusListener() {
	//
	// @Override
	// public void focusGained(FocusEvent e) {
	// ((EclipseGUI) CurrentGui.getInstance().getGui()).setPaneFocused(Pane.PROPERTY);
	// }
	//
	// @Override
	// public void focusLost(FocusEvent e) {
	// ((EclipseGUI) CurrentGui.getInstance().getGui()).setPaneFocused(Pane.DIAGRAM);
	// }
	//
	// };

	private final JPanel contentPlaceHolder = new JPanel(new BorderLayout());

	public Panel initEclipseGui() {
		Panel embedded_panel = new Panel();
		embedded_panel.setLayout(new BorderLayout());
		embedded_panel.add(contentPlaceHolder);
		embedded_panel.addKeyListener(new GUIListener());

		// getCustomHandler().getPanel().getTextPane().addFocusListener(eclipseCustomCodePaneListener);
		// getPropertyTextPane().getTextComponent().addFocusListener(eclipseTextPaneListener);

		return embedded_panel;
	}

	public void setContent(JScrollPane scrollPane) {
		contentPlaceHolder.removeAll();
		contentPlaceHolder.add(scrollPane);
	}

	@Override
	public OwnSyntaxPane getPropertyTextPane() {
		return null;
	}

	@Override
	public void setCustomPanelEnabled(boolean enable) {
		// TODO@fab do not support yet
	}
}
