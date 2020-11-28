package com.baselet.plugin.core.controller;

import java.util.ArrayList;
import java.util.List;

import com.baselet.command.Command;

public class Controller {
	private List<Command> commands = new ArrayList<Command>();
	private int _cursor;
	private int changeOrigin;

	public Controller() {
		_cursor = -1;
		changeOrigin = -1;
	}

	protected void executeCommand(Command newCommand) {
		// Remove future commands
		for (int i = commands.size() - 1; i > _cursor; i--) {
			commands.remove(i);
			// reset change origin
			changeOrigin = -1;
		}
		commands.add(newCommand);
		newCommand.execute();

		if (commands.size() >= 2) {
			Command c_n, c_nMinus1;
			c_n = commands.get(commands.size() - 1);
			c_nMinus1 = commands.get(commands.size() - 2);

			if (c_n.isMergeableTo(c_nMinus1)) {
				commands.remove(c_n);
				commands.remove(c_nMinus1);
				Command c = c_n.mergeTo(c_nMinus1);
				commands.add(c);
			}
		}
		_cursor = commands.size() - 1;
	}

	public void undo() {
		if (isUndoable()) {
			Command c = commands.get(_cursor);
			c.undo();
			_cursor--;
		}
	}

	public void redo() {
		if (isRedoable()) {
			Command c = commands.get(_cursor + 1);
			c.execute();
			_cursor++;
		}
	}

	public boolean isEmpty() {
		return commands.isEmpty();
	}

	public boolean isUndoable() {
		return _cursor >= 0;
	}

	public boolean isRedoable() {
		return _cursor < commands.size() - 1;
	}

	public long getCommandCount() {
		return commands.size();
	}

	public void clear() {
		commands = new ArrayList<Command>();
		_cursor = -1;
	}

	public void setChangeOrigin() {
		changeOrigin = _cursor;
	}

	public boolean isChanged() {
		return changeOrigin != _cursor;
	}
}
