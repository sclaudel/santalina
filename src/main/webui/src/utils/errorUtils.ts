/**
 * Extrait un message d'erreur lisible depuis une réponse d'erreur axios.
 * Gère le format standard Quarkus { violations: [...] } ainsi que { message: "..." }.
 */
export function getErrorMessage(err: unknown): string {
  const e = err as {
    response?: {
      data?: {
        message?: string;
        violations?: Array<{ message?: string }>;
      };
    };
    message?: string;
  };

  // Format personnalisé : { message: "..." }
  if (e?.response?.data?.message) {
    return e.response.data.message;
  }

  // Format Quarkus par défaut : { violations: [{ message: "..." }] }
  const violations = e?.response?.data?.violations;
  if (Array.isArray(violations) && violations.length > 0) {
    const msgs = violations.map(v => v.message).filter((m): m is string => !!m);
    if (msgs.length > 0) return msgs.join(', ');
  }

  // Fallback sur le message JS de l'erreur
  if (e?.message) return e.message;

  return 'Une erreur est survenue';
}
