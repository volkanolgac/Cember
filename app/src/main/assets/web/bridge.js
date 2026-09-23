/**
 * Volkan Web2Android JavaScript Client SDK
 * Production-ready asynchronous RPC bridge for Android Native Shell.
 */
(function (global) {
  const pendingRequests = new Map();
  let reqCounter = 0;

  function generateRequestId() {
    return 'req_' + Date.now() + '_' + (++reqCounter);
  }

  const VolkanSDK = {
    isAvailable: function () {
      return typeof global.VolkanAndroid !== 'undefined';
    },

    invoke: function (action, params = {}) {
      return new Promise((resolve, reject) => {
        const requestId = generateRequestId();
        const payload = JSON.stringify({
          requestId: requestId,
          action: action,
          params: params
        });

        if (!this.isAvailable()) {
          console.warn('[VolkanSDK] Native host not detected. Mocking response for action:', action);
          return resolve(this._getMockFallback(action, params));
        }

        pendingRequests.set(requestId, { resolve, reject });

        try {
          if (global.VolkanAndroid.postMessageAsync) {
            global.VolkanAndroid.postMessageAsync(payload);
          } else if (global.VolkanAndroid.postMessage) {
            const raw = global.VolkanAndroid.postMessage(payload);
            if (raw) {
              const res = JSON.parse(raw);
              pendingRequests.delete(requestId);
              if (res.success) resolve(res.data);
              else reject(new Error(res.error || 'Native bridge error'));
            }
          } else {
            reject(new Error('No compatible native postMessage method'));
          }
        } catch (err) {
          pendingRequests.delete(requestId);
          reject(err);
        }
      });
    },

    getAppInfo: function () { return this.invoke('getAppInfo'); },
    getNetworkState: function () { return this.invoke('getNetworkState'); },
    vibrate: function (duration = 50) { return this.invoke('vibrate', { duration }); },
    share: function (title, text, url) { return this.invoke('share', { title, text, url }); },
    openExternal: function (url) { return this.invoke('openExternal', { url }); },
    setOrientation: function (orientation) { return this.invoke('setOrientation', { orientation }); },
    setFullscreen: function (fullscreen) { return this.invoke('setFullscreen', { fullscreen }); },
    setKeepScreenOn: function (keepScreenOn) { return this.invoke('setKeepScreenOn', { keepScreenOn }); },
    setClipboard: function (text) { return this.invoke('setClipboard', { text }); },
    getClipboard: function () { return this.invoke('getClipboard'); },

    _onResponse: function (responseString) {
      try {
        const res = typeof responseString === 'string' ? JSON.parse(responseString) : responseString;
        const entry = pendingRequests.get(res.requestId);
        if (entry) {
          pendingRequests.delete(res.requestId);
          if (res.success) entry.resolve(res.data);
          else entry.reject(new Error(res.error || 'Native bridge error'));
        }
      } catch (err) {
        console.error('[VolkanSDK] Failed to parse native response:', err);
      }
    },

    _getMockFallback: function (action, params) {
      switch (action) {
        case 'getAppInfo':
          return { appName: 'Web Browser Mock', applicationId: 'com.browser.mock', versionName: '1.0.0', versionCode: 1, buildType: 'web' };
        case 'getNetworkState':
          return { isConnected: navigator.onLine, type: 'WIFI', isWifi: true, isCellular: false };
        case 'vibrate':
          if (navigator.vibrate) navigator.vibrate(params.duration || 50);
          return { vibrated: true };
        case 'setClipboard':
          if (navigator.clipboard) navigator.clipboard.writeText(params.text || '');
          return { copied: true };
        default:
          return { fallback: true };
      }
    }
  };

  if (global.VolkanAndroid) {
    global.VolkanAndroid._onResponse = VolkanSDK._onResponse.bind(VolkanSDK);
  } else {
    global.VolkanAndroid = { _onResponse: VolkanSDK._onResponse.bind(VolkanSDK) };
  }

  global.Volkan = VolkanSDK;
})(window);
