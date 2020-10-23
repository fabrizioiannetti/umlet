package com.baselet.plugin.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Panel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.baselet.control.Main;
import com.baselet.diagram.PaletteHandler;
import com.baselet.gui.BaseGUIBuilder;
import com.baselet.gui.listener.DividerListener;
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

	// need to replicate it here as it is private in the base class
	private JPanel palettePanel;

	private CardLayout palettePanelLayout;

	public Panel initEclipseGui() {
		Panel embedded_panel = new Panel();
		embedded_panel.setLayout(new BorderLayout());
		embedded_panel.add(contentPlaceHolder);
		embedded_panel.addKeyListener(new GUIListener());

		// create palette panel
		palettePanel = newPalettePanel();

		// getCustomHandler().getPanel().getTextPane().addFocusListener(eclipseCustomCodePaneListener);
		// getPropertyTextPane().getTextComponent().addFocusListener(eclipseTextPaneListener);

		return embedded_panel;
	}

	@Override
	public JPanel newPalettePanel() {
		palettePanelLayout = new CardLayout();
		JPanel palettePanel = new JPanel(palettePanelLayout);
		palettePanel.addComponentListener(new DividerListener()); // Adding the DividerListener which refreshes Scrollbars here is enough for all dividers
		for (PaletteHandler palette : Main.getInstance().getPalettes().values()) {
			palettePanel.add(palette.getDrawPanel().getScrollPane(), palette.getName());
		}
		return palettePanel;
	}

	@Override
	public JPanel getPalettePanel() {
		return palettePanel;
	}

	@Override
	public void setPaletteActive(String paletteName) {
		palettePanelLayout.show(palettePanel, paletteName);
	}

	// @Override
	// public JPanel newPalettePanel() {
	// palettePanelLayout = new CardLayout();
	// JPanel palettePanel = new JPanel(palettePanelLayout);
	// palettePanel.addComponentListener(new DividerListener()); // Adding the DividerListener which refreshes Scrollbars here is enough for all dividers
	// for (SWTPaletteHandler palette : SWTMain.getInstance().getPalettes().values()) {
	// palettePanel.add(palette.getDrawPanel().getScrollPane(), palette.getName());
	// }
	// return palettePanel;
	// }

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
