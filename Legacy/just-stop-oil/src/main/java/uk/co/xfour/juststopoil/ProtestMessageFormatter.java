package uk.co.xfour.juststopoil;

/**
 * Formats protest messages with a prefix suitable for chat output.
 */
public final class ProtestMessageFormatter {
    private ProtestMessageFormatter() {
    }

    /**
     * Builds a chat message by combining a prefix and a message.
     *
     * @param prefix the formatted prefix to use, may be blank
     * @param message the formatted message to append
     * @return the combined message
     */
    public static String buildMessage(String prefix, String message) {
        String safePrefix = prefix == null ? "" : prefix.trim();
        String safeMessage = message == null ? "" : message.trim();
        if (safePrefix.isEmpty()) {
            return safeMessage;
        }
        if (safeMessage.isEmpty()) {
            return safePrefix;
        }
        return safePrefix + " " + safeMessage;
    }
}
