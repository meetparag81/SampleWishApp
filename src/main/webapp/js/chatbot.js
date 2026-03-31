(function () {
    const RUNNER_SERVLET = '/SampleWishApp/TestRunnerServlet';

    // ── Build chatbot UI ──────────────────────────────────────────────
    const dock = document.createElement('div');
    dock.id = 'chatbotDock';
    Object.assign(dock.style, {
        position: 'fixed', bottom: '20px', right: '20px', width: '320px',
        background: '#1f2937', color: '#fff', borderRadius: '12px',
        boxShadow: '0 8px 28px rgba(0,0,0,0.35)',
        fontFamily: 'system-ui,-apple-system,Segoe UI,Roboto,sans-serif',
        zIndex: '9999', overflow: 'hidden'
    });

    dock.innerHTML = `
    <div id="chatHeader" style="background:#111827;padding:12px 16px;
         cursor:pointer;display:flex;justify-content:space-between;
         align-items:center;">
        <span style="font-weight:700;font-size:15px;">🤖 WishApp Test Bot</span>
        <span id="chatToggle" style="font-size:18px;">▲</span>
    </div>
    <div id="chatBody" style="display:flex;flex-direction:column;height:360px;">
        <div id="chatLog" style="flex:1;overflow-y:auto;padding:12px;
             font-size:13px;line-height:1.6;"></div>
        <div id="fileRow" style="padding:8px;border-top:1px solid #374151;">
            <input type="file" id="fileInput" accept=".xlsx"
                style="width:100%;box-sizing:border-box;color:#d1d5db;
                font-size:12px;margin-bottom:6px;" />
            <button id="executeBtn"
                style="width:100%;padding:9px;background:#2563eb;
                color:#fff;border:none;border-radius:8px;cursor:pointer;
                font-weight:600;font-size:13px;">
                ▶ Execute Tests
            </button>
        </div>
    </div>`;

    document.body.appendChild(dock);

    // ── Collapse / expand ─────────────────────────────────────────────
    document.getElementById('chatHeader').addEventListener('click', () => {
        const body   = document.getElementById('chatBody');
        const tog    = document.getElementById('chatToggle');
        const hidden = body.style.display === 'none';
        body.style.display = hidden ? 'flex' : 'none';
        tog.textContent    = hidden ? '▲' : '▼';
    });

    // ── Log helpers ───────────────────────────────────────────────────
    function log(msg, color) {
        const div = document.createElement('div');
        div.style.cssText = 'margin-bottom:6px;color:' + (color || '#d1d5db');
        div.innerHTML = msg;
        const cl = document.getElementById('chatLog');
        cl.appendChild(div);
        cl.scrollTop = cl.scrollHeight;
    }
    const logBot  = m => log('<b style="color:#60a5fa">Bot:</b> ' + m);
    const logUser = m => log('<b style="color:#34d399">You:</b> ' + m);
    const logInfo = m => log('ℹ️ ' + m, '#9ca3af');
    const logOk   = m => log('✅ ' + m, '#4ade80');
    const logErr  = m => log('❌ ' + m, '#f87171');

    // ── Greeting ──────────────────────────────────────────────────────
    logBot('Hi! 👋 Welcome to WishApp Test Bot.');
    logBot('Please <b>select TestData.xlsx</b> and click <b>Execute Tests</b>.');

    // ── Execute handler ───────────────────────────────────────────────
    async function handleExecute() {
        const fileInput = document.getElementById('fileInput');
        const execBtn   = document.getElementById('executeBtn');

        if (!fileInput.files || fileInput.files.length === 0) {
            logErr('Please select a TestData.xlsx file first.');
            return;
        }
        const file = fileInput.files[0];
        if (!file.name.endsWith('.xlsx')) {
            logErr('Only .xlsx files are supported.');
            return;
        }

        // Lock button during test run
        execBtn.disabled         = true;
        execBtn.textContent      = '⏳ Running…';
        execBtn.style.background = '#374151';

        logUser('📎 ' + file.name + ' selected');
        logInfo('Uploading to TestRunnerServlet…');
        logInfo('Selenium tests running — please wait ⏳');

        try {
            const formData = new FormData();
            formData.append('testDataFile', file);

            const res = await fetch(RUNNER_SERVLET, {
                method: 'POST',
                body: formData
            });

            if (res.ok) {
                const html = await res.text();
                if (html.includes('executed successfully') ||
                    html.includes('✅')) {
                    logOk('All tests <b>executed successfully!</b>');
                } else if (html.includes('error') ||
                           html.includes('Error')) {
                    logErr('Tests ran but errors found. Check results file.');
                } else {
                    logOk('Tests completed!');
                }

                logBot('📥 <a href="/SampleWishApp/DownloadServlet"'
                     + ' target="_blank"'
                     + ' style="color:#4ade80;font-weight:bold;">'
                     + 'Download TestResults.xlsx ↗</a>');

                logBot('Run again? <span id="restartBtn"'
                     + ' style="color:#60a5fa;cursor:pointer;'
                     + 'text-decoration:underline;">Click here</span>');
                document.getElementById('restartBtn')
                        .addEventListener('click', restartBot);

            } else {
                logErr('TestRunnerServlet returned HTTP ' + res.status);
                resetBtn();
            }

        } catch (e) {
            logErr('Request failed: ' + e.message);
            resetBtn();
        }
    }

    function resetBtn() {
        const b          = document.getElementById('executeBtn');
        b.disabled       = false;
        b.textContent    = '▶ Execute Tests';
        b.style.background = '#2563eb';
    }

    function restartBot() {
        document.getElementById('chatLog').innerHTML = '';
        document.getElementById('fileInput').value  = '';
        resetBtn();
        logBot('Ready for another run! 👋');
        logBot('Select <b>TestData.xlsx</b> and click Execute Tests.');
    }

    document.getElementById('executeBtn')
            .addEventListener('click', handleExecute);
})();