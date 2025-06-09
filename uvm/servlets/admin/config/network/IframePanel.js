Ext.define('Ung.cmp.IframePanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.iframe-panel',
    layout: 'fit',
    border: false,

    config: {
        iframeUrl: ''
    },

    initComponent: function () {
        this.callParent(arguments);

        this.iframeCmp = Ext.create('Ext.Component', {
            autoEl: {
                tag: 'iframe',
                src: this.iframeUrl || '',
                allowFullscreen: true,
                layout: 'fit',
            }
        });

        this.add(this.iframeCmp);
    },

    updateIframeUrl: function (url) {
        if (this.iframeCmp && this.iframeCmp.getEl()) {
            this.iframeCmp.getEl().dom.src = url;
        }
    }
});
