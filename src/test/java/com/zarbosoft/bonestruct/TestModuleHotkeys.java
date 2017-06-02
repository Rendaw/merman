package com.zarbosoft.bonestruct;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zarbosoft.bonestruct.editor.hid.HIDEvent;
import com.zarbosoft.bonestruct.helper.*;
import com.zarbosoft.bonestruct.modules.hotkeys.HotkeyRule;
import com.zarbosoft.bonestruct.modules.hotkeys.Hotkeys;
import com.zarbosoft.bonestruct.modules.hotkeys.Key;
import com.zarbosoft.bonestruct.modules.hotkeys.grammar.Terminal;
import com.zarbosoft.bonestruct.syntax.FreeAtomType;
import com.zarbosoft.bonestruct.syntax.Syntax;
import org.junit.Test;

public class TestModuleHotkeys {
	final public static FreeAtomType one;
	final static Syntax syntax;

	static {
		final Hotkeys hotkeys = new Hotkeys();
		{
			final HotkeyRule rule = new HotkeyRule();
			final Terminal terminal = new Terminal();
			terminal.key = Key.Q;
			terminal.press = true;
			rule.hotkeys.put("delete", ImmutableList.of(terminal));
			rule.freeTyping = false;
			hotkeys.rules.add(rule);
		}
		one = new TypeBuilder("one").back(Helper.buildBackPrimitive("one")).frontMark("3_1").build();
		syntax = new SyntaxBuilder("any")
				.type(one)
				.group("any", new GroupBuilder().type(one).build())
				.style(new StyleBuilder().split(true).build())
				.build();
		syntax.modules.add(hotkeys);
	}

	@Test
	public void testInitialHotkeys() {
		new GeneralTestWizard(syntax, new TreeBuilder(one).build())
				.sendHIDEvent(new HIDEvent(Key.Q, true, ImmutableSet.of()))
				.checkTree(syntax.gap.create());
	}
}
