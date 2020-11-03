package com.baselet.plugin.gui;

import org.eclipse.jface.text.ITextOperationTarget;

public interface IOperationTarget {
	static final int UNDO = ITextOperationTarget.UNDO;
	static final int REDO = ITextOperationTarget.UNDO;
	static final int CUT = ITextOperationTarget.CUT;
	static final int COPY = ITextOperationTarget.COPY;
	static final int PASTE = ITextOperationTarget.PASTE;
	static final int DELETE = ITextOperationTarget.DELETE;
	static final int SELECT_ALL = ITextOperationTarget.SELECT_ALL;

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
	 */
	void doOperation(int operation);
}
