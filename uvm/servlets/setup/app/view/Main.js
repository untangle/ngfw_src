Ext.define('Ung.Setup.Main', {
    extend: 'Ext.container.Viewport',
    
    viewModel: {
        data: {
            resuming: false,
            remoteReachable: null
        }
    },
    layout: 'fit',
    padding: 20,
    listeners: {
        afterrender: function (view) {
            view.setHtml('<iframe src="/vue/setup/" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;"></iframe>');
        },
    }

});
