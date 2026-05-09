package com.exampl.util;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    /**
     * Extrai os query params da URI.
     * Ex: /lutadores/1?nome=Tyson&categoria=1  →  {nome=Tyson, categoria=1}
     */
    public Map<String, String> extrairQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery(); // retorna null se não houver query string

        if (query == null || query.isBlank()) return params;

        for (String par : query.split("&")) {
            String[] partes = par.split("=", 2);
            if (partes.length == 2) {
                params.put(partes[0].trim(), partes[1].trim());
            } else if (partes.length == 1) {
                params.put(partes[0].trim(), "");
            }
        }
        return params;
    }

    /** Converte String para Integer, retorna null se inválido */
    public Integer parseIntOuNull(String valor) {
        if (valor == null) return null;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Extrai campo simples de JSON: {"campo":"valor"} */
    public String extrairCampoJson(String json, String campo) {
        if (json == null || campo == null) return null;
        String pattern = "\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }
    
}
