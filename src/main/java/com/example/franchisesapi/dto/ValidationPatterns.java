package com.example.franchisesapi.dto;

/**
 * Expresiones regulares compartidas por las validaciones de los DTOs de entrada.
 */
public final class ValidationPatterns {

    /** Dígitos, espacios, guiones y paréntesis, con prefijo internacional opcional. */
    public static final String PHONE = "^\\+?[0-9 ()-]{7,20}$";
    public static final String PHONE_MESSAGE = "must be a valid phone number (7 to 20 digits, spaces, hyphens or parentheses)";

    /** NIT, RUT, ... : letras, dígitos, puntos y guiones. */
    public static final String TAX_ID = "^[0-9A-Za-z.-]{5,30}$";
    public static final String TAX_ID_MESSAGE = "must contain 5 to 30 letters, digits, dots or hyphens";

    public static final String WEBSITE = "^https?://\\S+$";
    public static final String WEBSITE_MESSAGE = "must be a valid http or https URL";

    /** Códigos de sucursal y SKU de producto: letras, dígitos y guiones. */
    public static final String CODE = "^[A-Za-z0-9-]{2,30}$";
    public static final String CODE_MESSAGE = "must contain 2 to 30 letters, digits or hyphens";

    /** Código de moneda ISO 4217. */
    public static final String CURRENCY = "^[A-Za-z]{3}$";
    public static final String CURRENCY_MESSAGE = "must be a 3-letter ISO 4217 currency code";

    private ValidationPatterns() {
    }

}
