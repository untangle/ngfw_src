Ext.define('Ung.config.localdirectory.LocalDirectoryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.localdirectory',

    control: {
        '#': {
            beforerender: 'loadSettings'
        }
    },

    localDirectory: rpc.UvmContext.localDirectory(),

    loadSettings: function () {
        var me = this;
        this.localDirectory.getUsers(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            console.log(result);
            me.getViewModel().set('usersData', result);
        });
    }

});