package com.zarbosoft.bonestruct.visual.nodes.parts;

import com.zarbosoft.bonestruct.visual.Attachment;
import com.zarbosoft.bonestruct.visual.Brick;
import com.zarbosoft.bonestruct.visual.Context;
import com.zarbosoft.bonestruct.visual.Obbox;

public class BorderAttachment {
	Brick first;
	Brick last;
	private final Obbox border;
	private final Attachment firstAttachment = new Attachment() {
		@Override
		public void setTransverse(final Context context, final int transverse) {
			startTransverse = transverse;
			redraw(context);
		}

		@Override
		public void setConverse(final Context context, final int converse) {
			startConverse = converse;
			redraw(context);
		}

		@Override
		public void setTransverseSpan(final Context context, final int ascent, final int descent) {
			startTransverseSpan = ascent + descent;
			redraw(context);
		}
	};
	private final Attachment lastAttachment = new Attachment() {
		@Override
		public void setTransverse(final Context context, final int transverse) {
			endTransverse = transverse;
			redraw(context);
		}

		@Override
		public void setConverse(final Context context, final int converse) {
			redraw(context);
		}

		@Override
		public void setTransverseSpan(final Context context, final int ascent, final int descent) {
			endTransverseSpan = ascent + descent;
			redraw(context);
		}
	};
	private int startConverse;
	private int startTransverse;
	private int startTransverseSpan;
	private int endTransverse;
	private int endTransverseSpan;

	public BorderAttachment(final Context context, final Obbox.Settings style, final Brick first, final Brick last) {
		final Obbox.BakedSettings baked = new Obbox.BakedSettings();
		baked.merge(style);
		border = Obbox.fromSettings(baked);
		context.background.getChildren().add(border);
		setFirst(context, first);
		setLast(context, last);
	}

	public void setFirst(final Context context, final Brick first) {
		if (this.first != null)
			this.first.removeAttachment(context, this.firstAttachment);
		this.first = first;
		this.first.addAttachment(context, this.firstAttachment);
	}

	public void setLast(final Context context, final Brick last) {
		if (this.last != null)
			this.last.removeAttachment(context, this.lastAttachment);
		this.last = last;
		this.last.addAttachment(context, this.lastAttachment);
	}

	public void destroy(final Context context) {
		this.first.removeAttachment(context, this.firstAttachment);
		this.last.removeAttachment(context, this.lastAttachment);
		context.background.getChildren().remove(border);
	}

	public void redraw(final Context context) {
		if (first == null)
			return;
		if (last == null)
			return;
		border.setSize(
				context,
				startConverse,
				startTransverse,
				startTransverse + startTransverseSpan,
				last.converseEdge(context),
				endTransverse,
				endTransverse + endTransverseSpan
		);
	}
}