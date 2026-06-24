package nl.rslot.ssx.conditional.condition;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class Conditions {

	private static final Map<String, Condition> CONDITIONS_BY_TYPE;

	static {
		final Condition[] conditions = new Condition[] {
				new CurrentServerCondition(),
				new EffectCondition(),
				new HasHiddenOthersCondition(),
				new InWorldCondition(),
				new OpenMenuCondition(),
				new PapiCondition(),
				new PermissionCondition(),
				new ServerPlaceholderCondition(),
				new ServerOnlineCondition(),
		};

		CONDITIONS_BY_TYPE = new HashMap<>(conditions.length);

		for (final Condition condition : conditions) {
			CONDITIONS_BY_TYPE.put(condition.getType(), condition);
		}
	}

	public static @Nullable Condition getConditionByType(final String type) {
		return CONDITIONS_BY_TYPE.get(type);
	}

}
