package de.bund.zrb.webdriver.command.response;

import de.bund.zrb.markerInterfaces.WDResultData;

/**
 * Repräsentiert ein leeres Ergebnis für Befehle wie `session.end`.
 */
public class WDEmptyResult implements WDResultData {
    // Kein Inhalt, da es sich um ein leeres DTO handelt, sicherheitshalber als class anstatt als Interface, falls
    // GSON ansonsten Probleme hat, weil es ein Interface nicht instanziieren kann.
}