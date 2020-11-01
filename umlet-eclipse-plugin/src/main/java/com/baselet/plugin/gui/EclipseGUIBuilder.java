package com.baselet.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Panel;

import javax.swing.JPanel;

import com.baselet.gui.BaseGUIBuilder;
import com.baselet.gui.listener.GUIListener;
import com.baselet.gui.pane.OwnSyntaxPane;

public class EclipseGUIBuilder extends BaseGUIBuilder {

	public Panel initEclipseGui() {
		Panel embedded_panel = new Panel();
		embedded_panel.setLayout(new BorderLayout());
		embedded_panel.add(new JPanel(new BorderLayout()));
		embedded_panel.addKeyListener(new GUIListener());
		return embedded_panel;
	}

	@Override
	public JPanel newPalettePanel() {
		return null;
	}

	@Override
	public JPanel getPalettePanel() {
		return null;
	}

	@Override
	public void setPaletteActive(String paletteName) {
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
