Ext.define('Ung.config.system.SystemController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.system',

    control: {
        '#': {
            beforerender: 'loadSystem'
        },
        '#regional': {
            afterrender: 'loadRegional'
        },
        '#protocols': {
            beforerender: 'initProtocols'
        },
        '#shield': {
            afterrender: 'loadShieldSettings'
        }
    },

    loadSystem: function (view) {
        view.getViewModel().set('isExpertMode', rpc.isExpertMode);
    },

    // Regional
    getLanguageSettings: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred();
        try { vm.set('languageSettings', rpc.languageManager.getLanguageSettings()); deferred.resolve(); }
        catch (ex) { deferred.reject(ex); }
        return deferred.promise;
    },

    getLanguagesList: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred();
        try { vm.set('languagesList', rpc.languageManager.getLanguagesList()); deferred.resolve(); }
        catch (ex) { deferred.reject(ex); }
        return deferred.promise;
    },

    getSystemSettings: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred();
        try { vm.set('systemSettings', rpc.systemManager.getSettings()); deferred.resolve(); }
        catch (ex) { deferred.reject(ex); }
        return deferred.promise;
    },

    getTime: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred();
        rpc.systemManager.getDate(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            vm.set('time', result);
            deferred.resolve();
        });
        return deferred.promise;
    },

    getTimeZone: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred();
        try { vm.set('timeZone', rpc.systemManager.getTimeZone()); deferred.resolve(); }
        catch (ex) { deferred.reject(ex); }
        return deferred.promise;
    },

    getTimeZonesList: function () {
        var vm = this.getViewModel(), deferred = new Ext.Deferred(), timeZones = [];
        try {
            eval(rpc.systemManager.getTimeZones()).forEach(function (tz) {
                timeZones.push({name: '(' + tz[1] + ') ' + tz[0], value: tz[0]})
            });
            vm.set('timeZonesList', timeZones);
            deferred.resolve();
        }
        catch (ex) { deferred.reject(ex); }
        return deferred.promise;
    },

    loadRegional: function (v) {
        v.setLoading(true);
        Ext.Deferred.sequence([
            this.getLanguageSettings,
            this.getLanguagesList,
            this.getSystemSettings,
            this.getTime,
            this.getTimeZone,
            this.getTimeZonesList
        ], this).then(function () {
            v.setLoading(false);
        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },

    // getTimeZone: function () {
    //     var tz = rpc.systemManager.getTimeZone();
    //     if (tz && typeof tz !== 'string' ) {
    //         tz = tz.ID;
    //     }
    //     // this.rpc.timeZone = tz;
    // },

    syncTime: function () {
        var me = this;
        Ext.MessageBox.confirm(
            'Force Time Synchronization'.t(),
            'Forced time synchronization can cause problems if the current date is far in the future.'.t() + '<br/>' +
            'A reboot is suggested after time sychronization.'.t() + '<br/><br/>' +
            'Continue?'.t(),
            function(btn) {
                if (btn === 'yes') {
                    Ext.MessageBox.wait('Syncing time with the internet...'.t(), 'Please wait'.t());
                    rpc.UvmContext.forceTimeSync(function (result, ex) {
                        Ext.MessageBox.hide();
                        if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
                        if (result !== 0) {
                            Ung.Util.exceptionToast('Time synchronization failed. Return code:'.t() + ' ' + result);
                        } else {
                            me.getTime();
                            Ung.Util.successToast('Time was synchronized!');
                        }
                    });
                }
            });
    },

    syncLanguage: function () {
        Ext.MessageBox.wait('Syncing time with the internet...'.t(), 'Please wait'.t());
        rpc.languageManager.synchronizeLanguage(function (result, ex) {
            document.location.reload();
        });
    },


    // Shield
    loadShieldSettings: function (v) {
        var vm = this.getViewModel();
        v.setLoading(true);
        try {
            vm.set('shieldSettings', rpc.nodeManager.node("untangle-node-shield").getSettings());
            v.setLoading(false);
        }
        catch (ex) {
            v.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        }
    },








    saveSettings: function () {
        var v = this.getView();
        v.setLoading('Saving...');
        Ext.Deferred.sequence([
            this.setLanguage,
            this.setSystem,
            this.setTimezone,
            this.setDate
        ], this).then(function () {
            v.setLoading(false);
            Ung.Util.successToast('System settings saved!');
        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },

    setLanguage: function () {
        console.log('Saving Language...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();

        if (vm.get('languageSettings.regionalFormats') === 'default') {
            // reset overrides
            vm.set('languageSettings.overrideDateFmt', '');
            vm.set('languageSettings.overrideDecimalSep', '');
            vm.set('languageSettings.overrideThousandSep', '');
            vm.set('languageSettings.overrideTimestampFmt', '');
        }
        rpc.languageManager.setLanguageSettings(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('languageSettings'));
        return deferred.promise;
    },

    setSystem: function () {
        console.log('Saving System...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();
        rpc.systemManager.setSettings(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('systemSettings'));
        return deferred.promise;
    },

    setTimezone: function () {
        console.log('Saving Timezone...');
        var deferred = new Ext.Deferred(),
            vm = this.getView().down('#regional').getViewModel();
        rpc.systemManager.setTimeZone(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('tz'));
        return deferred.promise;
    },

    setDate: function () {
        console.log('Saving Date...');
        var deferred = new Ext.Deferred(),
            v = this.getView().down('#regional');
        rpc.systemManager.setDate(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, new Date(v.down('datefield').getValue()).getTime());
        return deferred.promise;
    },

    // setTimezone: function () {
    //     console.log('Saving Timezone...');
    //     var deferred = new Ext.Deferred(),
    //         vm = this.getView().down('#regional').getViewModel();
    //     rpc.systemManager.setTimeZone(function (result, ex) { if (ex) { deferred.reject(ex); } deferred.resolve(); }, vm.get('tz'));
    //     return deferred.promise;
    // }


    // Support methods
    downloadSystemLogs: function () {
        Ext.Msg.alert('Status', 'Not yet implemented!');
    },

    manualReboot: function () {
        Ext.MessageBox.confirm('Manual Reboot Warning'.t(),
            Ext.String.format('The server is about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete.'.t(), rpc.companyName),
            function (btn) {
                if (btn === 'yes') {
                    rpc.UvmContext.rebootBox(function (result, ex) {
                        if (ex) { console.error(ex); Ung.Util.exceptionToast(Ext.String.format('Error: Unable to reboot {0} Server', rpc.companyName)); return; }
                        Ext.MessageBox.wait(
                            Ext.String.format('The {0} Server is rebooting.'.t(), rpc.companyName),
                            'Please wait'.t(), {
                                interval: 20, //bar will move fast!
                                increment: 500,
                                animate: true,
                                text: ''
                            });
                    });
                }
            });
    },

    manualShutdown: function () {
        Ext.MessageBox.confirm('Manual Shutdown Warning'.t(),
            Ext.String.format('The {0} Server is about to shutdown.  This will stop all network operations.'.t(), rpc.companyName),
            function (btn) {
                if (btn === 'yes') {
                    rpc.UvmContext.shutdownBox(function (result, ex) {
                        if (ex) { console.error(ex); Ung.Util.exceptionToast(Ext.String.format('Error: Unable to shutdown {0} Server', rpc.companyName)); return; }
                        Ext.MessageBox.wait(
                            Ext.String.format('The {0} Server is shutting down.'.t(), rpc.companyName),
                            'Please wait'.t(), {
                                interval: 20,
                                increment: 500,
                                animate: true,
                                text: ''
                            });
                    });
                }
            });
    },

    setupWizard: function () {
        Ext.Msg.alert('Status', 'Not yet implemented!');
    },

    factoryDefaults: function () {
        Ext.MessageBox.confirm('Reset to Factory Defaults Warning'.t(),
            'This will RESET ALL SETTINGS to factory defaults. ALL current settings WILL BE LOST.'.t(),
            function (btn) {
                if (btn === 'yes') {
                    // Ung.MetricManager.stop(); stop metrics
                    Ext.MessageBox.wait('Resetting to factory defaults...'.t(), 'Please wait'.t(), {
                        interval: 20,
                        increment: 500,
                        animate: true,
                        text: ''
                    });
                    rpc.execManager.exec(function (result, ex) {
                        Ext.MessageBox.hide();
                        if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
                        Ext.MessageBox.alert(
                            'Factory Defaults'.t(),
                            'All settings have been reset to factory defaults.', console.log('reload homepage'));
                    }, 'nohup /usr/share/untangle/bin/factory-defaults');
                }
            });
    },

    // Backup method(s)
    backupToFile: function () {
        Ext.Msg.alert('Status', 'Not yet implemented!');
    },

    // Restore method(s)
    restoreFromFile: function () {
        Ext.Msg.alert('Status', 'Not yet implemented!');
    },

    getHttpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('httpSettings', rpc.nodeManager.node('untangle-casing-http').getHttpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
        }
    },
    getFtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('ftpSettings', rpc.nodeManager.node('untangle-casing-ftp').getFtpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
        }
    },

    getSmtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('smtpSettings', rpc.nodeManager.node('untangle-casing-smtp').getSmtpNodeSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
        }
    },

    // Protocols methods
    initProtocols: function () {
        this.getHttpSettings();
        this.getFtpSettings();
        this.getSmtpSettings();
    }

});