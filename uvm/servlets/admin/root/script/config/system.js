Ext.define('Ung.config.system.System', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.system',
    requires: [
        'Ung.config.system.SystemController',
        'Ung.config.system.SystemModel',
    ],
    controller: 'config.system',
    viewModel: {
        type: 'config.system'
    },
    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'System'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],
    items: [{
        xtype: 'config.system.regional'
    }, {
        xtype: 'config.system.support'
    }, {
        xtype: 'config.system.backup'
    }, {
        xtype: 'config.system.restore'
    }, {
        xtype: 'config.system.protocols'
    }, {
        xtype: 'config.system.shield'
    }]
});
Ext.define('Ung.config.system.SystemController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.system',
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
                        if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
                        if (result !== 0) {
                            Util.exceptionToast('Time synchronization failed. Return code:'.t() + ' ' + result);
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
            vm.set('shieldSettings', rpc.nodeManager.node('untangle-node-shield').getSettings());
            v.setLoading(false);
        }
        catch (ex) {
            v.setLoading(false);
            console.error(ex);
            Util.exceptionToast(ex);
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
        var newDate = new Date(v.down('#regional').down('datefield').getValue()).getTime();
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
            Util.exceptionToast(ex);
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
        rpc.nodeManager.node('untangle-node-shield').setSettings(function (result, ex) { if (ex) { console.log('exception'); deferred.reject(ex); } deferred.resolve(); }, vm.get('shieldSettings'));
        return deferred.promise;
    },
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
                        if (ex) { console.error(ex); Util.exceptionToast(Ext.String.format('Error: Unable to reboot {0} Server', rpc.companyName)); return; }
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
                        if (ex) { console.error(ex); Util.exceptionToast(Ext.String.format('Error: Unable to shutdown {0} Server', rpc.companyName)); return; }
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
                        if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
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
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
        }
    },
    getFtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('ftpSettings', rpc.nodeManager.node('untangle-casing-ftp').getFtpSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
        }
    },
    getSmtpSettings: function () {
        var vm = this.getViewModel();
        try {
            vm.set('smtpSettings', rpc.nodeManager.node('untangle-casing-smtp').getSmtpNodeSettings());
        } catch (ex) {
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
        }
    },
    // Protocols methods
    initProtocols: function () {
        this.getHttpSettings();
        this.getFtpSettings();
        this.getSmtpSettings();
    }
});
Ext.define('Ung.config.system.SystemModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.config.system',
    data: {
        time: null,
        languageSettings: null,
        languagesList: null,
        systemSettings: null,
        timeZone: null,
        timeZonesList: null,
        shieldSettings: null
    },
    formulas: {
        timeSource: function (get) {
            return get('systemSettings.timeSource') === 'manual' ? 'Time was set manually'.t() : 'Time is automatically synchronized via NTP'.t();
        },
        manualDate: {
            get: function (get) {
                // to fix because rpc.systemManager.getDate() returns an invalid date string
                return get('time') ? new Date(get('time').replace('EET', '(EET)')) : new Date();
            },
            set: function (val) {
                return;
            }
        },
        // used for setting the date/time
        manualDateFormat: function (get) { return get('languageSettings.overrideTimestampFmt') || 'timestamp_fmt'.t(); },
        dateFormat: {
            get: function (get) {
                var fmt = get('languageSettings.overrideDateFmt');
                return fmt.length === 0 ? 'Y-m-d' : fmt;
            },
            set: function (value) {
                var fmt = value + ' ' + this.get('timeFormat');
                this.set('languageSettings.overrideDateFmt', value === 'Y-m-d' ? '' : value);
                this.set('languageSettings.overrideTimestampFmt', fmt === 'Y-m-d h:i:s a' ? '' : fmt);
            }
        },
        timeFormat: {
            get: function (get) {
                var tsFmt = get('languageSettings.overrideTimestampFmt');
                return tsFmt.length > 0 ? tsFmt.substring(6) : 'h:i:s a';
            },
            set: function (value) {
                var dateFmt = this.get('languageSettings.overrideDateFmt'),
                    fmt = (dateFmt.length === 0 ? 'Y-m-d' : dateFmt) + ' ' + value;
                this.set('languageSettings.overrideTimestampFmt', fmt === 'Y-m-d h:i:s a' ? '' : fmt);
            }
        },
        lastLanguageSync: function (get) {
            // todo: to update setting new date based on timeoffsets
            var ts = get('languageSettings.lastSynchronized');
            return ts ? new Date(ts) : 'Never'.t();
        }
    },
    stores: {
        timeZones: {
            fields: ['name', 'value'],
            data: '{timeZonesList}'
        },
        languages: {
            fields: ['code', 'name', 'statistics', {
                name: 'cc',
                calculate: function (r) {
                    return r.code ? r.code.split('-')[1] : r.code;
                }
            }],
            data: '{languagesList.list}'
        },
        shieldRules: {
            data: '{shieldSettings.rules.list}'
        }
    }
});
Ext.define('Ung.config.system.view.Backup', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.backup',
    viewModel: true,
    title: 'Backup'.t(),
    bodyPadding: 10,
    scrollable: true,
    defaults: {
        xtype: 'fieldset',
        padding: 10
    },
    items: [{
        title: 'Backup to File'.t(),
        items: [{
            xtype: 'component',
            html: 'Backup can save the current system configuration to a file on your local computer for later restoration. The file name will end with .backup'.t() +
                  '<br />' +
                  'After backing up your current system configuration to a file, you can then restore that configuration through this dialog by clicking on Restore from File.'.t()
        }, {
            xtype: 'button',
            margin: '10 0 0 0',
            text: 'Backup to File'.t(),
            handler: 'backupToFile'
        }]
    }]
});
Ext.define('Ung.config.system.view.Protocols', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.protocols',
    itemId: 'protocols',
    viewModel: {
        formulas: {
            smtpTimeout: {
                get: function (get) {
                    return get('smtpSettings.smtpTimeout') / 1000;
                },
                set: function (value) {
                    this.set('smtpSettings.smtpTimeout', value * 1000);
                }
            }
        }
    },
    title: 'Protocols'.t(),
    bodyPadding: 10,
    scrollable: true,
    defaults: {
        xtype: 'fieldset',
        padding: 10
    },
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> '  + 'These settings should not be changed unless instructed to do so by support.'.t()
    }],
    items: [{
        title: 'HTTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!httpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{httpSettings.enabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of HTTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of HTTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }, {
            xtype: 'checkbox',
            boxLabel: 'Log Referer in HTTP events.'.t(),
            bind: '{httpSettings.logReferer}'
        }]
    }, {
        title: 'FTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!ftpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{ftpSettings.enabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of FTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of FTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }]
    }, {
        title: 'SMTP'.t(),
        disabled: true,
        bind: {
            disabled: '{!smtpSettings}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{smtpSettings.smtpEnabled}',
            items: [{
                boxLabel: '<strong>' + 'Enable processing of SMTP traffic.  (This is the default setting)'.t() + '</strong>',
                inputValue: true
            }, {
                boxLabel: '<strong>' + 'Disable processing of SMTP traffic.'.t() + '</strong>',
                inputValue: false
            }]
        }, {
            xtype: 'numberfield',
            fieldLabel: 'SMTP timeout (seconds)'.t(),
            labelAlign: 'top',
            allowDecimals: false,
            allowNegative: false,
            minValue: 0,
            maxValue: 86400,
            bind: '{smtpTimeout}'
        }, {
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            items: [{
                boxLabel: 'Allow TLS encryption over SMTP.'.t(),
                inputValue: true
            }, {
                boxLabel: 'Stop TLS encryption over SMTP.'.t(),
                inputValue: false
            }]
        }]
    }]
});
Ext.define('Ung.config.system.view.Regional', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.regional',
    itemId: 'regional',
    viewModel: true,
    scrollable: true,
    title: 'Regional'.t(),
    bodyPadding: 10,
    defaults: {
        xtype: 'fieldset',
        padding: 10
    },
    items: [{
        title: 'Current Time'.t(),
        hidden: true,
        bind: {
            hidden: '{!time}'
        },
        items: [{
            xtype: 'component',
            bind: '{timeSource}'
        }, {
            xtype: 'component',
            margin: '10 0 0 0',
            bind: '<i class="fa fa-clock-o"></i> <strong>{time}</strong>'
        }]
    }, {
        title: 'Force Sync Time'.t(),
        hidden: true,
        bind: {
            hidden: '{isExpertMode || systemSettings.timeSource === "manual" || !time}'
        },
        layout: {
            type: 'hbox'
        },
        magin: 5,
        items: [{
            xtype: 'button',
            margin: '0 10 0 0',
            text: 'Synchronize Time'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'syncTime'
        }, {
            xtype: 'displayfield',
            value: 'Click to force instant time synchronization.'.t()
        }]
    }, {
        title: 'Time Settings'.t(),
        hidden: true,
        bind: {
            hidden: '{!(isExpertMode || systemSettings.timeSource === "manual" || !time)}'
        },
        items: [{
            xtype: 'radiogroup',
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{systemSettings.timeSource}',
            items: [{
                boxLabel: '<strong>' + 'Synchronize time automatically via NTP'.t() + '</strong>',
                inputValue: 'ntp'
            }, {
                xtype: 'fieldset',
                margin: 5,
                border: false,
                disabled: true,
                hidden: true,
                layout: {
                    type: 'hbox'
                },
                bind: {
                    disabled: '{systemSettings.timeSource !== "ntp"}',
                    hidden: '{systemSettings.timeSource !== "ntp"}'
                },
                items: [{
                    xtype: 'button',
                    margin: '0 10 0 0',
                    text: 'Synchronize Time'.t(),
                    iconCls: 'fa fa-refresh',
                    handler: 'syncTime'
                }, {
                    xtype: 'displayfield',
                    value: 'Click to force instant time synchronization.'.t()
                }]
            }, {
                boxLabel: '<strong>' + 'Set system clock manually'.t() + '</strong>',
                inputValue: 'manual'
            }, {
                xtype: 'datefield',
                width: 180,
                margin: '5 25',
                disabled: true,
                hidden: true,
                value: new Date(),
                bind: {
                    value: '{manualDate}',
                    format: '{manualDateFormat}',
                    disabled: '{systemSettings.timeSource !== "manual"}',
                    hidden: '{systemSettings.timeSource !== "manual"}'
                }
            }]
        }]
    }, {
        title: 'Timezone'.t(),
        hidden: true,
        bind: {
            hidden: '{!timeZonesList || !timeZone}'
        },
        items: [{
            xtype: 'combo',
            width: 350,
            bind: {
                store: '{timeZones}',
                value: '{timeZone.ID}'
            },
            displayField: 'name',
            valueField: 'value',
            editable: false,
            queryMode: 'local'
        }]
    }, {
        title: 'Language'.t(),
        hidden: true,
        bind: {
            hidden: '{!languagesList || !languageSettings}'
        },
        items: [{
            xtype: 'combo',
            width: 350,
            bind: {
                store: '{languages}',
                value: '{languageSettings.language}'
            },
            displayField: 'name',
            valueField: 'cc',
            editable: false,
            queryMode: 'local',
            listConfig:{
                getInnerTpl: function() {
                    return '<div data-qtip="{statistics}">{name}</div>';
                }
            }
        }, {
            xtype: 'fieldset',
            title: 'Regional Formats'.t(),
            border: 0,
            margin: 10,
            items: [{
                xtype: 'radiogroup',
                columns: 1,
                vertical: true,
                simpleValue: true,
                bind: '{languageSettings.regionalFormats}',
                items: [{
                    boxLabel: 'Use defaults'.t(),
                    inputValue: 'default'
                }, {
                    boxLabel: 'Override'.t(),
                    inputValue: 'override'
                }]
            }, {
                xtype: 'fieldset',
                disabled: true,
                hidden: true,
                border: false,
                bind: {
                    disabled: '{languageSettings.regionalFormats === "default"}',
                    hidden: '{languageSettings.regionalFormats === "default"}'
                },
                defaults: {
                    xtype: 'combo',
                    labelAlign: 'right',
                    labelWidth: 150
                },
                items: [{
                    fieldLabel: 'Decimal Separator'.t(),
                    bind: '{languageSettings.overrideDecimalSep}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['', '. (DOT)'], // DOT is by default so value is set as ''
                        [',', ', (COMMA)'],
                        [' ', '&nbsp; (SPACE)'],
                        ['\'', '\' (APOSTROPHE)'],
                        ['&middot;', '&middot; (MIDDLE DOT)'],
                        ['&#729;', '&#729; (DOT ABOVE)']
                    ]
                }, {
                    fieldLabel: 'Thousand Separator'.t(),
                    bind: '{languageSettings.overrideThousandSep}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['.', '. (DOT)'],
                        ['', ', (COMMA)'], // COMMA is by default so value is set as ''
                        [' ', '&nbsp; (SPACE)'],
                        ['\'', '\' (APOSTROPHE)'],
                        ['&middot;', '&middot; (MIDDLE DOT)'],
                        ['&#729;', '&#729; (DOT ABOVE)']
                    ],
                }, {
                    fieldLabel: 'Date Format'.t(),
                    width: 400,
                    bind: '{dateFormat}',
                    // bind: '{dateFormat}',
                    queryMode: 'local',
                    editable: false,
                    store: [
                        ['Y-m-d', 'Y-m-d (e.g. 2017-05-23) - default'], // 'Y-m-d' is by default
                        ['m-d-Y', 'm-d-Y (e.g. 05-23-2017)'],
                        ['d-m-Y', 'd-m-Y (e.g. 23-05-2017)'],
                        ['Y/m/d', 'Y/m/d (e.g. 2017/05/23)'],
                        ['m/d/Y', 'm/d/Y (e.g. 05/23/2017)'],
                        ['d/m/Y', 'd/m/Y (e.g. 23/05/2017)'],
                        ['d.m.Y', 'd.m.Y (e.g. 23.05.2017)']
                    ]
                }, {
                    fieldLabel: 'Time Format'.t(),
                    width: 400,
                    bind: '{timeFormat}',
                    store: [
                        ['h:i:s a', 'h:i:s a (e.g. 06:45:35 PM) - default'],
                        ['G:i:s', 'G:i:s (e.g. 18:45:35)']
                    ],
                    queryMode: 'local',
                    editable: false
                }]
            }]
        }, {
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'button',
                margin: '0 10 0 0',
                text: 'Synchronize Language'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'syncLanguage'
            }, {
                xtype: 'displayfield',
                bind: '<strong>' + 'Last synchronized'.t() + '</strong>: {lastLanguageSync}'
                // fieldLabel: ,
                // bind: '{languageSettings.lastSynchronized}'
            }]
        }]
    }]
});
Ext.define('Ung.config.system.view.Restore', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.restore',
    viewModel: true,
    title: 'Restore'.t(),
    bodyPadding: 10,
    scrollable: true,
    defaults: {
        xtype: 'fieldset',
        padding: 10
    },
    items: [{
        title: 'Restore from File'.t(),
        items: [{
            xtype: 'component',
            html: 'Restore can restore a previous system configuration to the server from a backup file on your local computer.  The backup file name ends with .backup'.t()
        }, {
            xtype: 'form',
            padding: 10,
            margin: 10,
            border: false,
            url: 'upload',
            defaults: {
                labelAlign: 'right',
                labelWidth: 150
            },
            items: [{
                xtype: 'combo',
                fieldLabel: 'Restore Options'.t(),
                width: 500,
                store: [['', 'Restore all settings.'.t()], ['.*/network.*', 'Restore all except keep current network settings.'.t()]],
                value: '',
                queryMode: 'local',
                allowBlank: false,
                editable: false
            }, {
                xtype: 'filefield',
                margin: '10 0 0 0',
                fieldLabel: 'File'.t(),
                name: 'file',
                width: 500,
                allowBlank: false,
                validateOnBlur: false
            }, {
                xtype: 'button',
                margin: '10 0 0 155',
                text: 'Restore from File'.t(),
                handler: 'restoreFromFile'
            }, {
                xtype: 'hidden',
                name: 'type',
                value: 'restore'
            }]
        }]
    }]
});
Ext.define('Ung.config.system.view.Shield', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.shield',
    itemId: 'shield',
    viewModel: true,
    title: 'Shield'.t(),
    layout: 'fit',
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        padding: '8 5',
        style: { fontSize: '12px' },
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + 'Enable Shield'.t() + '</strong>',
            bind: '{shieldSettings.shieldEnabled}'
        }]
    }],
    items: [{
        xtype: 'ungrid',
        border: false,
        title: 'Shield Rules'.t(),
        disabled: true,
        bind: {
            disabled: '{!shieldSettings.shieldEnabled}',
            store: '{shieldRules}'
        },
        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'shieldSettings.rules.list',
        ruleJavaClass: 'com.untangle.node.shield.ShieldRuleCondition',
        emptyRow: {
            ruleId: -1,
            enabled: true,
            description: '',
            action: 'SCAN',
            javaClass: 'com.untangle.node.shield.ShieldRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            }
        },
        conditions: [Cond.dstAddr, Cond.dstPort, Cond.dstIntf, Cond.srcAddr, Cond.srcPort, Cond.srcIntf,
            Cond.protocol([['TCP','TCP'],['UDP','UDP']])
        ],
        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description'.t(),
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'Action'.t(),
            width: 150,
            dataIndex: 'action',
            renderer: function (value) {
                var action;
                switch (value) {
                case 'SCAN': action = 'Scan'.t(); break;
                case 'PASS': action = 'Pass'.t(); break;
                default: action = 'Unknown Action: ' + value;
                }
                return action;
            }
        }],
        editorFields: [
            Fields.enableRule(),
            Fields.description,
            Fields.conditions, {
                xtype: 'combo',
                fieldLabel: 'Action',
                allowBlank: false,
                editable: false,
                bind: '{record.action}',
                store: [
                    ['SCAN', 'Scan'.t()],
                    ['PASS', 'Pass'.t()]
                ],
                queryMode: 'local'
            }
        ]
    }]
});
Ext.define('Ung.config.system.view.Support', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.system.support',
    viewModel: true,
    title: 'Support'.t(),
    bodyPadding: 10,
    scrollable: true,
    defaults: {
        xtype: 'fieldset',
        padding: 10
    },
    items: [{
        title: 'Support'.t(),
        items: [{
            xtype: 'checkbox',
            boxLabel: Ext.String.format('Connect to {0} cloud'.t(), this.oemName),
            bind: '{systemSettings.cloudEnabled}'
        }, {
            xtype: 'checkbox',
            boxLabel: Ext.String.format('Allow secure remote access to {0} support team'.t(), this.oemName),
            bind: '{systemSettings.supportEnabled}'
        }]
    }, {
        title: 'Logs'.t(),
        items: [{
            xtype: 'button',
            text: 'Download System Logs'.t(),
            handler: 'downloadSystemLogs',
            iconCls: 'fa fa-download'
        }]
    }, {
        title: 'Manual Reboot'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Reboot the server.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Reboot'.t(),
                iconCls: 'fa fa-refresh',
                handler: 'manualReboot'
            }]
        }]
    }, {
        title: 'Manual Shutdown'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Power off the server.'.t(),
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Shutdown'.t(),
                iconCls: 'fa fa-power-off',
                handler: 'manualShutdown'
            }]
        }]
    }, {
        title: 'Setup Wizard'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Launch the Setup Wizard.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Setup Wizard'.t(),
                iconCls: 'fa fa-magic',
                handler: 'setupWizard'
            }]
        }]
    }, {
        title: 'Factory Defaults'.t(),
        items: [{
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            items: [{
                xtype: 'displayfield',
                value: 'Reset all settings to factory defaults.'.t()
            }, {
                xtype: 'button',
                margin: '0 0 0 10',
                text: 'Reset to Factory Defaults'.t(),
                iconCls: 'fa fa-industry',
                handler: 'factoryDefaults'
            }]
        }]
    }]
});