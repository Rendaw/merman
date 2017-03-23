package com.zarbosoft.bonestruct.editor.visual.nodes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zarbosoft.bonestruct.editor.model.Hotkeys;
import com.zarbosoft.bonestruct.editor.model.Node;
import com.zarbosoft.bonestruct.editor.model.NodeType;
import com.zarbosoft.bonestruct.editor.model.front.FrontConstantPart;
import com.zarbosoft.bonestruct.editor.model.middle.DataArrayBase;
import com.zarbosoft.bonestruct.editor.visual.Context;
import com.zarbosoft.bonestruct.editor.visual.Vector;
import com.zarbosoft.bonestruct.editor.visual.attachments.BorderAttachment;
import com.zarbosoft.bonestruct.editor.visual.attachments.MultiVisualAttachmentAdapter;
import com.zarbosoft.bonestruct.editor.visual.attachments.VisualAttachmentAdapter;
import com.zarbosoft.bonestruct.editor.visual.attachments.VisualBorderAttachment;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNode;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNodeParent;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNodePart;
import com.zarbosoft.bonestruct.editor.visual.wall.Brick;
import com.zarbosoft.rendaw.common.Pair;
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.zarbosoft.rendaw.common.Common.enumerate;
import static com.zarbosoft.rendaw.common.Common.last;
import static java.io.File.separator;

public abstract class ArrayVisualNode extends GroupVisualNode {

	private final DataArrayBase.Listener dataListener;
	private final DataArrayBase.Value data;

	public ArrayVisualNode(final Context context, final DataArrayBase.Value data, final Set<VisualNode.Tag> tags) {
		super(tags);
		this.data = data;
		dataListener = new DataArrayBase.Listener() {
			@Override
			public void added(final Context context, final int index, final List<Node> nodes) {
				ArrayVisualNode.this.add(context, index, nodes);
			}

			@Override
			public void removed(final Context context, final int index, final int count) {
				remove(context, index, count);
			}
		};
		data.addListener(dataListener);
		add(context, 0, data.get());
	}

	@Override
	public boolean select(final Context context) {
		if (children.isEmpty())
			return false;
		((ArrayVisualNodeParent) children.get(0).parent()).selectDown(context);
		return true;
	}

	@Override
	protected VisualNodeParent createParent(final int index) {
		final boolean selectable = ((ChildGroup) children.get(index)).selectable;
		return new ArrayVisualNodeParent(index, selectable);
	}

	@Override
	public void remove(final Context context, int start, int size) {
		if (!getSeparator().isEmpty()) {
			size += size - 1;
			start *= 2;
		}
		final boolean retagFirst = tagFirst() && start == 0;
		final boolean retagLast = tagLast() && start + size == children.size();
		super.remove(context, start, size);
		if (!children.isEmpty()) {
			if (retagFirst)
				children.get(0).changeTags(context, new VisualNode.TagsChange().add(new VisualNode.PartTag("first")));
			if (retagLast)
				last(children).changeTags(context, new VisualNode.TagsChange().add(new VisualNode.PartTag("last")));
		}
		if (hoverable != null) {
			if (hoverable.index > start + size) {
				hoverable.setIndex(context, hoverable.index - size);
			} else if (hoverable.index >= start) {
				context.clearHover();
			}
		}
		if (selection != null) {
			if (selection.beginIndex >= start) {
				selection.setBegin(context, Math.max(start, selection.beginIndex - size));
			}
			if (selection.endIndex >= start) {
				selection.setEnd(context, Math.max(start, selection.endIndex - size));
			}
		}
	}

	protected abstract boolean tagLast();

	protected abstract boolean tagFirst();

	private class ChildGroup extends GroupVisualNode {

		private final boolean selectable;

		public ChildGroup(final Set<VisualNode.Tag> tags, final boolean selectable) {
			super(tags);
			this.selectable = selectable;
		}

		@Override
		public Brick createFirstBrick(final Context context) {
			final Brick out = super.createFirstBrick(context);
			if (selection != null) {
				if (selection.beginIndex == ((ArrayVisualNodeParent) parent).index)
					selection.border.setFirst(context, out);
				if (selection.endIndex == ((ArrayVisualNodeParent) parent).index)
					selection.adapter.notifySeedBrick(context, out);
			}
			if (hoverable != null && hoverable.index == ((ArrayVisualNodeParent) parent).index) {
				hoverable.border.setFirst(context, out);
				hoverable.border.notifySeedBrick(context, out);
			}
			return out;
		}

		@Override
		public Brick createLastBrick(final Context context) {
			final Brick out = super.createLastBrick(context);
			if (selection != null) {
				if (selection.beginIndex == ((ArrayVisualNodeParent) parent).index)
					selection.adapter.notifySeedBrick(context, out);
				if (selection.endIndex == ((ArrayVisualNodeParent) parent).index)
					selection.border.setLast(context, out);
			}
			if (hoverable != null && hoverable.index == ((ArrayVisualNodeParent) parent).index) {
				hoverable.border.setLast(context, out);
				hoverable.border.notifySeedBrick(context, out);
			}
			return out;
		}
	}

	public void add(final Context context, final int start, final List<Node> nodes) {
		final boolean retagFirst = tagFirst() && start == 0;
		final boolean retagLast = tagLast() && start == children.size();
		if (!children.isEmpty()) {
			if (retagFirst)
				children
						.get(0)
						.changeTags(context, new VisualNode.TagsChange().remove(new VisualNode.PartTag("first")));
			if (retagLast)
				last(children).changeTags(context, new VisualNode.TagsChange().remove(new VisualNode.PartTag("last")));
		}
		final PSet<VisualNode.Tag> tags = HashTreePSet.from(tags());
		enumerate(nodes.stream(), start).forEach(p -> {
			int index = p.first;
			if (p.first > 0 && !separator.isEmpty()) {
				index = index * 2 - 1;
				final ChildGroup group = new ChildGroup(ImmutableSet.of(), false);
				for (final FrontConstantPart fix : getSeparator())
					group.add(context, fix.createVisual(context, tags.plus(new VisualNode.PartTag("separator"))));
				super.add(context, group, index++);
			}
			final ChildGroup group = new ChildGroup(ImmutableSet.of(), true);
			for (final FrontConstantPart fix : getPrefix())
				group.add(context, fix.createVisual(context, tags.plus(new VisualNode.PartTag("prefix"))));
			final VisualNode nodeVisual = p.second.createVisual(context);
			group.add(context, new VisualNodePart(tags.plus(new VisualNode.PartTag("nested"))) {
				@Override
				public void setParent(final VisualNodeParent parent) {
					nodeVisual.setParent(parent);
				}

				@Override
				public VisualNodeParent parent() {
					return nodeVisual.parent();
				}

				@Override
				public boolean select(final Context context) {
					return nodeVisual.select(context);
				}

				@Override
				public Brick createFirstBrick(final Context context) {
					return nodeVisual.createFirstBrick(context);
				}

				@Override
				public Brick createLastBrick(final Context context) {
					return nodeVisual.createLastBrick(context);
				}

				@Override
				public Brick getFirstBrick(final Context context) {
					return nodeVisual.getFirstBrick(context);
				}

				@Override
				public Brick getLastBrick(final Context context) {
					return nodeVisual.getLastBrick(context);
				}

				@Override
				public Iterable<Pair<Brick, Brick.Properties>> getPropertiesForTagsChange(
						final Context context, final TagsChange change
				) {
					return nodeVisual.getPropertiesForTagsChange(context, change);
				}

				@Override
				public void destroyBricks(final Context context) {
					nodeVisual.destroyBricks(context);
				}
			});
			for (final FrontConstantPart fix : getSuffix())
				group.add(context, fix.createVisual(context, tags.plus(new VisualNode.PartTag("suffix"))));
			super.add(context, group, index);
		});
		if (!children.isEmpty()) {
			if (retagFirst)
				children.get(0).changeTags(context, new VisualNode.TagsChange().add(new VisualNode.PartTag("first")));
			if (retagLast)
				last(children).changeTags(context, new VisualNode.TagsChange().add(new VisualNode.PartTag("last")));
		}
		if (hoverable != null && hoverable.index >= start) {
			hoverable.setIndex(context, hoverable.index + nodes.size());
		}
		if (selection != null) {
			if (selection.endIndex >= start) {
				selection.setEnd(context, selection.endIndex + nodes.size());
			}
			if (selection.beginIndex >= start) {
				selection.setBegin(context, selection.beginIndex - nodes.size());
			}
		}
	}

	protected abstract Map<String, com.zarbosoft.luxemj.grammar.Node> getHotkeys();

	protected abstract List<FrontConstantPart> getPrefix();

	protected abstract List<FrontConstantPart> getSeparator();

	protected abstract List<FrontConstantPart> getSuffix();

	private ArrayHoverable hoverable;

	private class ArrayHoverable extends Context.Hoverable {
		private int index;
		VisualBorderAttachment border;

		public ArrayHoverable(final Context context) {
			border = new VisualBorderAttachment(context, context.syntax.hoverStyle);
		}

		public void setIndex(final Context context, final int index) {
			this.index = index;
			border.setFirst(context, children.get(index));
			border.setLast(context, children.get(index));
		}

		@Override
		public void clear(final Context context) {
			border.destroy(context);
			hoverable = null;
		}

		@Override
		public void click(final Context context) {
			((ArrayVisualNodeParent) children.get(index).parent()).selectDown(context);
		}

		@Override
		public NodeType.NodeTypeVisual node() {
			if (ArrayVisualNode.this.parent == null)
				return null;
			return ArrayVisualNode.this.parent.getNode();
		}

		@Override
		public VisualNodePart part() {
			return ArrayVisualNode.this;
		}
	}

	private ArraySelection selection;

	private class ArraySelection extends Context.Selection {
		MultiVisualAttachmentAdapter adapter;
		BorderAttachment border;
		int beginIndex;
		int endIndex;

		public ArraySelection(final Context context, final int index) {
			if (context.display != null) {
				border = new BorderAttachment(context, context.syntax.selectStyle);
				adapter = new MultiVisualAttachmentAdapter(context);
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
			setBegin(context, index);
			setEnd(context, index);
		}

		private void setEnd(final Context context, final int index) {
			endIndex = index;
			if (context.display != null) {
				adapter.setLast(context, children.get(index));
			}
		}

		private void setBegin(final Context context, final int index) {
			beginIndex = index;
			if (context.display != null) {
				adapter.setFirst(context, children.get(index));
			}
		}

		@Override
		protected Hotkeys getHotkeys(final Context context) {
			return context.getHotkeys(tags());
		}

		@Override
		public void clear(final Context context) {
			if (context.display != null) {
				adapter.destroy(context);
				border.destroy(context);
			}
			selection = null;
		}

		@Override
		public void addBrickListener(final Context context, final VisualAttachmentAdapter.BoundsListener listener) {
			adapter.addListener(context, listener);
			final Brick first = getVisual().getFirstBrick(context);
			final Brick last = getVisual().getLastBrick(context);
			if (first != null)
				listener.firstChanged(context, first);
			if (last != null)
				listener.lastChanged(context, last);
		}

		@Override
		public void removeBrickListener(final Context context, final VisualAttachmentAdapter.BoundsListener listener) {
			adapter.removeListener(context, listener);
		}

		@Override
		public Iterable<Context.Action> getActions(final Context context) {
			return ImmutableList.of(new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "enter";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "exit";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "next";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "previous";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "insert-before";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "insert-after";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "copy";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "cut";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "paste";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "reset-selection";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "gather-next";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "gather-previous";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "move-before";
				}
			}, new Context.Action() {
				@Override
				public void run(final Context context) {

				}

				@Override
				public String getName() {
					return "move-after";
				}
			});
		}

		@Override
		public VisualNodePart getVisual() {
			return children.get(beginIndex);
		}
	}

	private class ArrayVisualNodeParent extends GroupVisualNodeParent {

		private final boolean selectable;

		public ArrayVisualNodeParent(final int index, final boolean selectable) {
			super(ArrayVisualNode.this, index);
			this.selectable = selectable;
		}

		public void selectDown(final Context context) {
			if (hoverable != null && hoverable.index == index) {
				context.clearHover();
			}
			selection = new ArraySelection(context, index);
			context.setSelection(selection);
		}

		@Override
		public Context.Hoverable hover(final Context context, final Vector point) {
			if (!selectable) {
				if (parent != null)
					return parent.hover(context, point);
				return null;
			}
			if (selection != null && selection.beginIndex == index && selection.endIndex == index) {
				if (hoverable != null)
					context.clearHover();
				return null;
			}
			if (hoverable == null) {
				hoverable = new ArrayHoverable(context);
			}
			hoverable.setIndex(context, index);
			return hoverable;
		}

		@Override
		public Brick createPreviousBrick(final Context context) {
			final Brick next = super.createNextBrick(context);
			if (selection != null && index == selection.beginIndex)
				selection.adapter.notifyPreviousBrickPastEdge(context, next);
			if (hoverable != null && index == hoverable.index)
				hoverable.border.notifyPreviousBrickPastEdge(context, next);
			return next;
		}

		@Override
		public Brick createNextBrick(final Context context) {
			final Brick next = super.createNextBrick(context);
			if (selection != null && index == selection.endIndex)
				selection.adapter.notifyNextBrickPastEdge(context, next);
			if (hoverable != null && index == hoverable.index)
				hoverable.border.notifyNextBrickPastEdge(context, next);
			return next;
		}
	}
}
