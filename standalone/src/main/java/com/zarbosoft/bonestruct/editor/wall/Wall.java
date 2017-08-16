package com.zarbosoft.bonestruct.editor.wall;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.bonestruct.editor.Context;
import com.zarbosoft.bonestruct.editor.IdleTask;
import com.zarbosoft.bonestruct.editor.display.Group;
import com.zarbosoft.rendaw.common.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.flatStream;
import static com.zarbosoft.rendaw.common.Common.last;

public class Wall {
	/*
	Cornerstone may be null.
	Cornerstone course is only null in transition.
	 */
	public final Group visual;
	public List<Course> children = new ArrayList<>();
	private IdleAdjustTask idleAdjust;
	private IdleCompactTask idleCompact;
	private IdleExpandTask idleExpand;
	public Brick cornerstone;
	public Course cornerstoneCourse;
	Set<Bedding> bedding = new HashSet<>();
	public int beddingBefore = 0;
	int beddingAfter = 0;
	Set<BeddingListener> beddingListeners = new HashSet<>();
	Set<CornerstoneListener> cornerstoneListeners = new HashSet<>();

	public static abstract class CornerstoneListener {
		public abstract void cornerstoneChanged(Context context, Brick brick);
	}

	public static abstract class BeddingListener {

		public abstract void beddingChanged(Context context, int beddingBefore, int beddingAfter);
	}

	public Wall(final Context context) {
		visual = context.display.group();
	}

	public Stream<Brick> streamRange(
			final int courseStart, final int brickStart, final int courseEnd, final int brickEnd
	) {
		final List<Brick> startBricks = children.get(courseStart).children;
		if (courseStart == courseEnd)
			return startBricks.subList(brickStart, brickEnd + 1).stream();
		final List<Brick> endBricks = children.get(courseEnd).children;
		return flatStream(
				startBricks.subList(brickStart, startBricks.size()).stream(),
				children.subList(courseStart + 1, courseEnd).stream().flatMap(course -> course.children.stream()),
				endBricks.subList(0, brickEnd).stream()
		);
	}

	public void clear(final Context context) {
		while (!children.isEmpty())
			last(children).destroy(context);
		if (idleCompact != null)
			idleCompact.destroy();
		if (idleExpand != null)
			idleExpand.destroy();
		if (idleAdjust != null)
			idleAdjust.destroy();
	}

	private void renumber(final int at) {
		for (int index = at; index < children.size(); ++index) {
			children.get(index).index = index;
		}
	}

	private void getIdle(final Context context) {
		if (idleAdjust == null) {
			idleAdjust = new IdleAdjustTask(context);
			context.addIdle(idleAdjust);
		}
	}

	void add(final Context context, final int at, final List<Course> courses) {
		final boolean adjustForward = cornerstoneCourse == null ? true : at > cornerstoneCourse.index;
		children.addAll(at, courses);
		courses.stream().forEach(l -> l.parent = this);
		renumber(at);
		visual.addAll(at, courses.stream().map(l -> l.visual).collect(Collectors.toList()));
		getIdle(context);
		if (children.size() > 1) {
			if (idleAdjust.backward >= at)
				idleAdjust.backward += 1;
			if (idleAdjust.forward >= at && idleAdjust.forward < Integer.MAX_VALUE)
				idleAdjust.forward += 1;
			idleAdjust.at(at);
		}
	}

	void remove(final Context context, final int at) {
		if (cornerstoneCourse != null && cornerstoneCourse.index == at) {
			cornerstoneCourse = null;
		}
		children.remove(at);
		visual.remove(at);
		if (at < children.size()) {
			renumber(at);
			getIdle(context);
			if (at < idleAdjust.backward)
				idleAdjust.backward -= 1;
			if (at < idleAdjust.forward && idleAdjust.forward < Integer.MAX_VALUE)
				idleAdjust.forward -= 1;
			idleAdjust.at(at);
		}
	}

	public void idleCompact(final Context context) {
		if (idleCompact == null) {
			idleCompact = new IdleCompactTask(context);
			context.addIdle(idleCompact);
		}
		idleCompact.at = 0;
	}

	public void idleExpand(final Context context) {
		if (idleExpand == null) {
			idleExpand = new IdleExpandTask(context);
			context.addIdle(idleExpand);
		}
		idleExpand.at = 0;
	}

	public Set<BeddingListener> getBeddingListeners() {
		return beddingListeners;
	}

	public void addBeddingListener(final Context context, final BeddingListener listener) {
		beddingListeners.add(listener);
		listener.beddingChanged(context, beddingBefore, beddingAfter);
	}

	public void removeBeddingListener(final BeddingListener listener) {
		beddingListeners.remove(listener);
	}

	public void addCornerstoneListener(final Context context, final CornerstoneListener listener) {
		cornerstoneListeners.add(listener);
		if (cornerstone != null)
			listener.cornerstoneChanged(context, cornerstone);
	}

	public void removeCornerstoneListener(final CornerstoneListener listener) {
		cornerstoneListeners.remove(listener);
	}

	public void addBedding(final Context context, final Bedding bedding) {
		this.bedding.add(bedding);
		beddingChanged(context);
	}

	public void removeBedding(final Context context, final Bedding bedding) {
		this.bedding.remove(bedding);
		beddingChanged(context);
	}

	private void beddingChanged(final Context context) {
		final Pair<Integer, Integer> pair = ImmutableList
				.copyOf(bedding)
				.stream()
				.map(b -> new Pair<>(b.before, b.after))
				.reduce((a, b) -> new Pair<>(a.first + b.first, a.second + b.second))
				.orElse(new Pair<>(0, 0));
		beddingBefore = pair.first;
		beddingAfter = pair.second;
		ImmutableList
				.copyOf(beddingListeners)
				.stream()
				.forEach(a -> a.beddingChanged(context, beddingBefore, beddingAfter));
		adjust(context, cornerstoneCourse.index);
	}

	public void setCornerstone(final Context context, final Brick cornerstone) {
		this.cornerstone = cornerstone;
		if (cornerstone == null) {
			this.cornerstoneCourse = null;
		} else {
			if (cornerstone.parent == null) {
				clear(context);
				final Course course = new Course(context);
				add(context, 0, ImmutableList.of(course));
				course.add(context, 0, ImmutableList.of(cornerstone));
			}
			this.cornerstoneCourse = cornerstone.parent;
			if (beddingBefore > 0 || beddingAfter > 0)
				adjust(context, cornerstoneCourse.index);
			ImmutableList
					.copyOf(cornerstoneListeners)
					.forEach(listener -> listener.cornerstoneChanged(context, cornerstone));
			context.idleLayBricksBeforeStart(cornerstone);
			context.idleLayBricksAfterEnd(cornerstone);
		}
	}

	class IdleCompactTask extends IdleTask {
		private final Context context;
		Course.IdleCompactTask compactTask;
		int at = 0;

		IdleCompactTask(final Context context) {
			this.context = context;
		}

		@Override
		protected int priority() {
			return 110;
		}

		@Override
		public boolean runImplementation() {
			if (at >= children.size()) {
				return false;
			}
			if (compactTask == null) {
				compactTask = children.get(at).new IdleCompactTask(context);
				at++;
			}
			if (!compactTask.run()) {
				compactTask = null;
			}
			return true;
		}

		@Override
		protected void destroyed() {
			idleCompact = null;
		}
	}

	class IdleExpandTask extends IdleTask {
		private final Context context;
		Course.IdleExpandTask expandTask;
		int at = 0;

		IdleExpandTask(final Context context) {
			this.context = context;
		}

		@Override
		protected int priority() {
			return -100;
		}

		@Override
		public boolean runImplementation() {
			if (at >= children.size()) {
				return false;
			}
			if (expandTask == null) {
				expandTask = children.get(at).new IdleExpandTask(context);
				at++;
			}
			if (!expandTask.run()) {
				expandTask = null;
			}
			return true;
		}

		@Override
		protected void destroyed() {
			idleExpand = null;
		}
	}

	class IdleAdjustTask extends IdleTask {
		final private Context context;
		int forward = Integer.MAX_VALUE;
		int backward = Integer.MIN_VALUE;

		IdleAdjustTask(final Context context) {
			this.context = context;
		}

		@Override
		protected int priority() {
			return 160;
		}

		@Override
		public boolean runImplementation() {
			boolean modified = false;
			if (cornerstoneCourse.index <= backward || cornerstoneCourse.index >= forward) {
				backward = cornerstoneCourse.index - 1;
				forward = cornerstoneCourse.index + 1;
			}
			if (backward >= 0) {
				// Always < children size because of cornerstone
				final Course child = children.get(backward);
				final Course preceding = children.get(backward + 1);
				int transverse = preceding.transverseStart - child.transverseSpan();
				if (preceding == cornerstoneCourse)
					transverse -= beddingBefore;
				child.setTransverse(context, transverse);
				backward -= 1;
				modified = true;
			}
			if (forward < children.size()) {
				// Always > 0 because of cornerstone
				int transverse = children.get(forward - 1).transverseEdge(context);
				if (forward - 1 == cornerstoneCourse.index)
					transverse += beddingAfter;
				children.get(forward).setTransverse(context, transverse);
				forward += 1;
				modified = true;
			}
			return modified;
		}

		@Override
		protected void destroyed() {
			idleAdjust = null;
		}

		public void at(final int at) {
			if (cornerstoneCourse == null)
				return;
			if (at <= cornerstoneCourse.index && at > backward)
				backward = Math.min(cornerstoneCourse.index - 1, at);
			if (at >= cornerstoneCourse.index && at < forward)
				forward = Math.max(cornerstoneCourse.index + 1, at);
		}
	}

	void adjust(final Context context, final int at) {
		getIdle(context);
		idleAdjust.at(at);
	}
}
