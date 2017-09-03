package com.zarbosoft.merman.editor.display.derived;

import com.zarbosoft.merman.editor.Context;
import com.zarbosoft.merman.editor.display.DisplayNode;
import com.zarbosoft.merman.editor.display.Group;

import java.util.ArrayList;
import java.util.List;

public class TableLayout {
	private final Group group;
	int columns;
	List<List<DisplayNode>> cells = new ArrayList<>();

	public TableLayout(final Group group) {
		this.group = group;
	}

	public void add(final List<DisplayNode> row) {
		columns = Math.max(columns, row.size());
		cells.add(row);
	}

	public void layout(final Context context) {
		int transverse = 0;
		int converseSum = 0;
		for (int x = 0; x < columns; ++x) {
			int converse = 0;
			for (final List<DisplayNode> row : cells) {
				if (x >= row.size())
					continue;
				final DisplayNode cell = row.get(x);
				cell.setTransverse(context, transverse);
				transverse += cell.transverseSpan(context);
				cell.setConverse(context, converseSum);
				converse = Math.max(converse, cell.converseSpan(context));
			}
			converseSum += converse;
		}
	}
}