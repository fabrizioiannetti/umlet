package com.baselet.plugin.gui;

import java.util.List;

import org.eclipse.jface.text.ITextOperationTarget;

import com.baselet.element.interfaces.GridElement;

public interface IOperationTarget {
	static final int UNDO = ITextOperationTarget.UNDO;
	static final int REDO = ITextOperationTarget.REDO;
	static final int CUT = ITextOperationTarget.CUT;
	static final int COPY = ITextOperationTarget.COPY;
	static final int PASTE = ITextOperationTarget.PASTE;
	static final int DELETE = ITextOperationTarget.DELETE;
	static final int SELECT_ALL = ITextOperationTarget.SELECT_ALL;
	static final int DUPLICATE = ITextOperationTarget.STRIP_PREFIX + 1;
	static final int INSERT = ITextOperationTarget.STRIP_PREFIX + 2;

	/**
	 * Returns whether the operation specified by the given operation code
	 * can be performed.
	 *
	 * @param operation the operation code
	 * @return <code>true</code> if the specified operation can be performed
	 */
	boolean canDoOperation(int operation);

	/**
	 * Performs the operation specified by the operation code on the target.
	 * <code>doOperation</code> must only be called if <code>canDoOperation</code>
	 * returns <code>true</code>.
	 *
	 * @param operation the operation code
	 * @param elements optional elements for the operation (INSERT only)
	 */
	void doOperation(int operation, List<GridElement> elements);

}
