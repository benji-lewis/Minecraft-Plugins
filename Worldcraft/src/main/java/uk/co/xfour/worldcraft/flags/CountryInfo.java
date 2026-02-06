package uk.co.xfour.worldcraft.flags;

/**
 * Represents a country entry with an ISO code and display name.
 *
 * @param code ISO 3166-1 alpha-2 code
 * @param name display name
 */
public record CountryInfo(String code, String name) {
}
