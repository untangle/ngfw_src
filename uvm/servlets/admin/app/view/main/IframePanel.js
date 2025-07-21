Ext.define('Ung.view.main.IframePanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.iframe-panel',
    layout: 'fit',
    border: false,
    bodyPadding: 0,

    config: {
        iframeUrl: '',
        messageData: null
    },

    initComponent: function () {
        this.callParent(arguments);

        this.on('afterrender', function (panel) {
            console.log("Iframe Panel Loaded");
            var iframe = document.createElement('iframe');

            iframe.src = panel.iframeUrl || '';
            iframe.width = '100%';
            iframe.height = '100%';
            iframe.allowFullscreen = true;
            iframe.style.border = 'none';

            panel.body.dom.appendChild(iframe);
            panel.iframeEl = iframe;
        });
    },

    updateIframe: function ( iframeUrl, hidden, messageData ) {
        if (iframeUrl) {
            this.iframeUrl = iframeUrl;

            if (this.iframeEl) {
                this.iframeEl.src = iframeUrl;
            }
        }

        if (typeof hidden === 'boolean') {
            this.setHidden(hidden);
        }

        // Message Data to post message to Iframe
        if (messageData) {
            this.messageData = messageData;

            // Send data immediately if iframe already loaded
            if (this.iframeEl && this.iframeEl.contentWindow) {
                var me = this;
                this.iframeEl.onload = function () {
                    me.iframeEl.contentWindow.postMessage(
                        messageData,
                        new URL(me.iframeUrl).origin
                    );
                };
            }
        }
    }
});
