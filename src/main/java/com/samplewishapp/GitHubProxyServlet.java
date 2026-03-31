package com.samplewishapp;


import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@WebServlet("/GitHubProxyServlet")
@MultipartConfig(maxFileSize = 10485760, maxRequestSize = 10485760)
public class GitHubProxyServlet extends HttpServlet {

    private static final Logger log =
        Logger.getLogger(GitHubProxyServlet.class.getName());

    // ── CONFIG ────────────────────────────────────────────────────────
    private static final String OWNER    = "meetparag81";
    private static final String REPO     = "SampleWishApp";
    private static final String WORKFLOW = "tests.yml";
    private static final String BRANCH   = "master";

    // GH_TOKEN from environment variable — never hardcoded
    private String getToken() {
        String t = System.getenv("GH_TOKEN");
        return (t != null && !t.isEmpty()) ? t : "";
    }

    // ════════════════════════════════════════════════════════════════
    // POST → commitFile | trigger
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        String action = req.getParameter("action");
        log.info("GitHubProxyServlet POST → action: " + action);

        switch (action != null ? action : "") {

            case "commitFile":
                // Step 1: Receive TestData.xlsx from chatbot
                // and commit it to GitHub repo
                commitTestData(req, resp);
                break;

            case "trigger":
                // Step 2: Trigger GitHub Actions pipeline
                triggerWorkflow(resp);
                break;

            default:
                resp.setStatus(400);
                resp.getWriter().write(
                    "{\"error\":\"Unknown POST action: " + action + "\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // GET → latestRun | status | artifacts
    // ════════════════════════════════════════════════════════════════
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");

        String action = req.getParameter("action");
        String runId  = req.getParameter("runId");
        log.info("GitHubProxyServlet GET → action: " + action
            + " runId: " + runId);

        switch (action != null ? action : "") {

            case "latestRun":
                // Step 3: Get latest pipeline run ID
                getLatestRun(resp);
                break;

            case "status":
                // Step 4: Poll run status (queued/in_progress/completed)
                getRunStatus(runId, resp);
                break;

            case "artifacts":
                // Step 5: Get artifact download info
                getArtifacts(runId, resp);
                break;

            default:
                resp.setStatus(400);
                resp.getWriter().write(
                    "{\"error\":\"Unknown GET action: " + action + "\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // STEP 1: Commit TestData.xlsx to GitHub repo
    // ════════════════════════════════════════════════════════════════
    private void commitTestData(HttpServletRequest req,
                                 HttpServletResponse resp)
            throws IOException, ServletException {

        log.info("commitTestData: reading uploaded file...");

        // Read file bytes from multipart upload
        Part filePart = req.getPart("testDataFile");
        if (filePart == null) {
            resp.setStatus(400);
            resp.getWriter().write(
                "{\"error\":\"No file received. " +
                "Field name must be testDataFile\"}");
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = filePart.getInputStream()) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
        }
        log.info("commitTestData: file size = " + baos.size() + " bytes");

        // Base64 encode — GitHub Contents API requires this
        String encoded = java.util.Base64.getEncoder()
                             .encodeToString(baos.toByteArray());

        // ── Get current file SHA (required for update, not create) ──
        String contentsUrl = String.format(
            "https://api.github.com/repos/%s/%s/contents/TestData.xlsx",
            OWNER, REPO);

        StringBuilder shaResult = new StringBuilder();
        int shaCode = callGitHub(contentsUrl, "GET", null, shaResult);
        String sha  = (shaCode == 200) ? extractSha(shaResult.toString()) : null;
        log.info("commitTestData: existing SHA = " + sha);

        // ── Build PUT body ───────────────────────────────────────────
        String commitBody;
        if (sha != null) {
            // Update existing file
            commitBody = "{\"message\":\"Update TestData.xlsx from chatbot\"," +
                         "\"content\":\"" + encoded + "\"," +
                         "\"sha\":\"" + sha + "\"}";
        } else {
            // Create new file
            commitBody = "{\"message\":\"Add TestData.xlsx from chatbot\"," +
                         "\"content\":\"" + encoded + "\"}";
        }

        // ── Commit to GitHub ─────────────────────────────────────────
        StringBuilder putResult = new StringBuilder();
        int putCode = callGitHub(contentsUrl, "PUT", commitBody, putResult);
        log.info("commitTestData: PUT response code = " + putCode);

        if (putCode == 200 || putCode == 201) {
            resp.setStatus(200);
            resp.getWriter().write(
                "{\"status\":\"committed\"," +
                "\"message\":\"TestData.xlsx committed to GitHub repo!\"}");
        } else {
            resp.setStatus(500);
            resp.getWriter().write(
                "{\"error\":\"Commit failed. HTTP " + putCode +
                ". Response: " + putResult.toString()
                    .replace("\"", "'") + "\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // STEP 2: Trigger workflow_dispatch
    // ════════════════════════════════════════════════════════════════
    private void triggerWorkflow(HttpServletResponse resp)
            throws IOException {

        String apiUrl = String.format(
            "https://api.github.com/repos/%s/%s/actions/workflows/%s/dispatches",
            OWNER, REPO, WORKFLOW);

        String body = "{\"ref\":\"" + BRANCH + "\"}";
        log.info("triggerWorkflow: POST to " + apiUrl);

        int code = callGitHub(apiUrl, "POST", body, null);
        log.info("triggerWorkflow: response code = " + code);

        // GitHub returns 204 No Content on success
        if (code == 204) {
            resp.setStatus(200);
            resp.getWriter().write(
                "{\"status\":\"triggered\"," +
                "\"message\":\"GitHub Actions pipeline triggered!\"}");
        } else {
            resp.setStatus(500);
            resp.getWriter().write(
                "{\"error\":\"Trigger failed. HTTP " + code +
                ". Check GH_TOKEN has workflow scope.\"}");
        }
    }

    // ════════════════════════════════════════════════════════════════
    // STEP 3: Get latest run ID
    // ════════════════════════════════════════════════════════════════
    private void getLatestRun(HttpServletResponse resp)
            throws IOException {

        String apiUrl = String.format(
            "https://api.github.com/repos/%s/%s/actions/workflows/%s/runs" +
            "?per_page=1&branch=%s",
            OWNER, REPO, WORKFLOW, BRANCH);

        StringBuilder result = new StringBuilder();
        callGitHub(apiUrl, "GET", null, result);
        resp.getWriter().write(result.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // STEP 4: Poll run status
    // ════════════════════════════════════════════════════════════════
    private void getRunStatus(String runId, HttpServletResponse resp)
            throws IOException {

        if (runId == null || runId.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"runId is required\"}");
            return;
        }

        String apiUrl = String.format(
            "https://api.github.com/repos/%s/%s/actions/runs/%s",
            OWNER, REPO, runId);

        StringBuilder result = new StringBuilder();
        callGitHub(apiUrl, "GET", null, result);
        resp.getWriter().write(result.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // STEP 5: Get artifacts for completed run
    // ════════════════════════════════════════════════════════════════
    private void getArtifacts(String runId, HttpServletResponse resp)
            throws IOException {

        if (runId == null || runId.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\":\"runId is required\"}");
            return;
        }

        String apiUrl = String.format(
            "https://api.github.com/repos/%s/%s/actions/runs/%s/artifacts",
            OWNER, REPO, runId);

        StringBuilder result = new StringBuilder();
        callGitHub(apiUrl, "GET", null, result);
        resp.getWriter().write(result.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // GitHub API HTTP Helper
    // ════════════════════════════════════════════════════════════════
    private int callGitHub(String apiUrl, String method,
                           String body, StringBuilder responseOut)
            throws IOException {

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + getToken());
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int code = conn.getResponseCode();

        if (responseOut != null) {
            InputStream is = (code < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null)
                        responseOut.append(line);
                }
            }
        }

        conn.disconnect();
        return code;
    }

    // ════════════════════════════════════════════════════════════════
    // Extract SHA from GitHub Contents API JSON response
    // ════════════════════════════════════════════════════════════════
    private String extractSha(String json) {
        int i = json.indexOf("\"sha\":");
        if (i == -1) return null;
        int start = json.indexOf("\"", i + 6) + 1;
        int end   = json.indexOf("\"", start);
        return (start > 0 && end > start)
            ? json.substring(start, end) : null;
    }
}




