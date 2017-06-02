package com.zarbosoft.bonestruct.editor.visual;

import com.zarbosoft.bonestruct.editor.Context;

public interface AlignmentListener {
	void align(Context context);

	/**
	 * @param context
	 * @return the converse position of the brick if it were unaligned.
	 */
	int getMinConverse(Context context);
}
