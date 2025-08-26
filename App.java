// Importações necessárias para HTTP, IO e manipulação de dados
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class App {

    static final int PORT = 8080;          // Porta do servidor
    static final String CSV = "data_tasks.csv"; // Arquivo CSV para salvar tarefas
    static final int MAX = 5000;           // Número máximo de tarefas

    // Arrays para armazenar dados das tarefas
    static String[] ids = new String[MAX];
    static String[] titulos = new String[MAX];
    static String[] descrs = new String[MAX];
    static int[] status = new int[MAX];     // 0 TODO, 1 DOING, 2 DONE
    static long[] criados = new long[MAX];  // Timestamp de criação
    static int n = 0;                       // Contador de tarefas

    public static void main(String[] args) throws Exception {
        carregar();  // Carrega tarefas do CSV

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new RootHandler());           // HTML
        server.createContext("/api/tasks", new ApiTasksHandler()); // API REST
        server.setExecutor(null);
        System.out.println("Servindo em http://localhost:" + PORT);
        server.start();
    }

    // Handler para servir a página HTML
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { 
                send(ex, 405, ""); 
                return; 
            }
            byte[] body = INDEX_HTML.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        }
    }

    // Handler da API REST
    static class ApiTasksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();

            try {
                // GET /api/tasks → lista todas tarefas
                if ("GET".equals(method) && "/api/tasks".equals(path)) {
                    sendJson(ex, 200, listarJSON());
                    return;
                }

                // POST /api/tasks → cria uma nova tarefa
                if ("POST".equals(method) && "/api/tasks".equals(path)) {
                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String titulo = jsonGet(body, "titulo");
                    String descricao = jsonGet(body, "descricao");
                    if (titulo == null || titulo.isBlank()) {
                        sendJson(ex, 400, "{\"error\":\"titulo obrigatório\"}");
                        return;
                    }
                    Map<String, Object> t = criar(titulo, descricao == null ? "" : descricao);
                    salvar();
                    sendJson(ex, 200, toJsonTask(t));
                    return;
                }

                // PATCH /api/tasks/{id}/status → atualiza status
                if ("PATCH".equals(method) && path.startsWith("/api/tasks/") && path.endsWith("/status")) {
                    String id = path.substring("/api/tasks/".length(), path.length() - "/status".length());
                    String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String stStr = jsonGet(body, "status");
                    if (stStr == null) { sendJson(ex, 400, "{\"error\":\"status ausente\"}"); return; }
                    int st = clampStatus(parseIntSafe(stStr, 0));
                    int i = findIdxById(id);
                    if (i < 0) { sendJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                    status[i] = st;
                    salvar();
                    sendJson(ex, 200, toJsonTask(mapOf(i)));
                    return;
                }

                // DELETE /api/tasks/{id} → remove tarefa
                if ("DELETE".equals(method) && path.startsWith("/api/tasks/")) {
                    String id = path.substring("/api/tasks/".length());
                    int i = findIdxById(id);
                    if (i < 0) { sendJson(ex, 404, "{\"error\":\"not found\"}"); return; }
                    for (int k = i; k < n - 1; k++) { // shift
                        ids[k] = ids[k+1]; titulos[k] = titulos[k+1]; descrs[k] = descrs[k+1];
                        status[k] = status[k+1]; criados[k] = criados[k+1];
                    }
                    n--;
                    salvar();
                    sendJson(ex, 204, "");
                    return;
                }

                send(ex, 404, "");
            } catch (Exception e) {
                e.printStackTrace();
                sendJson(ex, 500, "{\"error\":\"server\"}");
            }
        }
    }

    // Funções auxiliares: criar, listar, salvar, JSON etc.
    // Exemplo: carregar tarefas do CSV
    static void carregar() { /* ... */ }
    static void salvar() { /* ... */ }
    static Map<String, Object> criar(String titulo, String descr) { /* ... */ }
    static int findIdxById(String id){ /* ... */ return -1; }
    static Map<String,Object> mapOf(int i){ /* ... */ return null; }
    static String listarJSON(){ /* ... */ return null; }
    static String toJsonTask(Map<String,Object> t){ /* ... */ return null; }
    static String jsonGet(String body, String key){ /* ... */ return null; }
    static void send(HttpExchange ex, int code, String body) throws IOException { /* ... */ }
    static void sendJson(HttpExchange ex, int code, String body) throws IOException { /* ... */ }
    static int clampStatus(int s){ return Math.max(0, Math.min(2, s)); }
    static int parseIntSafe(String s, int def){ try { return Integer.parseInt(s.trim()); } catch(Exception e){ return def; } }
    static long parseLongSafe(String s, long def){ try { return Long.parseLong(s.trim()); } catch(Exception e){ return def; } }
}
