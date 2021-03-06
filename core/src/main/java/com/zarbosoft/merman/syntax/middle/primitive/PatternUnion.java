package com.zarbosoft.merman.syntax.middle.primitive;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.Node;

import java.util.List;

@Configuration(name = "union")
public class PatternUnion extends Pattern {
	@Configuration
	public List<Pattern> children;

	@Override
	public Node build() {
		final com.zarbosoft.pidgoon.nodes.Union out = new com.zarbosoft.pidgoon.nodes.Union();
		for (final Pattern child : children)
			out.add(child.build());
		return out;
	}
}
