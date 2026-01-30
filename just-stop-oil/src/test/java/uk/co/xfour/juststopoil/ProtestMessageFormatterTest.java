package uk.co.xfour.juststopoil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProtestMessageFormatterTest {
    @Test
    void buildMessageCombinesPrefixAndMessage() {
        String result = ProtestMessageFormatter.buildMessage("[Prefix]", "Hello");
        assertEquals("[Prefix] Hello", result);
    }

    @Test
    void buildMessageHandlesMissingPrefix() {
        String result = ProtestMessageFormatter.buildMessage("", "Hello");
        assertEquals("Hello", result);
    }

    @Test
    void buildMessageHandlesMissingMessage() {
        String result = ProtestMessageFormatter.buildMessage("[Prefix]", "");
        assertEquals("[Prefix]", result);
    }
}
