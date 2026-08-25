package com.kasirpintar.kaspintest.ui.common;

/** One-shot LiveData payload (e.g. a toast/snackbar message) that a screen rotation won't replay. */
public class Event<T> {

    private final T content;
    private boolean consumed = false;

    public Event(T content) {
        this.content = content;
    }

    public T consume() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return content;
    }
}
