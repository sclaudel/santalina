package org.santalina.diving.security;

/**
 * Utilitaires de normalisation des noms / prénoms.
 */
public final class NameUtil {

    private NameUtil() { }

    /**
     * Capitalise chaque segment d'un prénom.
     * <ul>
     *   <li>La première lettre de chaque segment est mise en majuscule.</li>
     *   <li>Le reste du segment est mis en minuscule.</li>
     *   <li>Les séparateurs reconnus sont l'espace {@code ' '} et le tiret {@code '-'}.</li>
     * </ul>
     * Exemples :
     * <pre>
     *   capitalize("marie")        → "Marie"
     *   capitalize("JEAN-PAUL")    → "Jean-Paul"
     *   capitalize("anne marie")   → "Anne Marie"
     *   capitalize("JEAN luc")     → "Jean Luc"
     * </pre>
     *
     * @param name prénom brut (peut être {@code null} ou vide)
     * @return prénom capitalisé, ou la valeur d'origine si {@code null}/vide
     */
    public static String capitalize(String name) {
        if (name == null || name.isBlank()) return name;
        String trimmed = name.trim();
        StringBuilder result = new StringBuilder(trimmed.length());
        boolean capNext = true;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == ' ' || c == '-') {
                result.append(c);
                capNext = true;
            } else if (capNext) {
                result.append(Character.toUpperCase(c));
                capNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
}
