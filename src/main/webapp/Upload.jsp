<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <title>SampleWishApp - AI Test Runner</title>
    <style>
        body { font-family: Arial; background: #f0f4f8; text-align: center; margin-top: 80px; }
        .box { background: white; padding: 40px; width: 500px; margin: auto;
               border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
        h2 { color: #2c3e50; }
        p  { color: #666; font-size: 14px; }
        input[type=file] { margin: 20px 0; width: 100%; padding: 8px; }
        .btn { background: #2980b9; color: white; padding: 12px 30px;
               border: none; border-radius: 6px; font-size: 15px; cursor: pointer; }
        .btn:hover { background: #1a5276; }
        .success { color: green; font-weight: bold; margin-top: 15px; }
        .error   { color: red; margin-top: 15px; }
        .dl-btn  { display: inline-block; margin-top: 15px; background: #27ae60;
                   color: white; padding: 10px 22px; border-radius: 6px;
                   text-decoration: none; font-size: 14px; }
    </style>
</head>
<body>
    <div class="box">
        <h2>🤖 AI Selenium Test Runner</h2>
        <p>Upload your <b>TestData.xlsx</b> file.<br/>
           The system will execute Selenium automation and return results.</p>

        <form action="TestRunnerServlet" method="post" enctype="multipart/form-data">
            <input type="file" name="testDataFile" accept=".xlsx" required />
            <br/>
            <button type="submit" class="btn">▶ Upload &amp; Run Tests</button>
        </form>

        <% if (request.getAttribute("message") != null) { %>
            <p class="success"><%= request.getAttribute("message") %></p>
        <% } %>

        <% if (request.getAttribute("error") != null) { %>
            <p class="error">❌ <%= request.getAttribute("error") %></p>
        <% } %>

        <% if (request.getAttribute("downloadLink") != null) { %>
            <a class="dl-btn" href="<%= request.getAttribute("downloadLink") %>">
                📥 Download TestResults.xlsx
            </a>
        <% } %>
    </div>
<script src="/SampleWishApp/js/chatbot.js"></script>
</body>
</html>
