package com.zarbosoft.bonestruct;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.bonestruct.document.Atom;
import com.zarbosoft.bonestruct.document.values.ValueArray;
import com.zarbosoft.bonestruct.editor.history.changes.ChangeArray;
import com.zarbosoft.bonestruct.editor.visual.tags.FreeTag;
import com.zarbosoft.bonestruct.editor.visual.tags.StateTag;
import com.zarbosoft.bonestruct.helper.GeneralTestWizard;
import com.zarbosoft.bonestruct.helper.Helper;
import com.zarbosoft.bonestruct.helper.StyleBuilder;
import com.zarbosoft.bonestruct.syntax.FreeAtomType;
import com.zarbosoft.bonestruct.syntax.Syntax;
import org.junit.Test;

public class TestCompaction {
	final public static FreeAtomType one;
	final public static FreeAtomType infinity;
	final public static FreeAtomType low;
	final public static FreeAtomType mid;
	final public static FreeAtomType high;
	final public static Syntax syntax;

	static {
		infinity = new Helper.TypeBuilder("infinity")
				.back(Helper.buildBackPrimitive("infinity"))
				.front(new Helper.FrontMarkBuilder("infinity").build())
				.build();
		one = new Helper.TypeBuilder("one")
				.back(Helper.buildBackPrimitive("one"))
				.front(new Helper.FrontMarkBuilder("one").build())
				.build();
		low = new Helper.TypeBuilder("low")
				.middleArray("value", "any")
				.back(Helper.buildBackDataArray("value"))
				.front(new Helper.FrontDataArrayBuilder("value")
						.addPrefix(new Helper.FrontSpaceBuilder().tag("split").build())
						.build())
				.precedence(0)
				.depthScore(1)
				.build();
		mid = new Helper.TypeBuilder("mid")
				.middleArray("value", "any")
				.back(Helper.buildBackDataArray("value"))
				.front(new Helper.FrontDataArrayBuilder("value")
						.addPrefix(new Helper.FrontSpaceBuilder().tag("split").build())
						.build())
				.precedence(50)
				.depthScore(1)
				.build();
		high = new Helper.TypeBuilder("high")
				.middleArray("value", "any")
				.back(Helper.buildBackDataArray("value"))
				.front(new Helper.FrontDataArrayBuilder("value")
						.addPrefix(new Helper.FrontSpaceBuilder().tag("split").build())
						.build())
				.precedence(100)
				.depthScore(1)
				.build();
		syntax = new Helper.SyntaxBuilder("any")
				.type(one)
				.type(infinity)
				.type(low)
				.type(mid)
				.type(high)
				.group("any", new Helper.GroupBuilder().type(infinity).type(one).type(low).type(mid).type(high).build())
				.style(new StyleBuilder().tag(new FreeTag("split")).tag(new StateTag("compact")).broken(true).build())
				.build();
		syntax.ellipsizeThreshold = 2;
	}

	@Test
	public void testSplitOnResize() {
		new GeneralTestWizard(syntax,
				new Helper.TreeBuilder(low)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build()
		)
				.resize(40)
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 1, "one")
				.resize(10_000_000)
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(0, 3, "one");
	}

	@Test
	public void testSplitOrder() {
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(low).addArray("value",
				new Helper.TreeBuilder(high)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				new Helper.TreeBuilder(one).build()
		).build())
				.resize(70)
				.checkTextBrick(0, 2, "one")
				.checkTextBrick(0, 4, "one")
				.checkTextBrick(1, 1, "one")
				.resize(40)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 1, "one")
				.resize(70)
				.checkTextBrick(0, 2, "one")
				.checkTextBrick(0, 4, "one")
				.checkTextBrick(1, 1, "one")
				.resize(10_000_000)
				.checkTextBrick(0, 2, "one")
				.checkTextBrick(0, 4, "one")
				.checkTextBrick(0, 6, "one");
	}

	@Test
	public void testSplitOrderInverted() {
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(high).addArray("value",
				new Helper.TreeBuilder(low)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				new Helper.TreeBuilder(one).build()
		).build())
				.resize(70)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(2, 3, "one")
				.resize(40)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 1, "one")
				.resize(70)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(2, 3, "one")
				.resize(10_000_000)
				.checkTextBrick(0, 2, "one")
				.checkTextBrick(0, 4, "one")
				.checkTextBrick(0, 6, "one");
	}

	@Test
	public void testSplitOrderRule() {
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(mid).addArray("value",
				new Helper.TreeBuilder(low)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				new Helper.TreeBuilder(high).addArray("value",
						new Helper.TreeBuilder(one).build(),
						new Helper.TreeBuilder(one).build(),
						new Helper.TreeBuilder(one).build()
				).build()
		).build())
				.resize(110)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 2, "one")
				.checkTextBrick(3, 4, "one")
				.checkTextBrick(3, 6, "one")
				.resize(80)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(4, 1, "one")
				.checkTextBrick(5, 1, "one")
				.checkTextBrick(6, 1, "one")
				.resize(40)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(4, 1, "one")
				.checkTextBrick(5, 1, "one")
				.checkTextBrick(6, 1, "one")
				.resize(80)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(4, 1, "one")
				.checkTextBrick(5, 1, "one")
				.checkTextBrick(6, 1, "one")
				.resize(110)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 2, "one")
				.checkTextBrick(3, 4, "one")
				.checkTextBrick(3, 6, "one")
				.resize(10_000_000)
				.checkTextBrick(0, 2, "one")
				.checkTextBrick(0, 4, "one")
				.checkTextBrick(0, 7, "one")
				.checkTextBrick(0, 9, "one")
				.checkTextBrick(0, 11, "one");
	}

	@Test
	public void testSplitDynamic() {
		final Atom lowAtom = new Helper.TreeBuilder(low).addArray("value", new Helper.TreeBuilder(one).build()).build();
		final ValueArray array = (ValueArray) lowAtom.data.get("value");
		new GeneralTestWizard(syntax, lowAtom)
				.resize(40)
				.checkCourseCount(1)
				.checkTextBrick(0, 1, "one")
				.run(context -> context.history.apply(context,
						new ChangeArray(array, 1, 0, ImmutableList.of(new Helper.TreeBuilder(one).build()))
				))
				.checkCourseCount(2)
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 1, "one")
				.run(context -> context.history.apply(context, new ChangeArray(array, 1, 1, ImmutableList.of())))
				.checkCourseCount(1)
				.checkTextBrick(0, 1, "one");
	}

	@Test
	public void testSplitOrderRuleDynamic() {
		final Atom highAtom = new Helper.TreeBuilder(high)
				.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
				.build();
		final ValueArray array = (ValueArray) highAtom.data.get("value");
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(mid).addArray("value",
				new Helper.TreeBuilder(low)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				highAtom
		).build())
				.resize(80)
				.checkCourseCount(4)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 2, "one")
				.checkTextBrick(3, 4, "one")
				.run(context -> context.history.apply(context,
						new ChangeArray(array, 2, 0, ImmutableList.of(new Helper.TreeBuilder(one).build()))
				))
				.checkCourseCount(7)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(4, 1, "one")
				.checkTextBrick(5, 1, "one")
				.checkTextBrick(6, 1, "one")
				.run(context -> context.history.apply(context, new ChangeArray(array, 2, 1, ImmutableList.of())))
				.checkCourseCount(4)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 2, "one")
				.checkTextBrick(3, 4, "one");
	}

	@Test
	public void testStartCompactDynamic() {
		final Atom lowAtom = new Helper.TreeBuilder(low).addArray("value",
				new Helper.TreeBuilder(one).build(),
				new Helper.TreeBuilder(one).build(),
				new Helper.TreeBuilder(infinity).build()
		).build();
		final ValueArray array = (ValueArray) lowAtom.data.get("value");
		new GeneralTestWizard(syntax, lowAtom)
				.resize(100)
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "infinity")
				.run(context -> context.history.apply(context,
						new ChangeArray(array, 1, 0, ImmutableList.of(new Helper.TreeBuilder(one).build()))
				))
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(3, 1, "infinity");
	}

	@Test
	public void testCompactWindowDownSimple() {
		final Atom midAtom = new Helper.TreeBuilder(mid).addArray("value",
				new Helper.TreeBuilder(one).build(),
				new Helper.TreeBuilder(high)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				new Helper.TreeBuilder(one).build()
		).build();
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(low).addArray("value", midAtom).build())
				.act("window")
				.resize(70)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "...")
				.checkTextBrick(3, 1, "one")
				.run(context -> midAtom.parent.selectUp(context))
				.act("window")
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 2, "one")
				.checkTextBrick(1, 4, "one")
				.checkTextBrick(2, 1, "one");
	}

	@Test
	public void testCompactWindowDown() {
		final Atom midAtom = new Helper.TreeBuilder(mid).addArray("value",
				new Helper.TreeBuilder(one).build(),
				new Helper.TreeBuilder(low)
						.addArray("value", new Helper.TreeBuilder(one).build(), new Helper.TreeBuilder(one).build())
						.build(),
				new Helper.TreeBuilder(one).build()
		).build();
		new GeneralTestWizard(syntax, new Helper.TreeBuilder(high).addArray("value", midAtom).build())
				.act("window")
				.resize(70)
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "...")
				.checkTextBrick(3, 1, "one")
				.run(context -> midAtom.parent.selectUp(context))
				.act("window")
				.checkTextBrick(0, 1, "one")
				.checkTextBrick(1, 1, "one")
				.checkTextBrick(2, 1, "one")
				.checkTextBrick(2, 3, "one");
	}
}
