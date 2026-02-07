package uk.co.xfour.spacetravel.modules;

/**
 * Lifecycle contract for a configurable SpaceTravel module.
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
