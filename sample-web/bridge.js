/**
 * Volkan Web2Android Modern WebMessage Bridge SDK
 * Secure, asynchronous, Promise-based native RPC communication via AndroidX WebMessageListener.
 */
(function(window) {
  'use strict';

  const pendingRequests = new Map();
  const REQUEST_TIMEOUT_MS = 8000;
  let isListenerAttached = false;

  function generateRequestId() {
    return 'req_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
  }

  function attachNativeReplyListener() {
    if (isListenerAttached) return;
    if (typeof window.VolkanAndroid === 'object' && window.VolkanAndroid !== null) {
      try {
        window.VolkanAndroid.onmessage = function(event) {
          handleIncomingNativeReply(event.data);
        };
        if (typeof window.VolkanAndroid.addEventListener === 'function') {
          window.VolkanAndroid.addEventListener('message', function(event) {
            handleIncomingNativeReply(event.data);
          });
        }
        isListenerAttached = true;
      } catch (err) {
        console.warn('[VolkanBridge] Error attaching message listener to window.VolkanAndroid:', err);
      }
    }
  }

  function handleIncomingNativeReply(rawData) {
    if (!rawData) return;
    try {
      const response = typeof rawData === 'string' ? JSON.parse(rawData) : rawData;
      const { requestId, success, data, error, message } = response;

      if (!requestId || !pendingRequests.has(requestId)) {
        return;
      }

      const { resolve, reject, timer } = pendingRequests.get(requestId);
      clearTimeout(timer);
      pendingRequests.delete(requestId);

      if (success) {
        resolve(data || {});
      } else {
        const err = new Error(message || error || 'Native bridge call failed');
        err.code = error || 'UNKNOWN_ERROR';
        reject(err);
      }
    } catch (e) {
      console.error('[VolkanBridge] Failed to parse native message reply:', e, rawData);
    }
  }

  // Attempt initial attachment
  attachNativeReplyListener();

  /**
   * Core RPC method to post message to native shell and await response.
   */
  function invokeNative(action, params = {}) {
    attachNativeReplyListener();

    return new Promise((resolve, reject) => {
      if (!window.VolkanAndroid || typeof window.VolkanAndroid.postMessage !== 'function') {
        const err = new Error('Volkan Android native bridge is not available in this environment');
        err.code = 'BRIDGE_UNAVAILABLE';
        return reject(err);
      }

      const requestId = generateRequestId();

      const timer = setTimeout(() => {
        if (pendingRequests.has(requestId)) {
          pendingRequests.delete(requestId);
          const timeoutErr = new Error(`Native bridge call '${action}' timed out after ${REQUEST_TIMEOUT_MS}ms`);
          timeoutErr.code = 'TIMEOUT';
          reject(timeoutErr);
        }
      }, REQUEST_TIMEOUT_MS);

      pendingRequests.set(requestId, { resolve, reject, timer });

      const payload = JSON.stringify({
        requestId: requestId,
        action: action,
        params: params
      });

      try {
        window.VolkanAndroid.postMessage(payload);
      } catch (sendError) {
        clearTimeout(timer);
        pendingRequests.delete(requestId);
        reject(sendError);
      }
    });
  }

  // Public SDK Surface
  const Volkan = {
    isAvailable: function() {
      return typeof window.VolkanAndroid === 'object' && window.VolkanAndroid !== null && typeof window.VolkanAndroid.postMessage === 'function';
    },

    ping: function() {
      return invokeNative('ping');
    },

    getAppInfo: function() {
      return invokeNative('getAppInfo');
    },

    getCapabilities: function() {
      return invokeNative('getCapabilities');
    },

    getNetworkState: function() {
      return invokeNative('getNetworkState');
    },

    vibrate: function(durationMs = 100) {
      return invokeNative('vibrate', { duration: durationMs });
    },

    share: function(options = {}) {
      return invokeNative('share', {
        title: options.title || '',
        text: options.text || '',
        url: options.url || ''
      });
    },

    copyToClipboard: function(text) {
      return invokeNative('copyToClipboard', { text: text || '' });
    },

    getClipboard: function() {
      return invokeNative('getClipboard');
    },

    keepScreenOn: function(enabled = true) {
      return invokeNative('keepScreenOn', { enabled: Boolean(enabled) });
    },

    setOrientation: function(mode = 'PORTRAIT') {
      return invokeNative('setOrientation', { mode: String(mode).toUpperCase() });
    },

    setFullscreen: function(enabled = true) {
      return invokeNative('setFullscreen', { fullscreen: Boolean(enabled) });
    },

    openExternal: function(url) {
      return invokeNative('openExternal', { url: String(url) });
    },

    _invoke: invokeNative
  };

  window.Volkan = Volkan;
  window.addEventListener('DOMContentLoaded', attachNativeReplyListener);
})(window);
