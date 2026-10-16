package com.alumni.alumni_management_system.security;

import java.io.IOException;
import java.util.Map;

import com.alumni.alumni_management_system.dto.AuthUserResponse;

import jakarta.servlet.http.HttpServletResponse;

public final class JsonResponses {

    private JsonResponses() {
    }

    public static void writeError(HttpServletResponse response, int status, String error,
                                  String message, Map<String, String> fieldErrors) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\":").append(status)
          .append(",\"error\":").append(quote(error))
          .append(",\"message\":").append(quote(message))
          .append(",\"fieldErrors\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : fieldErrors.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
        }
        sb.append("}}");
        write(response, status, sb.toString());
    }

    public static void writeUser(HttpServletResponse response, AuthUserResponse user) throws IOException {
        String body = "{\"id\":" + quote(user.id())
                + ",\"email\":" + quote(user.email())
                + ",\"fullName\":" + quote(user.fullName())
                + ",\"role\":" + quote(user.role())
                + ",\"redirect\":" + quote(user.redirect()) + "}";
        write(response, HttpServletResponse.SC_OK, body);
    }

    private static void write(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(body);
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}