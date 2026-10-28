package com.example.franchisesapi.service;

import java.util.Locale;

/**
 * Limpieza de los textos que llegan en las peticiones antes de guardarlos.
 */
final class InputNormalizer {

    private InputNormalizer() {
    }

    /** Quita los espacios de los extremos; un texto vacío se guarda como null. */
    static String text(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Para códigos, SKU, NIT y monedas: se comparan y se guardan en mayúsculas. */
    static String upper(String value) {
        String text = text(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    /** Para correos electrónicos. */
    static String lower(String value) {
        String text = text(value);
        return text == null ? null : text.toLowerCase(Locale.ROOT);
    }

}
