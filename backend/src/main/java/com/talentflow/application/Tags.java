package com.talentflow.application;

import com.talentflow.common.BadRequestException;

import java.util.Locale;
import java.util.regex.Pattern;

final class Tags {

    private static final Pattern VALID = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N} ._+#-]{0,49}$");

    private Tags() {
    }

    /** Minuscules, espaces compactés : "  Spring   Boot " devient "spring boot". */
    static String normalize(String raw) {
        return raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String normalizeAndValidate(String raw) {
        String tag = normalize(raw);
        if (!VALID.matcher(tag).matches()) {
            throw new BadRequestException(
                    "Invalid tag: use letters, digits, spaces and . _ + # - (50 characters max)");
        }
        return tag;
    }
}
