
(function () {
  const dock = document.createElement('div');
  dock.id = 'chatbotDock';
  Object.assign(dock.style, {
    position: 'fixed',
    bottom: '20px',
    right: '20px',
    width: '280px',
    background: '#1f2937',
    color: '#fff',
    borderRadius: '10px',
    boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
    fontFamily: 'system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Cantarell,Noto Sans',
    zIndex: '9999'
  });

  dock.innerHTML = `
    <div style="padding:12px; font-weight:600;">Assistant</div>
    <div id="chatLog" style="height:150px; overflow:auto; background:#111827; padding:10px;"></div>
    <div style="display:flex; gap:6px; padding:10px;">
      <input id="chatInput" placeholder="Type here..." style="flex:1; border-radius:6px; border:1px solid #374151; padding:8px; background:#1f2937; color:#fff;" />
      <button id="chatSend" style="border:none; background:#10b981; color:#031014; padding:8px 10px; border-radius:6px; font-weight:600;">Send</button>
    </div>
  `;
  document.body.appendChild(dock);

  const log = document.getElementById('chatLog');
  const input = document.getElementById('chatInput');
  const send = document.getElementById('chatSend');

  function addLine(who, text) {
    const el = document.createElement('div');
    el.style.margin = '6px 0';
    el.innerHTML = `<span style="color:#9ca3af">${who}:</span> ${text}`;
    log.appendChild(el);
    log.scrollTop = log.scrollHeight;
  }

  async function handleMessage(msg) {
    addLine('You', msg);

    if (msg.trim().toLowerCase() === 'hello') {
      // 1) Trigger existing Home button if present
      const homeBtn = document.getElementById('homeBtn'); // <-- ensure your page renders a #homeBtn element
      if (homeBtn && typeof homeBtn.click === 'function') {
        homeBtn.click();
      }

      // 2) Try server-side greeting first (if /greet exists)
      try {
        const resp = await fetch('./greet', { method: 'GET' });
        if (resp.ok) {
          const data = await resp.json();
          addLine('Bot', data.message);
          return;
        }
      } catch (_) {
        // ignore and fallback
      }

      // 3) Fallback: client-side greeting using browser time
      const h = new Date().getHours();
      const message =
        h >= 5 && h < 12 ? 'Good morning' :
        h >= 12 && h < 17 ? 'Good afternoon' :
        h >= 17 && h < 22 ? 'Good evening' :
        'Good night';
      addLine('Bot', message);
      return;
    }

    addLine('Bot', "Type 'hello' to go Home and see your greeting.");
  }

  send.addEventListener('click', () => {
    const msg = input.value;
    input.value = '';
    handleMessage(msg);
  });

  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') send.click();
  });

  // Helpful debug:
  console.log('chatbot.js loaded and widget injected');
})();
