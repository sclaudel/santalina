package org.santalina.diving.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.santalina.diving.security.NameUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour {@link NameUtil#capitalize(String)}.
 */
class NameUtilTest {

    @Test
    void capitalize_shouldReturnNull_whenInputIsNull() {
        assertNull(NameUtil.capitalize(null));
    }

    @Test
    void capitalize_shouldReturnBlank_whenInputIsBlank() {
        assertEquals("", NameUtil.capitalize("   ").trim());
    }

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @CsvSource({
        "marie,        Marie",
        "MARIE,        Marie",
        "Marie,        Marie",
        "jean,         Jean",
        "JEAN-PAUL,    Jean-Paul",
        "jean-paul,    Jean-Paul",
        "anne marie,   Anne Marie",
        "ANNE MARIE,   Anne Marie",
        "jean luc,     Jean Luc",
        "JEAN LUC,     Jean Luc",
        "mARIE-pIERRE, Marie-Pierre",
    })
    void capitalize_shouldCapitalizeFirstLettersCorrectly(String input, String expected) {
        assertEquals(expected.strip(), NameUtil.capitalize(input.strip()));
    }

    @Test
    void capitalize_shouldHandleSingleLetter() {
        assertEquals("A", NameUtil.capitalize("a"));
        assertEquals("Z", NameUtil.capitalize("Z"));
    }

    @Test
    void capitalize_shouldPreserveHyphenAsIs() {
        String result = NameUtil.capitalize("jean-luc");
        assertTrue(result.contains("-"), "Le tiret doit être conservé");
        assertEquals("Jean-Luc", result);
    }

    @Test
    void capitalize_shouldTrimLeadingAndTrailingSpaces() {
        assertEquals("Marie", NameUtil.capitalize("  marie  "));
    }
}
