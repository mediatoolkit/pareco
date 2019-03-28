package com.mediatoolkit.pareco.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 2019-03-21
 */
@Component
@ConditionalOnProperty(
	value = "session.auto-expire.enabled", havingValue = "true", matchIfMissing = true
)
public class SessionExpireTrigger {

	private final SessionRepository sessionRepository;

	@Autowired
	public SessionExpireTrigger(SessionRepository sessionRepository) {
		this.sessionRepository = sessionRepository;
	}

	@Scheduled(fixedDelay = 10_000L)
	public void triggerExpire() {
		sessionRepository.expireInactiveSessions();
	}
}
