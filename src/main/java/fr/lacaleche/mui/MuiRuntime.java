package fr.lacaleche.mui;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/** Observable process-wide bootstrap state for the client binding. */
public final class MuiRuntime {

    public enum State {
        NOT_STARTED,
        STARTING,
        READY,
        FAILED,
        STOPPING,
        STOPPED
    }

    private static final AtomicReference<State> STATE = new AtomicReference<>(State.NOT_STARTED);
    private static volatile Throwable failure;

    private MuiRuntime() {
    }

    public static State state() {
        return STATE.get();
    }

    public static Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }

    public static void requireReady() {
        State current = STATE.get();
        if (current == State.READY) return;
        if (current == State.FAILED) {
            throw new IllegalStateException("ModernUI MC Lite failed to initialize", failure);
        }
        throw new IllegalStateException("ModernUI MC Lite is not ready: " + current);
    }

    public static void starting() {
        if (!STATE.compareAndSet(State.NOT_STARTED, State.STARTING)) {
            throw new IllegalStateException("ModernUI MC Lite bootstrap started twice");
        }
    }

    public static void ready() {
        STATE.compareAndSet(State.STARTING, State.READY);
    }

    public static void failed(Throwable cause) {
        failure = cause;
        STATE.set(State.FAILED);
    }

    public static void stopping() {
        State current = STATE.get();
        if (current != State.STOPPED) STATE.set(State.STOPPING);
    }

    public static void stopped() {
        STATE.set(State.STOPPED);
    }
}
