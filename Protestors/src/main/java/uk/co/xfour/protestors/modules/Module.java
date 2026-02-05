package uk.co.xfour.protestors.modules;

/**
 * Lifecycle contract for a configurable Protestors module.
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
