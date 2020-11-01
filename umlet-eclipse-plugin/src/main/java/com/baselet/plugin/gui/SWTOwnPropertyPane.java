package com.baselet.plugin.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.widgets.Display;

import com.baselet.element.interfaces.GridElement;
import com.baselet.gui.AutocompletionText;
import com.baselet.gui.pane.OwnSyntaxPane;

public class SWTOwnPropertyPane extends OwnSyntaxPane {
	private List<AutocompletionText> words;
	private final TextViewer viewer;
	private final Display display;

	public SWTOwnPropertyPane(TextViewer viewer) {
		this.viewer = viewer;
		display = viewer.getControl().getDisplay();
	}

	@Override
	public String getText() {
		final AtomicReference<String> text = new AtomicReference<String>();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				text.set(viewer.getDocument().get());
			}
		});
		return text.get();
	}

	@Override
	public JPanel getPanel() {
		return null;
	}

	@Override
	public void invalidate() {
		viewer.refresh();
	}

	public void doCopy() {
		if (viewer.canDoOperation(ITextOperationTarget.COPY)) {
			viewer.doOperation(ITextOperationTarget.COPY);
		}
	}

	public void doCut() {
		if (viewer.canDoOperation(ITextOperationTarget.CUT)) {
			viewer.doOperation(ITextOperationTarget.CUT);
		}
	}

	public void doPaste() {
		if (viewer.canDoOperation(ITextOperationTarget.PASTE)) {
			viewer.doOperation(ITextOperationTarget.PASTE);
		}
	}

	public void doSelectAll() {
		if (viewer.canDoOperation(ITextOperationTarget.SELECT_ALL)) {
			viewer.doOperation(ITextOperationTarget.SELECT_ALL);
		}
	}

	@Override
	public JTextComponent getTextComponent() {
		return null;
	}

	@Override
	public void switchToElement(GridElement e) {
		words = e.getAutocompletionList();
		setText(e.getPanelAttributes());
	}

	@Override
	public void switchToNonElement(String text) {
		words = new ArrayList<AutocompletionText>();
		setText(text);

	}

	public void updateGridElement() {
		// TODO@fab
		// GridElement gridElement = Main.getInstance().getEditedGridElement();
		// String s = getText();
		// DiagramHandler handler = CurrentDiagram.getInstance().getDiagramHandler();
		//
		// if (gridElement != null) {
		// // only create command if changes were made
		// if (!s.equals(gridElement.getPanelAttributes())) {
		// int newCaretPos = 0;// CurrentGui.getInstance().getGui().getPropertyPane().getTextComponent().getCaretPosition();
		// int oldCaretPos = 0;// newCaretPos - (s.length() - gridElement.getPanelAttributes().length());
		//
		// if (HandlerElementMap.getHandlerForElement(gridElement) instanceof CustomPreviewHandler) {
		// HandlerElementMap.getHandlerForElement(gridElement).getController().executeCommand(new CustomCodePropertyChanged(gridElement.getPanelAttributes(), s, oldCaretPos, newCaretPos));
		// }
		// else {
		// HandlerElementMap.getHandlerForElement(gridElement).getController().executeCommand(new ChangePanelAttributes(gridElement, gridElement.getPanelAttributes(), s, oldCaretPos, newCaretPos));
		// }
		// }
		// }
		// else if (handler != null && !s.equals(handler.getHelpText())) { // help panel has been edited
		// handler.getController().executeCommand(new HelpPanelChanged(s));
		// }

		// TODO@fab scrollbars?
		// Scrollbars must be updated cause some entities can grow out of screen border by typing text inside (eg: autoresize custom elements)
		// if (handler != null) {
		// handler.getDrawPanel().updatePanelAndScrollbars();
		// }
	}

	private void setText(final String text) {
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.getDocument().set(text);
			}
		});
	}

}