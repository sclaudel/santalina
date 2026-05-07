package org.santalina.diving.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Rate limiter simple en mémoire par clé (ex. adresse IP ou email).
 * Utilise une fenêtre glissante : si plus de {@code maxAttempts} appels
 * sont faits dans {@code windowSeconds} secondes, une exception 429 est levée.
 * Les anciennes entrées sont purgées périodiquement à chaque appel.
 */
@ApplicationScoped
public class RateLimiter {

    private static final Logger LOG = Logger.getLogger(RateLimiter.class);

    /** Nb max de tentatives par fenêtre pour les endpoints d'authentification (login, reset) */
    public static final int  AUTH_MAX_ATTEMPTS  = 10;
    public static final long AUTH_WINDOW_SECONDS = 60;

    /** Nb max de tentatives par fenêtre pour l'inscription */
    public static final int  REGISTER_MAX_ATTEMPTS  = 5;
    public static final long REGISTER_WINDOW_SECONDS = 300;

    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    /**
     * Vérifie si la clé a dépassé le seuil autorisé.
     *
     * @param key            clé d'identification (ex. "login:<ip>")
     * @param maxAttempts    nombre max de tentatives dans la fenêtre
     * @param windowSeconds  taille de la fenêtre glissante en secondes
     * @throws TooManyRequestsException si le seuil est dépassé
     */
    public void check(String key, int maxAttempts, long windowSeconds) {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        Deque<Instant> timestamps = buckets.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Purger les entrées expirées
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxAttempts) {
            LOG.warnf("Rate limit dépassé pour la clé : %s (%d tentatives en %ds)", key, timestamps.size(), windowSeconds);
            throw new WebApplicationException(
                    Response.status(429).entity(java.util.Map.of("status", 429,
                            "message", "Trop de tentatives. Réessayez dans quelques instants.")).build());
        }

        timestamps.addLast(Instant.now());
    }
}
