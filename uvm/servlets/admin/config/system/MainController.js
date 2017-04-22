Ext.define('Ung.config.system.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config-system',

    control: {
        '#': { afterrender: 'loadSystem' },
        '#regional': { afterrender: 'loadRegional' },
        '#protocols': { beforerender: 'initProtocols' },
        '#shield': { afterrender: 'loadShieldSettings' }
    },

    loadSystem: function (view) {
        view.getViewModel().set('isExpertMode', rpc.isExpertMode);
    },

    // Regional
    loadRegional: function (v) {
        var vm = this.getViewModel(),
            timeZones = [];
        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.languageManager.getLanguageSettings'),
            Rpc.directPromise('rpc.languageManager.getLanguagesList'),
            Rpc.directPromise('rpc.systemManager.getSettings'),
            Rpc.asyncPromise('rpc.systemManager.getDate'),
            Rpc.directPromise('rpc.systemManager.getTimeZone'),
            Rpc.directPromise('rpc.systemManager.getTimeZones'),
        ], this).then(function (result) {
            v.setLoading(false);
            vm.set({
                languageSettings: result[0],
                languagesList: result[1],
                systemSettings: result[2],
                time: result[3],
                timeZone: result[4],
            });

            if (result[5]) {
                eval(result[5]).forEach(function (tz) {
                    timeZones.push({name: '(' + tz[1] + ') ' + tz[0], value: tz[0]});
                });
                vm.set('timeZonesList', timeZones);
            }
        });
    },

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
                        if (ex) { console.error(ex); Util.handleException(ex); return; }
                        if (result !== 0) {
                            Util.handleException('Time synchronization failed. Return code:'.t() + ' ' + result);
                        } else {
                            me.getTime();
                            Util.successToast('Time was synchronized!');
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
            vm.set('shieldSettings', rpc.appManager.app('shield').getSettings());
            v.setLoading(false);
        }
        catch (ex) {
            v.setLoading(false);
            console.error(ex);
            Util.handleException(ex);
        }
    },








    saveSettings: function () {
        var v = this.getView(),
            vm = this.getViewModel();

        v.setLoading('Saving...');
        if (vm.get('languageSettings.regionalFormats') === 'default') {
            // reset overrides
            vm.set('languageSettings.overrideDateFmt', '');
            vm.set('languageSettings.overrideDecimalSep', '');
            vm.set('languageSettings.overrideThousandSep', '');
            vm.set('languageSettings.overrideTimestampFmt', '');
        }

        // var newDate = new Date(v.down('#regional').down('datefield').getValue()).getTime();

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.languageManager.setLanguageSettings', vm.get('languageSettings')),
            Rpc.asyncPromise('rpc.systemManager.setSettings', vm.get('systemSettings')),
            Rpc.asyncPromise('rpc.systemManager.setTimeZone', vm.get('timeZone')),
            // Rpc.asyncPromise('rpc.systemManager.setDate', newDate),
            // this.setShield
        ], this).then(function () {
            v.setLoading(false);
            Util.successToast('System settings saved!');
        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Util.handleException(ex);
        });
    },

    setShield: function () {
        var deferred = new Ext.Deferred(),
            v = this.getView(), vm = this.getViewModel();
        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            console.log(store);
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
        rpc.appManager.app('shield').setSettings(function (result, ex) { if (ex) { console.log('exception'); deferred.reject(ex); } deferred.resolve(); }, vm.get('shieldSettings'));
        return deferred.promise;
    },


    // Support methods
    downloadSystemLogs: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'SystemSupportLogs';
        downloadForm.submit();
    },

    manualReboot: function () {
        Ext.MessageBox.confirm('Manual Reboot Warning'.t(),
            Ext.String.format('The server is about to manually reboot.  This will interrupt normal network operations until the {0} Server is finished automatically restarting. This may take up to several minutes to complete.'.t(), rpc.companyName),
            function (btn) {
                if (btn === 'yes') {
                    rpc.UvmContext.rebootBox(function (result, ex) {
                        if (ex) { console.error(ex); Util.handleException(Ext.String.format('Error: Unable to reboot {0} Server', rpc.companyName)); return; }
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
                        if (ex) { console.error(ex); Util.handleException(Ext.String.format('Error: Unable to shutdown {0} Server', rpc.companyName)); return; }
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
                        if (ex) { console.error(ex); Util.handleException(ex); return; }
                        Ext.MessageBox.alert(
                            'Factory Defaults'.t(),
                            'All settings have been reset to factory defaults.', console.log('reload homepage'));
                    }, 'nohup /usr/share/untangle/bin/factory-defaults');
                }
            });
    },

    // Backup method(s)
    backupToFile: function () {
        var downloadForm = document.getElementById('downloadForm');
        downloadForm.type.value = 'backup';
        downloadForm.submit();
    },

    // Restore method(s)
    restoreFromFile: function (btn) {
        var restoreFile = this.getView().down('#restoreFile').getValue();
        if (!restoreFile || restoreFile.length === 0) {
            Util.handleException('Please select a file to upload.'.t());
            return;
        }
        btn.up('form').submit({
            waitMsg: 'Restoring from File...'.t(),
            success: function (form, action) {
                Ext.MessageBox.alert('Restore'.t(), action.result.msg);
            },
            failure: function (form, action) {
                var errorMsg = 'The File restore procedure failed.'.t();
                if (action.result && action.result.msg) {
                    errorMsg = action.result.msg;
                }
                Ext.MessageBox.alert('Failed', errorMsg);
            }
        });
    },

    getHttpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('httpSettings', rpc.appManager.app('http').getHttpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Util.handleException(ex); return; }
        }
    },
    getFtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('ftpSettings', rpc.appManager.app('ftp').getFtpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Util.handleException(ex); return; }
        }
    },

    getSmtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('smtpSettings', rpc.appManager.app('smtp').getSmtpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Util.handleException(ex); return; }
        }
    },

    // Protocols methods
    initProtocols: function () {
        this.getHttpSettings();
        this.getFtpSettings();
        this.getSmtpSettings();
    }

});
