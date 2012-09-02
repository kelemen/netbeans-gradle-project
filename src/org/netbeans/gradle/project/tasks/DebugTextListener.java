package org.netbeans.gradle.project.tasks;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DebugTextListener implements TaskOutputListener {
    public static interface DebugeeListener {
        public void onDebugeeListening(int port);
    }

    private static final Logger LOGGER = Logger.getLogger(DebugTextListener.class.getName());

    // The implementation relies on the fact that the first character of this
    // text is not found anywhere else in the text.
    private static final String LISTEN_TEXT = "Listening for transport dt_socket at address";

    private final DebugeeListener listener;
    private final Lock mainLock;
    private int currentPosition;
    private boolean found;
    private volatile boolean done;
    private StringBuilder postFoundBuffer;

    public DebugTextListener(DebugeeListener listener) {
        if (listener == null)
            throw new NullPointerException("listener");
        this.mainLock = new ReentrantLock();
        this.listener = listener;
        this.currentPosition = 0;
        this.found = false;
        this.done = false;
        this.postFoundBuffer = null;
    }

    @Override
    public void receiveOutput(char[] buffer, int offset, int length) {
        if (done) {
            return;
        }

        String portStr = null;
        mainLock.lock();
        try {
            if (done) {
                return;
            }

            for (int i = 0; i < length; i++) {
                char ch = buffer[offset + i];

                if (found) {
                    if (postFoundBuffer == null) {
                        postFoundBuffer = new StringBuilder();
                    }
                    postFoundBuffer.append(ch);
                }
                else {
                    if (LISTEN_TEXT.charAt(currentPosition) == ch) {
                        currentPosition++;
                        if (currentPosition >= LISTEN_TEXT.length()) {
                            found = true;
                        }
                    }
                    else {
                        currentPosition = LISTEN_TEXT.charAt(0) == ch ? 1 : 0;
                    }
                }
            }

            if (found) {
                String postFound = postFoundBuffer.toString();
                int sepIndex = postFound.indexOf(':');
                if (sepIndex >= 0) {
                    String portPrefixedStr = postFound.substring(sepIndex + 1);
                    for (int i = 0; i < portPrefixedStr.length(); i++) {
                        if (!Character.isDigit(portPrefixedStr.charAt(i))) {
                            portStr = portPrefixedStr.substring(0, i);
                        }
                    }

                    if (portStr != null) {
                        portStr = portStr.trim();
                        done = true;
                        postFoundBuffer = null;
                    }
                }
            }
        } finally {
            mainLock.unlock();
        }

        if (portStr != null) {
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Unexpected port number: {0}", portStr);
                return;
            }

            listener.onDebugeeListening(port);
        }
    }
}
