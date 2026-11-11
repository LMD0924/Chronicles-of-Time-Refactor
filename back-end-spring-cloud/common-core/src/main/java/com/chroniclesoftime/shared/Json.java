package com.chroniclesoftime.shared;

import java.util.Map;

/** 极简 JSON 工具：JWT payload 只用到扁平 Map，避免引入第三方序列化依赖。 */
public final class Json {
    private Json() {}

    public static String write(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append('"').append(escape(String.valueOf(v))).append('"');
        }
        return sb.append('}').toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> read(String json) {
        // 仅解析扁平对象（字符串 / 数字 / 布尔），足够承载 JWT payload。
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        String s = json.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) return out;
        s = s.substring(1, s.length() - 1);
        boolean inStr = false, esc = false;
        StringBuilder buf = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (char c : s.toCharArray()) {
            if (esc) { buf.append(c); esc = false; continue; }
            if (c == '\\') { buf.append(c); esc = true; continue; }
            if (c == '"') { inStr = !inStr; buf.append(c); continue; }
            if (c == ',' && !inStr) { parts.add(buf.toString()); buf.setLength(0); continue; }
            buf.append(c);
        }
        if (buf.length() > 0) parts.add(buf.toString());
        for (String part : parts) {
            int colon = part.indexOf(':');
            if (colon < 0) continue;
            String k = unquote(part.substring(0, colon).trim());
            String raw = part.substring(colon + 1).trim();
            Object v;
            if (raw.startsWith("\"") && raw.endsWith("\"")) v = unquote(raw);
            else if ("true".equals(raw) || "false".equals(raw)) v = Boolean.valueOf(raw);
            else if ("null".equals(raw)) v = null;
            else { try { v = Long.valueOf(raw); } catch (NumberFormatException ex) { v = raw; } }
            out.put(k, v);
        }
        return out;
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return escapeReverse(s.substring(1, s.length() - 1));
        }
        return s;
    }
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
    private static String escapeReverse(String s) {
        return s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }
}
