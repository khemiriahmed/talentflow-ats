package com.talentflow.common;

import java.util.List;

/**
 * CSV compatible Excel FR : séparateur ";", fins de ligne CRLF, échappement RFC 4180.
 * Protège contre l'injection de formules (OWASP) : une cellule commençant par = + - @ est préfixée par '.
 */
public final class CsvFormat {

    public static final char DELIMITER = ';';
    public static final String BOM = "\uFEFF";

    private CsvFormat() {
    }

    public static void appendRow(StringBuilder out, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.append(DELIMITER);
            }
            out.append(cell(cells.get(i)));
        }
        out.append("\r\n");
    }

    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String text = value;
        if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        boolean mustQuote = text.indexOf(DELIMITER) >= 0 || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        if (text.indexOf('"') >= 0) {
            text = text.replace("\"", "\"\"");
        }
        return mustQuote ? "\"" + text + "\"" : text;
    }
}
