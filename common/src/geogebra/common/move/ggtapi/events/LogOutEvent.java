package geogebra.common.move.ggtapi.events;

import geogebra.common.move.events.BaseEvent;

/**
 * @author gabor
 *  Creates a new LogOutEvent
 */
public class LogOutEvent extends BaseEvent {
	public LogOutEvent() {
		super("logout");
	}

	@Override
	public void trigger() {
		// No action
	}
}
