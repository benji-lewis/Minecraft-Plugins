package uk.co.xfour.transport.modules;

/**
 * Lifecycle contract for a configurable Transport module.
 */
public interface Module {
    /**
     * Starts this module.
     */
    void start();

    /**
     * Stops this module.
     */
    void stop();
}
