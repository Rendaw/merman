package com.zarbosoft.bonestruct.syntax.front;

import com.zarbosoft.bonestruct.document.values.Value;
import com.zarbosoft.bonestruct.editor.Context;
import com.zarbosoft.bonestruct.editor.visual.nodes.VisualNode;
import com.zarbosoft.bonestruct.editor.visual.tree.VisualNodePart;
import com.zarbosoft.bonestruct.syntax.NodeType;
import com.zarbosoft.bonestruct.syntax.hid.grammar.Node;
import com.zarbosoft.bonestruct.syntax.middle.MiddleNode;
import com.zarbosoft.interface1.Configuration;
import org.pcollections.HashTreePSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration(name = "node")
public class FrontDataNode extends FrontPart {

	@Override
	public String middle() {
		return middle;
	}

	@Configuration
	public String middle;
	private MiddleNode dataType;

	@Configuration(optional = true)
	public Map<String, Node> hotkeys = new HashMap<>();

	@Override
	public VisualNodePart createVisual(
			final Context context,
			final Map<String, Value> data,
			final Set<com.zarbosoft.bonestruct.editor.visual.tree.VisualNode.Tag> tags
	) {
		return new VisualNode(
				context,
				dataType.get(data),
				HashTreePSet
						.from(tags)
						.plus(new com.zarbosoft.bonestruct.editor.visual.tree.VisualNode.PartTag("nested"))
						.plusAll(this.tags
								.stream()
								.map(s -> new com.zarbosoft.bonestruct.editor.visual.tree.VisualNode.FreeTag(s))
								.collect(Collectors.toSet()))
		);
	}

	@Override
	public void finish(final NodeType nodeType, final Set<String> middleUsed) {
		middleUsed.add(middle);
		dataType = nodeType.getDataNode(middle);
	}

	@Override
	public void dispatch(final DispatchHandler handler) {
		handler.handle(this);
	}
}