Ext.define('Ung.config.administration.AdministrationController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.administration',

    control: {
        '#': {
            beforerender: 'loadAdmin',
            tabchange: 'onTabChange'
        },
        '#certificates': {
            beforerender: 'loadCertificates'
        },
        '#skins': {
            beforerender: 'loadSkins'
        }
    },

    onTabChange: function (tabPanel, newCard) {
        // window.location.hash = '#config/administration/' + newCard.getItemId();
        // Ung.app.redirectTo('#config/administration/' + newCard.getItemId(), false);
    },

    certificateManager: rpc.UvmContext.certificateManager(),

    loadAdmin: function (view) {
        this.adminSettings();
        this.systemSettings();
        this.skinSettings();
    },

    loadCertificates: function (view) {
        this.serverCertificates();
        this.rootCertificateInformation();
        this.serverCertificateVerification();
    },

    loadSkins: function () {
        this.skinsList();
    },

    adminSettings: function () {
        var me = this;
        rpc.adminManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('adminSettings', result);
        });
    },

    systemSettings: function () {
        var me = this;
        rpc.systemManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('systemSettings', result);
        });
    },

    serverCertificates: function () {
        var me = this;
        this.certificateManager.getServerCertificateList(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('serverCertificates', result);
        });
    },

    rootCertificateInformation: function () {
        var me = this;
        this.certificateManager.getRootCertificateInformation(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('rootCertificateInformation', result);
        });
    },

    serverCertificateVerification: function () {
        var me = this;
        this.certificateManager.validateActiveInspectorCertificates(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('serverCertificateVerification', result);
        });
    },

    skinSettings: function () {
        var me = this;
        rpc.skinManager.getSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('skinSettings', result);
        });
    },

    skinsList: function () {
        var me = this;
        rpc.skinManager.getSkinsList(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('skinsList', result);
        });
    },

    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();

        view.setLoading('Saving ...');

        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });

        Ext.Deferred.sequence([
            this.setAdminSettings,
            this.setSkinSettings,
            this.setSystemSettings
        ], this).then(function () {
            view.setLoading(false);

            me.loadAdmin(); me.loadCertificates(); me.loadSkins();

            Ung.Util.successToast('Administration'.t() + ' settings saved!');
        }, function (ex) {
            view.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },

    setAdminSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        rpc.adminManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('adminSettings'));
         return deferred.promise;
    },

    setSkinSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        rpc.skinManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('skinSettings'));
        return deferred.promise;
    },

    setSystemSettings: function () {
        var me = this,
            deferred = new Ext.Deferred();
        rpc.systemManager.setSettings(function(result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        }, me.getViewModel().get('systemSettings'));
        return deferred.promise;
    },








    addAccount: function () {
        Ext.MessageBox.show({
            title: 'Administrator Warning'.t(),
            msg: 'This action will add an ADMINISTRATOR account.'.t() + '<br/>' + '<br/>' +
                '<b>' + 'ADMINISTRATORS (also sometimes known as admin or root or superuser) have ADMINISTRATOR access to the server.'.t() + '</b>' + '<br/>' + '<br/>' +
                'Administrator accounts have the ability to do anything including:'.t() + '<br/>' +
                '<ul>' +
                '<li>' + 'Read/Modify any setting'.t() + '</li>' +
                '<li>' + 'Restore/Backup all settings'.t() + '</li>' +
                '<li>' + 'Create more administrators'.t() + '</li>' +
                '<li>' + 'Delete/Modify/Create any file'.t() + '</li>' +
                '<li>' + 'Run any command'.t() + '</li>' +
                '<li>' + 'Install any software'.t() + '</li>' +
                '<li>' + 'Complete control and access identical to what you now possess'.t() + '</li>' +
                '</ul>' + '<br/>' +
                'Do you understand the above statement?'.t() + '<br/>' +
                '<input type="checkbox" id="admin_understand"/> <i>' + 'Yes, I understand.'.t() + '</i>' + '<br/>' +
                '<br/>' +
                'Do you wish to continue?'.t() + '<br/>',
            buttons: Ext.MessageBox.YESNO,
            fn: Ext.bind(function(btn) {
                if (btn == "yes") {
                    // if (Ext.get('admin_understand').dom.checked) {
                    //     Ung.grid.Panel.prototype.addHandler.call(this, button, e, rowData);
                    // }
                }
            }, this)});
    }


});