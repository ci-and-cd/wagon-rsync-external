package org.apache.maven.wagon.providers.rsync.external;

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionEventSupport;
import org.apache.maven.wagon.events.SessionListener;

public interface SessionEvents extends Wagon {

    @Override
    default void addSessionListener(SessionListener listener) {
        this.getSessionEventSupport().addSessionListener(listener);
    }

    @Override
    default boolean hasSessionListener(SessionListener listener) {
        return this.getSessionEventSupport().hasSessionListener(listener);
    }

    @Override
    default void removeSessionListener(SessionListener listener) {
        this.getSessionEventSupport().removeSessionListener(listener);
    }

    SessionEventSupport getSessionEventSupport();

    default void fireSessionDisconnected() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTED);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionDisconnected(sessionEvent);
    }

    default void fireSessionDisconnecting() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_DISCONNECTING);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionDisconnecting(sessionEvent);
    }

    default void fireSessionLoggedIn() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_LOGGED_IN);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionLoggedIn(sessionEvent);
    }

    default void fireSessionLoggedOff() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_LOGGED_OFF);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionLoggedOff(sessionEvent);
    }

    default void fireSessionOpened() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENED);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionOpened(sessionEvent);
    }

    default void fireSessionOpening() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_OPENING);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionOpening(sessionEvent);
    }

    default void fireSessionConnectionRefused() {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, SessionEvent.SESSION_CONNECTION_REFUSED);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionConnectionRefused(sessionEvent);
    }

    default void fireSessionError(Exception exception) {
        long timestamp = System.currentTimeMillis();

        SessionEvent sessionEvent = new SessionEvent(this, exception);

        sessionEvent.setTimestamp(timestamp);

        this.getSessionEventSupport().fireSessionError(sessionEvent);

    }

    default void fireSessionDebug(String message) {
        this.getSessionEventSupport().fireDebug(message);
    }
}
