package com.rudra.pos.util;

/**
 * Minimal, dependency-free JSON writer — just enough to serialise API
 * responses for the web layer without pulling in Jackson/Gson. Build objects
 * and arrays fluently:
 *
 * <pre>
 *   new Json.Obj()
 *       .str("name", "Milk")
 *       .num("price", 33)
 *       .end();                    // {"name":"Milk","price":33}
 * </pre>
 */
public final class Json {

    private Json() {
    }

    public static String quote(String s) {
        return '"' + escape(s) + '"';
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /** Fluent JSON object builder. */
    public static final class Obj {
        private final StringBuilder sb = new StringBuilder("{");
        private boolean first = true;

        private void comma() {
            if (!first) {
                sb.append(',');
            }
            first = false;
        }

        public Obj raw(String key, String rawJson) {
            comma();
            sb.append(quote(key)).append(':').append(rawJson);
            return this;
        }

        public Obj str(String key, String value) {
            return raw(key, value == null ? "null" : quote(value));
        }

        public Obj num(String key, Object number) {
            return raw(key, number == null ? "null" : number.toString());
        }

        public Obj bool(String key, boolean value) {
            return raw(key, Boolean.toString(value));
        }

        public String end() {
            sb.append('}');
            return sb.toString();
        }
    }

    /** Fluent JSON array builder. */
    public static final class Arr {
        private final StringBuilder sb = new StringBuilder("[");
        private boolean first = true;

        public Arr add(String rawJson) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(rawJson);
            return this;
        }

        public String end() {
            sb.append(']');
            return sb.toString();
        }
    }
}
