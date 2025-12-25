
package com.nt;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.time.ZoneId;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "WishSrv", urlPatterns = {"/wish"})
public class WishSrv extends HttpServlet {

    private String greetingFor(LocalTime now) {
        int h = now.getHour();
        if (h >= 5 && h < 12) return "Good Morning";
        if (h >= 12 && h < 17) return "Good Afternoon";
        if (h >= 17 && h < 22) return "Good Evening";
        return "Good Night";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // If asked for JSON greeting (server time), return JSON only and stop.
        String action = req.getParameter("action");
        if ("greet".equalsIgnoreCase(action)) {
            LocalTime now = LocalTime.now(ZoneId.systemDefault());
            String greeting = greetingFor(now);
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write("{\"message\":\"" + greeting + "\"}");
            return;
        }

        // Otherwise render a BLANK page (no greeting text yet)
        resp.setContentType("text/html; charset=UTF-8");

        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html><head><meta charset='utf-8'><title>Wish</title>");
            out.println("<style>");
            out.println("  body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans}");
            out.println("  #homeBtn{margin:12px 0;padding:8px 12px}");
            out.println("  #homeSection{margin-top:12px;color:#374151}");
            out.println("  #dbg{position:fixed;bottom:6px;left:6px;background:#ef4444;color:#fff;font-size:12px;padding:6px 10px;border-radius:6px;display:none}");
            out.println("</style></head><body>");

            // NO greeting printed here on initial load — page is effectively blank.
            out.println("<button id='homeBtn' onclick='showHome()' style='display:none;'>Home</button>");
            out.println("<div id='dbg'>Inline JS ran</div>");

            // Home handler (revealed after chatbot triggers it)
            out.println("<script>");
            out.println("function showHome(){");
            out.println("  console.log('showHome() called');");
            out.println("  var el=document.getElementById('homeSection');");
            out.println("  if(!el){ el=document.createElement('div'); el.id='homeSection'; document.body.appendChild(el); }");
            out.println("  // We do NOT set greeting here. The chatbot will fetch JSON and update the page.");
            out.println("  el.textContent='Home view rendered (no page reload).';");
            out.println("}");
            out.println("</script>");

            // ===== Inline chatbot (plain JS; fetches /wish?action=greet on "hello") =====
            out.println("<script>");
            out.println("(function(){");
            out.println("  try {");
            out.println("    console.log('Inline chatbot bootstrap');");
            out.println("    var dbg=document.getElementById('dbg'); if(dbg) dbg.style.display='inline-block';");

            out.println("    var dock=document.createElement('div');");
            out.println("    dock.id='chatbotDock';");
            out.println("    dock.style.position='fixed'; dock.style.bottom='20px'; dock.style.right='20px';");
            out.println("    dock.style.width='280px'; dock.style.background='#1f2937'; dock.style.color='#fff';");
            out.println("    dock.style.borderRadius='10px'; dock.style.boxShadow='0 8px 24px rgba(0,0,0,0.2)';");
            out.println("    dock.style.fontFamily='system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans';");
            out.println("    dock.style.zIndex='9999';");

            out.println("    var html='';");
            out.println("    html += '<div style=\"padding:12px;font-weight:600;\">Assistant</div>';"); // header
            out.println("    html += '<div id=\"chatLog\" style=\"height:150px;overflow:auto;background:#111827;padding:10px;\"></div>';"); // log
            out.println("    html += '<div style=\"display:flex;gap:6px;padding:10px;\">';");
            out.println("    html += '<input id=\"chatInput\" placeholder=\"Type here...\" style=\"flex:1;border-radius:6px;border:1px solid #374151;padding:8px;background:#1f2937;color:#fff;\" />';");
            out.println("    html += '<button id=\"chatSend\" style=\"border:none;background:#10b981;color:#031014;padding:8px 10px;border-radius:6px;font-weight:600;\">Send</button>';"); // button
            out.println("    html += '</div>';");

            out.println("    dock.innerHTML = html;");
            out.println("    document.body.appendChild(dock);");

            out.println("    var log=document.getElementById('chatLog');");
            out.println("    var input=document.getElementById('chatInput');");
            out.println("    var send=document.getElementById('chatSend');");
            out.println("    var homeBtn=document.getElementById('homeBtn');");

            out.println("    function addLine(who, text){");
            out.println("      var el=document.createElement('div');");
            out.println("      el.style.margin='6px 0';");
            out.println("      el.innerHTML='<span style=\"color:#9ca3af\">'+who+':</span> '+text;");
            out.println("      log.appendChild(el);");
            out.println("      log.scrollTop=log.scrollHeight;");
            out.println("    }");

            out.println("    function setGreetingOnPage(text){");
            out.println("      var el=document.getElementById('homeSection');");
            out.println("      if(!el){ el=document.createElement('div'); el.id='homeSection'; document.body.appendChild(el); }");
            out.println("      el.textContent = text;");
            out.println("    }");

            out.println("    function handleMessage(msg){");
            out.println("      addLine('You', msg);");
            out.println("      msg=(msg||'').trim().toLowerCase();");
            out.println("      if(msg==='hello'){");
            out.println("        // Reveal Home button (optional) and trigger its logic");
            out.println("        if(homeBtn){ homeBtn.style.display='inline-block'; if(typeof homeBtn.click==='function'){ homeBtn.click(); } }");

            out.println("        // Fetch server-time greeting via JSON");
            out.println("        fetch('./wish?action=greet')");
            out.println("          .then(function(r){ if(!r.ok) throw new Error('HTTP '+r.status); return r.json(); })");
            out.println("          .then(function(data){");
            out.println("            var g = data && data.message ? data.message : 'Hello';");
            out.println("            addLine('Bot', g);");
            out.println("            setGreetingOnPage(g);"); // also show on the page after the chat
            out.println("          })");
            out.println("          .catch(function(e){");
            out.println("            console.warn('greet failed:', e);");
            out.println("            addLine('Bot','(fallback) Unable to fetch server greeting.');");
            out.println("          });");
            out.println("        return;");
            out.println("      }");
            out.println("      addLine('Bot','Type \"hello\" to see your greeting.');");
            out.println("    }");

            out.println("    if(send){");
            out.println("      send.addEventListener('click', function(){");
            out.println("        var msg=input ? input.value : ''; if(input) input.value='';");
            out.println("        handleMessage(msg);");
            out.println("      });");
            out.println("    }");

            out.println("    if(input){");
            out.println("      input.addEventListener('keydown', function(e){");
            out.println("        if(e && e.key==='Enter'){ if(send) send.click(); }");
            out.println("      });");
            out.println("    }");

            out.println("    console.log('Inline chatbot injected');");
            out.println("  } catch(e) { console.error('Inline chatbot error:', e); }");
                       out.println("})();");
            out.println("</script>");

            out.println("</body></html>");
        }
    }
}