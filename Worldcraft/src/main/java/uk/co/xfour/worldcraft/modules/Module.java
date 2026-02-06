package uk.co.xfour.worldcraft.modules;

/**
 * Lifecycle contract for a configurable Worldcraft module.
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
