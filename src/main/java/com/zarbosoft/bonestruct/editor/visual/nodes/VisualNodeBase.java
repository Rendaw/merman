package com.zarbosoft.bonestruct.editor.visual.nodes;

import com.zarbosoft.bonestruct.document.Node;
import com.zarbosoft.bonestruct.editor.Action;
import com.zarbosoft.bonestruct.editor.Context;
import com.zarbosoft.bonestruct.editor.Hoverable;
import com.zarbosoft.bonestruct.editor.Selection;
import com.zarbosoft.bonestruct.editor.visual.Alignment;
import com.zarbosoft.bonestruct.editor.visual.Vector;
import com.zarbosoft.bonestruct.editor.visual.attachments.BorderAttachment;
import com.zarbosoft.bonestruct.editor.visual.attachments.VisualAttachmentAdapter;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNode;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNodeParent;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNodePart;
import com.zarbosoft.bonestruct.syntax.NodeType;
import com.zarbosoft.bonestruct.wall.Brick;
import com.zarbosoft.rendaw.common.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class VisualNodeBase extends VisualNodePart {
	protected VisualNode body;
	VisualNodeParent parent;
	boolean selected = false;
	private VisualAttachmentAdapter adapter;
	private BorderAttachment border;
	Hoverable hoverable;
	private NestedSelection selection;

	public VisualNodeBase(
			final Set<Tag> tags
	) {
		super(tags);
	}

	protected abstract void nodeSet(Context context, Node value);

	@Override
	public void setParent(final VisualNodeParent parent) {
		this.parent = parent;
	}

	@Override
	public VisualNodeParent parent() {
		return parent;
	}

	@Override
	public Brick getFirstBrick(final Context context) {
		return body.getFirstBrick(context);
	}

	@Override
	public Brick getLastBrick(final Context context) {
		return body.getLastBrick(context);
	}

	@Override
	public void rootAlignments(final Context context, final Map<String, Alignment> alignments) {
		body.rootAlignments(context, alignments);
	}

	@Override
	public void destroy(final Context context) {
		body.destroy(context);
	}

	@Override
	public Iterable<Pair<Brick, Brick.Properties>> getPropertiesForTagsChange(
			final Context context, final TagsChange change
	) {
		return body.getPropertiesForTagsChange(context, change);
	}

	@Override
	public Brick createFirstBrick(final Context context) {
		final Brick out = body.createFirstBrick(context);
		if (adapter != null) {
			adapter.setFirst(context, out);
			adapter.notifySeedBrick(context, out);
		}
		return out;
	}

	@Override
	public Brick createLastBrick(final Context context) {
		final Brick out = body.createLastBrick(context);
		if (adapter != null) {
			adapter.setLast(context, out);
			adapter.notifySeedBrick(context, out);
		}
		return out;
	}

	private void createAdapter(final Context context) {
		adapter = new VisualAttachmentAdapter();
		adapter.setBase(context, body);
		adapter.addListener(context, new VisualAttachmentAdapter.BoundsListener() {
			@Override
			public void firstChanged(final Context context, final Brick brick) {
				border.setFirst(context, brick);
			}

			@Override
			public void lastChanged(final Context context, final Brick brick) {
				border.setLast(context, brick);
			}
		});
	}

	@Override
	public boolean select(final Context context) {
		if (selected)
			throw new AssertionError("Already selected");
		else if (border != null) {
			context.clearHover();
		}
		selected = true;
		border = new BorderAttachment(context, context.syntax.selectStyle);
		createAdapter(context);
		context.setSelection(new NestedSelection(context));
		return true;
	}

	protected Stream<Action> getActions(final Context context) {
		return Stream.of(new Action() {
			@Override
			public void run(final Context context) {
				body.select(context);
			}

			@Override
			public String getName() {
				return "enter";
			}
		}, new Action() {
			@Override
			public void run(final Context context) {
				if (parent != null) {
					parent.selectUp(context);
				}
			}

			@Override
			public String getName() {
				return "exit";
			}
		}, new Action() {
			@Override
			public void run(final Context context) {
				nodeSet(context, context.syntax.gap.create());
			}

			@Override
			public String getName() {
				return "delete";
			}
		}, new Action() {
			@Override
			public void run(final Context context) {

			}

			@Override
			public String getName() {
				return "copy";
			}
		}, new Action() {
			@Override
			public void run(final Context context) {

			}

			@Override
			public String getName() {
				return "cut";
			}
		}, new Action() {
			@Override
			public void run(final Context context) {

			}

			@Override
			public String getName() {
				return "paste";
			}
		});
	}

	private class NestedSelection extends Selection {
		public NestedSelection(final Context context) {
			context.actions.put(this, VisualNodeBase.this.getActions(context).collect(Collectors.toList()));
		}

		@Override
		public void clear(final Context context) {
			border.destroy(context);
			border = null;
			adapter.destroy(context);
			adapter = null;
			selected = false;
			context.actions.remove(this);
		}

		@Override
		public VisualNodePart getVisual() {
			return VisualNodeBase.this;
		}

		@Override
		public void addBrickListener(final Context context, final VisualAttachmentAdapter.BoundsListener listener) {
			adapter.addListener(context, listener);

		}

		@Override
		public void removeBrickListener(final Context context, final VisualAttachmentAdapter.BoundsListener listener) {
			adapter.removeListener(context, listener);
		}
	}

	protected void set(final Context context, final Node data) {
		if (body != null)
			body.destroy(context);
		this.body = data.createVisual(context);
		body.setParent(new NestedParent());
		if (parent != null) {
			final Brick previousBrick = parent.getPreviousBrick(context);
			final Brick nextBrick = parent.getNextBrick(context);
			if (previousBrick != null && nextBrick != null)
				context.fillFromEndBrick(previousBrick);
		}
		if (adapter != null) {
			adapter.setBase(context, body);
		}
	}

	private class NestedParent extends VisualNodeParent {
		@Override
		public void selectUp(final Context context) {
			select(context);
		}

		@Override
		public VisualNode getTarget() {
			return VisualNodeBase.this;
		}

		@Override
		public NodeType.NodeTypeVisual getNode() {
			throw new NotImplementedException();
		}

		@Override
		public Alignment getAlignment(final String alignment) {
			return parent.getAlignment(alignment);
		}

		@Override
		public Brick getPreviousBrick(final Context context) {
			if (parent == null)
				return null;
			return parent.getPreviousBrick(context);
		}

		@Override
		public Brick getNextBrick(final Context context) {
			if (parent == null)
				return null;
			return parent.getNextBrick(context);
		}

		@Override
		public Brick createNextBrick(final Context context) {
			final Brick out;
			if (parent == null)
				out = null;
			else
				out = parent.createNextBrick(context);
			if (adapter != null)
				adapter.notifyNextBrickPastEdge(context, out);
			return out;
		}

		@Override
		public Brick createPreviousBrick(final Context context) {
			final Brick out;
			if (parent == null)
				out = null;
			else
				out = parent.createPreviousBrick(context);
			if (adapter != null)
				adapter.notifyPreviousBrickPastEdge(context, out);
			return out;
		}

		@Override
		public Hoverable hover(final Context context, final Vector point) {
			if (selected)
				return null;
			{
				final Hoverable parentHoverable;
				if (parent == null)
					parentHoverable = null;
				else
					parentHoverable = parent.hover(context, point);
				if (parentHoverable != null)
					return parentHoverable;
			}
			if (hoverable != null)
				return hoverable;
			border = new BorderAttachment(context, context.syntax.hoverStyle);
			createAdapter(context);
			hoverable = new Hoverable() {
				@Override
				public void clear(final Context context) {
					border.destroy(context);
					border = null;
					adapter.destroy(context);
					adapter = null;
					hoverable = null;
				}

				@Override
				public void click(final Context context) {
					select(context);
				}

				@Override
				public NodeType.NodeTypeVisual node() {
					if (VisualNodeBase.this.parent == null)
						return null;
					return VisualNodeBase.this.parent.getNode();
				}

				@Override
				public VisualNodePart part() {
					return VisualNodeBase.this;
				}
			};
			return hoverable;
		}
	}
}