<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    HttpSession existingSession = request.getSession(false);
    if (existingSession != null && existingSession.getAttribute("app_user") != null) {
        response.sendRedirect("page.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>SampleWishApp – Login</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Segoe UI', Arial, sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-card {
            background: #ffffff;
            border-radius: 16px;
            padding: 48px 40px;
            width: 100%;
            max-width: 420px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .logo { text-align: center; margin-bottom: 32px; }
        .logo-icon { font-size: 48px; display: block; margin-bottom: 8px; }
        .logo h1 { font-size: 22px; font-weight: 700; color: #1a1a2e; }
        .logo p { font-size: 13px; color: #888; margin-top: 4px; }
        .form-group { margin-bottom: 20px; }
        .form-group label {
            display: block; font-size: 13px;
            font-weight: 600; color: #444; margin-bottom: 6px;
        }
        .form-group input {
            width: 100%; padding: 12px 16px;
            border: 2px solid #e8e8e8; border-radius: 8px;
            font-size: 15px; color: #333;
            transition: border-color 0.2s; outline: none;
        }
        .form-group input:focus { border-color: #0f3460; }
        .btn-login {
            width: 100%; padding: 13px;
            background: #0f3460; color: white;
            border: none; border-radius: 8px;
            font-size: 15px; font-weight: 600;
            cursor: pointer; transition: background 0.2s; margin-top: 8px;
        }
        .btn-login:hover { background: #16213e; }
        .btn-login:disabled { background: #999; cursor: not-allowed; }
        .error-msg {
            background: #fff0f0; border: 1px solid #ffcccc;
            color: #cc0000; border-radius: 8px;
            padding: 10px 14px; font-size: 13px;
            margin-bottom: 16px; display: none; text-align: center;
        }
        .spinner {
            display: none; text-align: center;
            margin-top: 12px; font-size: 13px; color: #888;
        }
    </style>
</head>
<body>
<div class="login-card">
    <div class="logo">
        <span class="logo-icon">🤖</span>
        <h1>AI Selenium Test Runner</h1>
        <p>Sign in to access the test dashboard</p>
    </div>
    <div class="error-msg" id="errorMsg"></div>
    <form id="loginForm">
        <div class="form-group">
            <label for="username">Username</label>
            <input type="text" id="username" name="username"
                   placeholder="Enter username" required autocomplete="username"/>
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input type="password" id="password" name="password"
                   placeholder="Enter password" required autocomplete="current-password"/>
        </div>
        <button type="submit" class="btn-login" id="loginBtn">🔐 Login</button>
        <div class="spinner" id="spinner">⏳ Verifying credentials...</div>
    </form>
</div>
<script>
    document.getElementById('loginForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value.trim();
        const errorMsg = document.getElementById('errorMsg');
        const spinner  = document.getElementById('spinner');
        const loginBtn = document.getElementById('loginBtn');

        errorMsg.style.display = 'none';
        loginBtn.disabled = true;
        loginBtn.textContent = '⏳ Signing in...';
        spinner.style.display = 'block';

        try {
            const formData = new FormData();
            formData.append('username', username);
            formData.append('password', password);
            const response = await fetch('LoginServlet', {
                method: 'POST',
                body: formData
            });
            const data = await response.json();
            if (data.success) {
                window.location.href = 'page.html';
            } else {
                errorMsg.textContent = '❌ ' + data.message;
                errorMsg.style.display = 'block';
                loginBtn.disabled = false;
                loginBtn.textContent = '🔐 Login';
                spinner.style.display = 'none';
            }
        } catch (err) {
            errorMsg.textContent = '⚠️ Server error. Please try again.';
            errorMsg.style.display = 'block';
            loginBtn.disabled = false;
            loginBtn.textContent = '🔐 Login';
            spinner.style.display = 'none';
        }
    });
</script>
</body>
</html>