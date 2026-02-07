package uk.co.xfour.furniture.modules;

/**
 * Lifecycle contract for a configurable Furniture module.
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
