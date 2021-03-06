package com.zarbosoft.merman.syntax.back;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.merman.document.values.ValuePrimitive;
import com.zarbosoft.merman.editor.backevents.JFloatEvent;
import com.zarbosoft.merman.syntax.AtomType;
import com.zarbosoft.merman.syntax.Syntax;
import com.zarbosoft.pidgoon.Node;
import com.zarbosoft.pidgoon.events.MatchingEventTerminal;
import com.zarbosoft.pidgoon.events.Operator;
import com.zarbosoft.pidgoon.events.Store;
import com.zarbosoft.pidgoon.internal.Helper;
import com.zarbosoft.rendaw.common.Pair;

import java.util.Set;

@Configuration(name = "json_data_float")
public class BackDataJSONFloat extends BackPart {
	@Configuration
	public String middle;

	@Override
	public Node buildBackRule(final Syntax syntax, final AtomType atomType) {
		return new Operator(new MatchingEventTerminal(new JFloatEvent(null)), store -> {
			store = (Store) store.pushStack(new Pair<>(middle,
					new ValuePrimitive(atomType.getDataPrimitive(middle), ((JFloatEvent) store.top()).value)
			));
			return Helper.stackSingleElement(store);
		});
	}

	public void finish(final Syntax syntax, final AtomType atomType, final Set<String> middleUsed) {
		middleUsed.add(middle);
		atomType.getDataPrimitive(middle);
	}
}
