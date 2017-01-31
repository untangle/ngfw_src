Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            // beforerender: 'onBeforeRender',
            // close: 'onClose',
        },
    },

    onAfterRender: function (view) {
        console.log(view.getViewModel());
    },


});
