package org.santalina.diving.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 5;
    private static final long TTL_SECONDS = 300; // 5 minutes
    private static final int WIDTH = 200;
    private static final int HEIGHT = 60;

    private final ConcurrentHashMap<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    private record CaptchaEntry(String text, Instant createdAt) {}

    public record CaptchaChallenge(String id, String image) {}

    public CaptchaChallenge generate() {
        cleanExpired();
        Random rng = new Random();
        StringBuilder sb = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
        }
        String captchaText = sb.toString();
        String svg = buildSvg(captchaText, rng);
        String dataUri = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        String id = UUID.randomUUID().toString();
        store.put(id, new CaptchaEntry(captchaText, Instant.now()));
        return new CaptchaChallenge(id, dataUri);
    }

    /**
     * Vérifie la réponse au captcha. Retire l'entrée du store (usage unique).
     * Retourne false si l'id est inconnu, expiré ou si la réponse est incorrecte.
     */
    public boolean verify(String id, String answer) {
        cleanExpired();
        if (id == null || answer == null) return false;
        CaptchaEntry entry = store.remove(id);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.createdAt().plusSeconds(TTL_SECONDS))) return false;
        return entry.text().equalsIgnoreCase(answer.trim());
    }

    private void cleanExpired() {
        Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
        Iterator<Map.Entry<String, CaptchaEntry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().createdAt().isBefore(cutoff)) it.remove();
        }
    }

    private String buildSvg(String text, Random rng) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(WIDTH)
           .append("\" height=\"").append(HEIGHT).append("\">");

        // Fond
        svg.append("<rect width=\"").append(WIDTH).append("\" height=\"").append(HEIGHT)
           .append("\" fill=\"#eef2f7\" rx=\"4\"/>");

        // Lignes de bruit
        String[] noiseColors = {"#b0b8c5", "#aab2bd", "#9fa9b5"};
        for (int i = 0; i < 7; i++) {
            int x1 = rng.nextInt(WIDTH), y1 = rng.nextInt(HEIGHT);
            int x2 = rng.nextInt(WIDTH), y2 = rng.nextInt(HEIGHT);
            svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
               .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
               .append("\" stroke=\"").append(noiseColors[rng.nextInt(noiseColors.length)])
               .append("\" stroke-width=\"1\"/>");
        }

        // Lettres
        String[] letterColors = {"#1a3c78", "#7b1fa2", "#c62828", "#1b5e20", "#e65100", "#00695c"};
        int charWidth = WIDTH / (CAPTCHA_LENGTH + 1);
        for (int i = 0; i < text.length(); i++) {
            int x = charWidth / 2 + i * charWidth + rng.nextInt(10) - 5;
            int y = HEIGHT / 2 + 10 + rng.nextInt(12) - 6;
            int rotate = rng.nextInt(34) - 17;
            String color = letterColors[rng.nextInt(letterColors.length)];
            int fontSize = 24 + rng.nextInt(10);
            svg.append("<text x=\"").append(x).append("\" y=\"").append(y)
               .append("\" transform=\"rotate(").append(rotate).append(",")
               .append(x).append(",").append(y).append(")\"")
               .append(" font-family=\"Georgia,serif\" font-size=\"").append(fontSize).append("\"")
               .append(" font-weight=\"bold\" fill=\"").append(color).append("\">")
               .append(text.charAt(i)).append("</text>");
        }

        // Points de bruit
        for (int i = 0; i < 25; i++) {
            svg.append("<circle cx=\"").append(rng.nextInt(WIDTH))
               .append("\" cy=\"").append(rng.nextInt(HEIGHT))
               .append("\" r=\"1.5\" fill=\"#9fa9b5\"/>");
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
