package uk.co.xfour.worldcraft.flags;

/**
 * Describes the result of loading the country registry.
 *
 * @param success       whether loading succeeded
 * @param countryCount  number of countries loaded
 * @param message       human-readable status message
 */
public record CountryLoadResult(boolean success, int countryCount, String message) {
}
