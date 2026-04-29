package aidetector.ratelimiter.utils;

import org.slf4j.MDC;

/**
 * Classe utilitaire pour ajouter du contexte structuré aux logs via MDC (Mapped Diagnostic Context).
 * Les champs ajoutés ici seront inclus dans les logs JSON et collectés par Loki.
 */
public class LogContext {
    
    public static final String EVENT_TYPE = "event_type";
    public static final String SOURCE_IP = "source_ip";
    public static final String USER_ID = "user_id";
    public static final String STATUS = "status";
    public static final String MESSAGE_DETAIL = "detail";
    public static final String REDIS_KEY = "redis_key";
    public static final String COUNTER_VALUE = "counter_value";
    public static final String LIMIT_REACHED = "limit_reached";
    public static final String RATE_LIMIT = "rate_limit";
    public static final String TOKEN_COUNT = "token_count";
    public static final String ATTEMPTS = "attempts";
    public static final String EXCEPTION_MSG = "exception_message";
    public static final String TOKEN_SIGNATURE = "token_signature";
    public static final String PUBLIC_KEY_ID = "public_key_id";
    public static final String TARGET_URI = "target_uri";
    public static final String ROUTE_ID = "route_id";
    public static final String MALFORMED_IP = "MALFORMED_IP";

    // Event Types

    public static final String EVENT_TOKEN_CHECK = "EVENT_TOKEN_CHECK";
    public static final String EVENT_RATE_LIMIT_CHECK = "RATE_LIMIT_CHECK";
    public static final String EVENT_ATTEMPTS_COUNT_MODIFIED = "ATTEMPTS_COUNT_MODIFIED";
    public static final String EVENT_IP_VALIDATION = "IP_VALIDATION";
    public static final String EVENT_TOKEN_COUNT_MODIFIED = "TOKEN_COUNT_MODIFIED";
    public static final String EVENT_PUBLIC_KEY_RETREIVAL = "PUBLIC_KEY_RETREIVAL";
    public static final String EVENT_PROXY_FORWARD = "PROXY_FORWARD";

    /**
     * Initialise le contexte MDC pour un événement donné
     */
    public static void setEventContext(String eventType, String sourceIp, String userId) {
        MDC.put(EVENT_TYPE, eventType);
        if (sourceIp != null) MDC.put(SOURCE_IP, sourceIp);
        if (userId != null) MDC.put(USER_ID, userId);
    }

    /**
     * Ajoute un détail additionnel au contexte
     */
    public static void addDetail(String key, Object value) {
        if (value != null) MDC.put(key, String.valueOf(value));
    }

    /**
     * Nettoie le contexte MDC
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Nettoie les champs temporaires tout en gardant le contexte principal
     */
    public static void clearTemporary() {
        MDC.remove(MESSAGE_DETAIL);
        MDC.remove(REDIS_KEY);
        MDC.remove(COUNTER_VALUE);
        MDC.remove(EXCEPTION_MSG);
    }
}
