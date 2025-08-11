package com.mcp.server.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio cliente para interactuar con la API REST de Azure DevOps.
 * Centraliza manejo de credenciales (AZURE_DEVOPS_ORGANIZATION, AZURE_DEVOPS_PAT)
 * y soporta hosts dev.azure.com y app.vssps.visualstudio.com.
 */
@Service
public class AzureDevOpsClientService {

    private final WebClient webClient;
    private final String apiVersion;
    private final String vsspsApiVersion;

    public AzureDevOpsClientService(
            WebClient.Builder webClientBuilder,
            // Permite leer de ENV o de properties
            @Value("${AZURE_DEVOPS_ORGANIZATION:${azure.devops.organization:test}}") String organization,
            @Value("${AZURE_DEVOPS_PAT:${azure.devops.pat:}}") String pat,
            @Value("${AZURE_DEVOPS_API_VERSION:${azure.devops.api-version:7.2-preview.1}}") String apiVersion,
            @Value("${AZURE_DEVOPS_VSSPS_API_VERSION:${azure.devops.vssps-api-version:7.1}}") String vsspsApiVersion
    ) {
        this.apiVersion = apiVersion;
        this.vsspsApiVersion = vsspsApiVersion;
        String credentials = ":" + pat;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        this.webClient = webClientBuilder
            .baseUrl("https://dev.azure.com/" + organization)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public Map<String, Object> getWorkApi(String project, String team, String path) {
        // Construcción segura de segmentos con encoding adecuado
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();

        List<String> segments = new ArrayList<>();
        segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("work");
        // descomponer el path solicitado en segmentos seguros
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> body = webClient.get()
            .uri(builder -> builder
                .pathSegment(segments.toArray(new String[0]))
                .queryParam("api-version", apiVersion)
                .build()
            )
            .exchangeToMono(resp -> {
                if (resp.statusCode().isError()) {
                    return resp.bodyToMono(Map.class).defaultIfEmpty(Map.of()).map(b -> {
                        Map<String,Object> m = new HashMap<>();
                        if (b != null) m.putAll(b);
                        m.put("isHttpError", true);
                        m.put("httpStatus", resp.statusCode().value());
                        m.put("httpReason", resp.statusCode().toString());
                        return m;
                    });
                }
                return resp.bodyToMono(Map.class);
            })
            .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
            .block();
        return body != null ? body : new HashMap<>();
    }

    /**
     * Llamadas al área Core (Projects, Teams, etc.) bajo /_apis/{path}
     */
    public Map<String,Object> getCoreApi(String path, Map<String,String> query) {
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return doGetWithSegmentsAndQuery(segments, query, apiVersion);
    }

    private Map<String,Object> doGetWithSegmentsAndQuery(List<String> segments, Map<String,String> query, String defaultApiVersion) {
        @SuppressWarnings("unchecked")
        Map<String,Object> body = webClient.get()
            .uri(builder -> {
                var b = builder.pathSegment(segments.toArray(new String[0]));
                boolean hasApiVersion = false;
                if (query != null) {
                    for (Map.Entry<String,String> e : query.entrySet()) {
                        b.queryParam(e.getKey(), e.getValue());
                        if ("api-version".equalsIgnoreCase(e.getKey())) {
                            hasApiVersion = true;
                        }
                    }
                }
                if (!hasApiVersion && defaultApiVersion != null && !defaultApiVersion.isBlank()) {
                    b.queryParam("api-version", defaultApiVersion);
                }
                return b.build();
            })
            .exchangeToMono(resp -> {
                if (resp.statusCode().isError()) {
                    return resp.bodyToMono(Map.class).defaultIfEmpty(Map.of()).map(b -> {
                        Map<String,Object> m = new HashMap<>();
                        if (b != null) m.putAll(b);
                        m.put("isHttpError", true);
                        m.put("httpStatus", resp.statusCode().value());
                        m.put("httpReason", resp.statusCode().toString());
                        return m;
                    });
                }
                return resp.bodyToMono(Map.class);
            })
            .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
            .block();
        return body != null ? body : new HashMap<>();
    }

    private Map<String,Object> doExchangeWithSegments(HttpMethod method, List<String> segments, Map<String,String> query,
                                                      Object body, String apiVersionOverride, MediaType contentType) {
        @SuppressWarnings("unchecked")
        Map<String,Object> resp = webClient.method(method)
            .uri(builder -> {
                var b = builder.pathSegment(segments.toArray(new String[0]));
                if (query != null) {
                    for (Map.Entry<String,String> e : query.entrySet()) {
                        b.queryParam(e.getKey(), e.getValue());
                    }
                }
                String ver = (apiVersionOverride != null && !apiVersionOverride.isBlank()) ? apiVersionOverride : this.apiVersion;
                b.queryParam("api-version", ver);
                return b.build();
            })
            .headers(h -> { if (contentType != null) h.set(HttpHeaders.CONTENT_TYPE, contentType.toString()); })
            .body(body != null ? BodyInserters.fromValue(body) : BodyInserters.empty())
            .exchangeToMono(r -> {
                if (r.statusCode().isError()) {
                    return r.bodyToMono(Map.class).defaultIfEmpty(Map.of()).map(b -> {
                        Map<String,Object> m = new HashMap<>();
                        if (b != null) m.putAll(b);
                        m.put("isHttpError", true);
                        m.put("httpStatus", r.statusCode().value());
                        m.put("httpReason", r.statusCode().toString());
                        return m;
                    });
                }
                return r.bodyToMono(Map.class);
            })
            .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
            .block();
        return resp != null ? resp : new HashMap<>();
    }

    public Map<String,Object> postCoreApi(String path, Map<String,String> query, Object body, String apiVersionOverride) {
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : (path == null ? "" : path).split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return doExchangeWithSegments(HttpMethod.POST, segments, query, body, apiVersionOverride, MediaType.APPLICATION_JSON);
    }

    public Map<String,Object> patchCoreApi(String path, Map<String,String> query, Object body, String apiVersionOverride) {
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : (path == null ? "" : path).split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return doExchangeWithSegments(HttpMethod.PATCH, segments, query, body, apiVersionOverride, MediaType.APPLICATION_JSON);
    }

    public Map<String,Object> deleteCoreApi(String path, Map<String,String> query, String apiVersionOverride) {
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : (path == null ? "" : path).split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return doExchangeWithSegments(HttpMethod.DELETE, segments, query, null, apiVersionOverride, null);
    }

    /**
     * Llamadas a VSSPS (Accounts, Profiles, Graph, etc.).
     * pathWithQuery NO debe iniciar con '/'. Ej: "accounts" o "graph/avatars/{id}".
     * Si no contiene api-version, se añade la por defecto.
     */
    public Map<String,Object> getVsspsApi(String pathWithQuery) {
        String base = "https://app.vssps.visualstudio.com/_apis/" + (pathWithQuery == null ? "" : pathWithQuery);
        boolean hasApiVersion = base.contains("api-version=");
        String url = hasApiVersion ? base : base + (base.contains("?") ? "&" : "?") + "api-version=" + vsspsApiVersion;
        @SuppressWarnings("unchecked")
        Map<String,Object> body = webClient.get()
                .uri(url) // absoluta, ignora baseUrl
                .exchangeToMono(resp -> {
                    if (resp.statusCode().isError()) {
                        return resp.bodyToMono(Map.class).defaultIfEmpty(Map.of()).map(b -> {
                            Map<String,Object> m = new HashMap<>();
                            if (b != null) m.putAll(b);
                            m.put("isHttpError", true);
                            m.put("httpStatus", resp.statusCode().value());
                            m.put("httpReason", resp.statusCode().toString());
                            return m;
                        });
                    }
                    return resp.bodyToMono(Map.class);
                })
                .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
                .block();
        return body != null ? body : new HashMap<>();
    }

    /** Descarga binaria desde VSSPS (por ejemplo Avatars). Devuelve base64 y contentType si disponible. */
    public Map<String,Object> getVsspsBinary(String pathWithQuery) {
        String base = "https://app.vssps.visualstudio.com/_apis/" + (pathWithQuery == null ? "" : pathWithQuery);
        boolean hasApiVersion = base.contains("api-version=");
        String url = hasApiVersion ? base : base + (base.contains("?") ? "&" : "?") + "api-version=" + vsspsApiVersion;
        return webClient.get()
            .uri(url)
            .exchangeToMono(resp -> resp.bodyToMono(byte[].class).map(bytes -> {
                String b64 = Base64.getEncoder().encodeToString(bytes);
                String ct = resp.headers().contentType().map(MediaType::toString).orElse(null);
                Map<String,Object> m = new HashMap<>();
                m.put("data", b64);
                if (ct != null) m.put("contentType", ct);
                return m;
            }).onErrorResume(e -> Mono.just(Map.of("error", e.getMessage()))))
            .block();
    }

    /** Sube binario a VSSPS (por ejemplo Avatars via PUT). */
    public Map<String,Object> putVsspsBinary(String pathWithQuery, byte[] data, MediaType contentType) {
        String base = "https://app.vssps.visualstudio.com/_apis/" + (pathWithQuery == null ? "" : pathWithQuery);
        boolean hasApiVersion = base.contains("api-version=");
        String url = hasApiVersion ? base : base + (base.contains("?") ? "&" : "?") + "api-version=" + vsspsApiVersion;
        @SuppressWarnings("unchecked")
        Map<String,Object> body = webClient.put()
            .uri(url)
            .header(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .bodyValue(data != null ? data : new byte[0])
            .retrieve()
            .bodyToMono(Map.class)
            .onErrorResume(e -> Mono.just(Map.of("error", e.getMessage())))
            .block();
        return body != null ? body : new HashMap<>();
    }

    /**
     * Llamadas al área WIT bajo /{project}/{team?}/_apis/wit/{path}
     */
    public Map<String, Object> getWitApi(String project, String team, String path) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();

        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return doGetWithSegmentsAndQuery(segments, null, apiVersion);
    }

    /** GET WIT con query params y override opcional de api-version */
    public Map<String,Object> getWitApiWithQuery(String project, String team, String path, Map<String,String> query, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
        Map<String,String> q = new java.util.LinkedHashMap<>();
        if (query != null) q.putAll(query);
        if (!q.containsKey("api-version")) {
            q.put("api-version", (apiVersionOverride != null && !apiVersionOverride.isBlank()) ? apiVersionOverride : this.apiVersion);
        }
        return doGetWithSegmentsAndQuery(segments, q, this.apiVersion);
    }

    public Map<String, Object> postWitApi(String project, String team, String path, Object body, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();

        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
    return doExchangeWithSegments(HttpMethod.POST, segments, null, body, apiVersionOverride, MediaType.APPLICATION_JSON);
    }

    public Map<String, Object> postWitApiWithQuery(String project, String team, String path, Map<String,String> query, Object body, String apiVersionOverride, MediaType contentType) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        java.util.List<String> segments = new java.util.ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
    return doExchangeWithSegments(HttpMethod.POST, segments, query, body, apiVersionOverride, contentType);
    }

    public Map<String, Object> putWitApi(String project, String team, String path, Object body, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
    return doExchangeWithSegments(HttpMethod.PUT, segments, null, body, apiVersionOverride, MediaType.APPLICATION_JSON);
    }

    public Map<String, Object> patchWitApi(String project, String team, String path, Object body, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
    return doExchangeWithSegments(HttpMethod.PATCH, segments, null, body, apiVersionOverride, MediaType.APPLICATION_JSON);
    }

    public Map<String, Object> patchWitApiWithQuery(String project, String team, String path, Map<String,String> query, Object body, String apiVersionOverride, MediaType contentType) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        java.util.List<String> segments = new java.util.ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
    return doExchangeWithSegments(HttpMethod.PATCH, segments, query, body, apiVersionOverride, contentType);
    }

    public Map<String, Object> deleteWitApi(String project, String team, String path, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
    return doExchangeWithSegments(HttpMethod.DELETE, segments, null, null, apiVersionOverride, null);
    }

    public Map<String, Object> deleteWitApiWithQuery(String project, String team, String path, Map<String,String> query, String apiVersionOverride) {
        String proj = project == null ? "" : project.trim();
        String tm = (team == null || team.trim().isEmpty()) ? null : team.trim();
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        if (!proj.isEmpty()) segments.add(proj);
        if (tm != null) segments.add(tm);
        segments.add("_apis");
        segments.add("wit");
        for (String part : pth.split("/")) { if (!part.isBlank()) segments.add(part); }
        return doExchangeWithSegments(HttpMethod.DELETE, segments, query, null, apiVersionOverride, null);
    }

    public Map<String,Object> postCoreBinary(String path, Map<String,String> query, byte[] data, String apiVersionOverride, MediaType contentType) {
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        MediaType ct = contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM;
        return doExchangeWithSegments(HttpMethod.POST, segments, query, data != null ? data : new byte[0], apiVersionOverride, ct);
    }

    public Map<String,Object> getCoreBinary(String path, Map<String,String> query, String apiVersionOverride) {
        String pth = path == null ? "" : path.trim();
        List<String> segments = new ArrayList<>();
        segments.add("_apis");
        for (String part : pth.split("/")) {
            if (!part.isBlank()) segments.add(part);
        }
        return webClient.get()
            .uri(builder -> {
                var b = builder.pathSegment(segments.toArray(new String[0]));
                if (query != null) {
                    for (Map.Entry<String,String> e : query.entrySet()) {
                        b.queryParam(e.getKey(), e.getValue());
                    }
                }
                String ver = (apiVersionOverride != null && !apiVersionOverride.isBlank()) ? apiVersionOverride : this.apiVersion;
                b.queryParam("api-version", ver);
                return b.build();
            })
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .exchangeToMono(resp -> resp.bodyToMono(byte[].class).map(bytes -> {
                String b64 = Base64.getEncoder().encodeToString(bytes != null ? bytes : new byte[0]);
                String ct = resp.headers().contentType().map(MediaType::toString).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                Map<String,Object> m = new HashMap<>();
                m.put("data", b64);
                m.put("contentType", ct);
                return m;
            }).onErrorResume(e -> Mono.just(Map.of("error", e.getMessage()))))
            .block();
    }

}
