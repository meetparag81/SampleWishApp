package com.samplewishapp;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;
import java.io.*;

@MultipartConfig
@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "admin123";

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (isEmpty(username) || isEmpty(password)) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "{\"success\":false,\"message\":\"Username and password are required.\"}");
            return;
        }

        boolean valid = VALID_USERNAME.equals(username.trim())
                     && VALID_PASSWORD.equals(password.trim());

        if (valid) {
            HttpSession session = request.getSession(true);
            session.setAttribute("app_user", username.trim());
            session.setMaxInactiveInterval(30 * 60);

            sendJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"message\":\"Login successful!\"}");
        } else {
            sendJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "{\"success\":false,\"message\":\"Invalid username or password. Please try again.\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("app_user") != null) {
            sendJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"loggedIn\":true}");
        } else {
            sendJson(response, HttpServletResponse.SC_OK,
                "{\"success\":true,\"loggedIn\":false}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        sendJson(response, HttpServletResponse.SC_OK,
            "{\"success\":true,\"message\":\"Logged out.\"}");
    }

    private void sendJson(HttpServletResponse response,
                          int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private boolean isEmpty(String v) {
        return v == null || v.trim().isEmpty();
    }
}