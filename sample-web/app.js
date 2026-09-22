/**
 * Volkan Web2Android Host Sample Web Application
 */
document.addEventListener('DOMContentLoaded', () => {
  const logConsole = document.getElementById('logConsole');
  function log(msg, type = 'info') {
    const entry = document.createElement('div');
    entry.className = `log-entry log-${type}`;
    const time = new Date().toLocaleTimeString();
    entry.textContent = `[${time}] ${msg}`;
    logConsole.appendChild(entry);
    logConsole.scrollTop = logConsole.scrollHeight;
  }

  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');
  const fpsCounter = document.getElementById('fpsCounter');

  let particles = [];
  let isRunning = true;
  let lastFrameTime = performance.now();
  let frameCount = 0;
  let fps = 60;

  function resizeCanvas() {
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * window.devicePixelRatio;
    canvas.height = rect.height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  }
  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);

  function createParticle(x, y) {
    const angle = Math.random() * Math.PI * 2;
    const speed = Math.random() * 3 + 1;
    const colors = ['#38BDF8', '#0284C7', '#34D399', '#F472B6', '#FBBF24'];
    return {
      x: x || (canvas.clientWidth / 2),
      y: y || (canvas.clientHeight / 2),
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      radius: Math.random() * 4 + 2,
      color: colors[Math.floor(Math.random() * colors.length)],
      alpha: 1,
      decay: Math.random() * 0.02 + 0.01
    };
  }

  function spawnBurst(count = 30, x, y) {
    for (let i = 0; i < count; i++) {
      particles.push(createParticle(x, y));
    }
  }
  spawnBurst(40);

  function gameLoop(now) {
    if (isRunning) {
      frameCount++;
      if (now - lastFrameTime >= 1000) {
        fps = Math.round((frameCount * 1000) / (now - lastFrameTime));
        fpsCounter.textContent = `${fps} FPS`;
        frameCount = 0;
        lastFrameTime = now;
      }

      ctx.fillStyle = 'rgba(2, 6, 23, 0.25)';
      ctx.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight);

      for (let i = particles.length - 1; i >= 0; i--) {
        const p = particles[i];
        p.x += p.vx;
        p.y += p.vy;
        p.alpha -= p.decay;

        if (p.x < 0 || p.x > canvas.clientWidth) p.vx *= -1;
        if (p.y < 0 || p.y > canvas.clientHeight) p.vy *= -1;

        if (p.alpha <= 0) {
          particles.splice(i, 1);
          continue;
        }

        ctx.save();
        ctx.globalAlpha = p.alpha;
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
      }

      if (particles.length < 20) {
        particles.push(createParticle());
      }
    }
    requestAnimationFrame(gameLoop);
  }
  requestAnimationFrame(gameLoop);

  canvas.addEventListener('pointerdown', (e) => {
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    spawnBurst(25, x, y);
    Volkan.vibrate(20).catch(() => {});
  });

  document.getElementById('btnSpawnParticles').addEventListener('click', () => {
    spawnBurst(50);
    log('Spawned 50 particle entities', 'info');
  });

  document.getElementById('btnToggleMotion').addEventListener('click', (e) => {
    isRunning = !isRunning;
    e.target.textContent = isRunning ? '⏯️ Pause' : '▶️ Resume';
    log(`Canvas simulation ${isRunning ? 'resumed' : 'paused'}`, 'info');
  });

  document.getElementById('btnClearCanvas').addEventListener('click', () => {
    particles = [];
    ctx.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);
    log('Canvas cleared', 'info');
  });

  Volkan.getAppInfo().then((info) => {
    document.getElementById('appId').textContent = info.applicationId || 'Host Shell';
    document.getElementById('appVer').textContent = `${info.versionName} (${info.versionCode})`;
    log(`Host connected: ${info.appName} v${info.versionName} [${info.buildType}]`, 'success');
  }).catch((err) => {
    log(`AppInfo error: ${err.message}`, 'warn');
  });

  function updateNetworkUI(state) {
    const netStatus = document.getElementById('netStatus');
    const netType = document.getElementById('netType');
    if (state.isConnected) {
      netStatus.textContent = 'ONLINE';
      netStatus.className = 'stat-value text-success';
      netType.textContent = state.type || 'Connected';
    } else {
      netStatus.textContent = 'OFFLINE';
      netStatus.className = 'stat-value text-warn';
      netType.textContent = 'Disconnected';
    }
  }

  Volkan.getNetworkState().then((state) => {
    updateNetworkUI(state);
    log(`Network status: ${state.isConnected ? 'ONLINE' : 'OFFLINE'} (${state.type})`, 'info');
  }).catch(() => {});

  window.addEventListener('volkan:networkChange', (e) => {
    const state = e.detail;
    updateNetworkUI(state);
    log(`Network changed: ${state.isConnected ? 'ONLINE' : 'OFFLINE'} (${state.type})`, 'info');
  });

  let isLandscape = false;
  let isFullscreen = false;

  document.getElementById('btnVibrate').addEventListener('click', () => {
    Volkan.vibrate(60).then(() => {
      log('Vibration triggered (60ms)', 'success');
    }).catch(err => log(`Vibrate error: ${err.message}`, 'warn'));
  });

  document.getElementById('btnShare').addEventListener('click', () => {
    Volkan.share('Volkan Web2Android', 'Experience high-performance web applications running locally inside a native Android shell!', 'https://github.com').then(() => {
      log('Native share dialog requested', 'success');
    }).catch(err => log(`Share error: ${err.message}`, 'warn'));
  });

  document.getElementById('btnFullscreen').addEventListener('click', () => {
    isFullscreen = !isFullscreen;
    Volkan.setFullscreen(isFullscreen).then(() => {
      log(`Fullscreen set to: ${isFullscreen}`, 'success');
    }).catch(err => log(`Fullscreen error: ${err.message}`, 'warn'));
  });

  document.getElementById('btnOrientation').addEventListener('click', (e) => {
    isLandscape = !isLandscape;
    const mode = isLandscape ? 'LANDSCAPE' : 'PORTRAIT';
    e.target.textContent = isLandscape ? '🔄 Toggle Portrait' : '🔄 Toggle Landscape';
    Volkan.setOrientation(mode).then(() => {
      log(`Orientation changed to: ${mode}`, 'success');
    }).catch(err => log(`Orientation error: ${err.message}`, 'warn'));
  });

  document.getElementById('btnClipboard').addEventListener('click', () => {
    const textToCopy = `Volkan Web2Android Token #${Math.floor(Math.random() * 10000)}`;
    Volkan.setClipboard(textToCopy).then(() => {
      log(`Copied to clipboard: "${textToCopy}"`, 'success');
    }).catch(err => log(`Clipboard error: ${err.message}`, 'warn'));
  });

  document.getElementById('btnExternal').addEventListener('click', () => {
    Volkan.openExternal('https://android.com').then(() => {
      log('Opened external URL: https://android.com', 'info');
    }).catch(err => log(`External link error: ${err.message}`, 'warn'));
  });

  const storageCount = document.getElementById('storageCount');
  let currentCount = parseInt(localStorage.getItem('volkan_counter') || '0', 10);
  storageCount.textContent = currentCount;

  document.getElementById('btnIncrement').addEventListener('click', () => {
    currentCount++;
    localStorage.setItem('volkan_counter', currentCount.toString());
    storageCount.textContent = currentCount;
    log(`LocalStorage updated count to: ${currentCount}`, 'success');
    Volkan.vibrate(15).catch(() => {});
  });

  const fileInput = document.getElementById('fileInput');
  const fileName = document.getElementById('fileName');
  fileInput.addEventListener('change', (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      fileName.textContent = `${file.name} (${Math.round(file.size / 1024)} KB)`;
      log(`File selected: ${file.name} [${file.type}]`, 'success');
    }
  });

  document.getElementById('btnClearLogs').addEventListener('click', () => {
    logConsole.innerHTML = '';
  });
});
