if (!Ung.hasResource["Ung.System"]) {
    Ung.hasResource["Ung.System"] = true;

    Ung.System = Ext.extend(Ung.ConfigWin, {
        panelUntangleSupport : null,
        panelBackup : null,
        panelRestore : null,
        panelProtocolSettings : null,
        panelRegionalSettings : null,
        initComponent : function() {
            this.breadcrumbs = [{
                title : i18n._("Configuration"),
                action : function() {
                    this.cancelAction();
                }.createDelegate(this)
            }, {
                title : i18n._('System')
            }];
            Ung.System.superclass.initComponent.call(this);
        },

        onRender : function(container, position) {
            // call superclass renderer first
            Ung.System.superclass.onRender.call(this, container, position);
            this.initSubCmps.defer(1, this);
            // builds the 2 tabs
        },
        initSubCmps : function() {
            this.buildUntangleSupport();
            this.buildBackup();
            this.buildRestore();
            this.buildProtocolSettings();
            this.buildRegionalSettings();
            // builds the tab panel with the tabs
            this.buildTabPanel([this.panelUntangleSupport, this.panelBackup, this.panelRestore, this.panelProtocolSettings,
                    this.panelRegionalSettings]);
            this.tabs.activate(this.panelUntangleSupport);
            this.panelRestore.disable();
            this.panelProtocolSettings.disable();

        },
        // get languange settings object
        getLanguageSettings : function(forceReload) {
            if (forceReload || this.rpc.languageSettings === undefined) {
                this.rpc.languageSettings = rpc.languageManager.getLanguageSettings();
            }
            return this.rpc.languageSettings;
        },
        getAccessSettings : function(forceReload) {
            if (forceReload || this.rpc.accessSettings === undefined) {
                this.rpc.accessSettings = rpc.networkManager.getAccessSettings();
            }
            return this.rpc.accessSettings;
        },
        getMiscSettings : function(forceReload) {
            if (forceReload || this.rpc.miscSettings === undefined) {
                this.rpc.miscSettings = rpc.networkManager.getMiscSettings();
            }
            return this.rpc.miscSettings;
        },
        getTimeZone : function(forceReload) {
            if (forceReload || this.rpc.timeZone === undefined) {
                this.rpc.timeZone = rpc.adminManager.getTimeZone();
            }
            return this.rpc.timeZone;
        },
        getTODOPanel : function(title) {
            return new Ext.Panel({
                title : this.i18n._(title),
                layout : "form",
                autoScroll : true,
                bodyStyle : 'padding:5px 5px 0px 5px',
                items : [{
                    xtype : 'fieldset',
                    title : this.i18n._(title),
                    autoHeight : true,
                    items : [{
                        xtype : 'textfield',
                        fieldLabel : 'TODO',
                        name : 'todo',
                        allowBlank : false,
                        value : 'todo',
                        disabled : true
                    }]
                }]
            });
        },
        buildUntangleSupport : function() {
            this.panelUntangleSupport = new Ext.Panel({
                // private fields
                name : 'panelUntangleSupport',
                parentId : this.getId(),
                title : this.i18n._('Untangle Support'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Support'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        xtype : 'checkbox',
                        name : 'isSupportEnabled',
                        boxLabel : this.i18n._('<b>Allow</b> us to securely access your server for support purposes.'),
                        hideLabel : true,
                        checked : this.getAccessSettings().isSupportEnabled,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getAccessSettings().isSupportEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }, {
                        xtype : 'checkbox',
                        name : 'isSupportEnabled',
                        boxLabel : this.i18n
                                ._('<b>Send</b> us data about your server. This will send us status updates and an email if any unexpected problems occur, but will not allow us to login to your server. No personal information about your network traffic will be transmitted.'),
                        hideLabel : true,
                        checked : this.getMiscSettings().isExceptionReportingEnabled,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getMiscSettings().isExceptionReportingEnabled = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }]
            });

        },
        buildBackup : function() {
            this.panelBackup = new Ext.Panel({
                // private fields
                name : 'panelBackup',
                parentId : this.getId(),
                title : this.i18n._('Backup'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Backup to File'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                    	html: "test"
                    }]
                },{
                    title : this.i18n._('Backup to USB Key'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: "test"
                    }]
                },{
                    title : this.i18n._('Backup to Hard Disk'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px;'
                    },
                    items : [{
                        html: "test"
                    }]
                }]
            });

        },
        buildRestore : function() {
            this.panelRestore = this.getTODOPanel("Restore");
        },
        buildProtocolSettings : function() {
            this.panelProtocolSettings = this.getTODOPanel("Protocol Settings");
        },
        buildRegionalSettings : function() {
            var languagesStore = new Ext.data.Store({
                proxy : new Ung.RpcProxy(rpc.languageManager.getLanguagesList, false),
                reader : new Ext.data.JsonReader({
                    root : 'list',
                    fields : ['code', 'name']
                })
            });
            var timeZonesData = [
                    ["GMT-12:00", "International Dateline West", "Etc/GMT-12"],
                    ["GMT-11:00", "Midway Island, Samoa", "Pacific/Midway"],
                    ["GMT-10:00", "Hawaii", "US/Hawaii"],
                    ["GMT-09:00", "Alaska", "US/Alaska"],
                    ["GMT-08:00", "Pacific Time (US & Canada), Tijuana", "US/Pacific"],
                    ["GMT-07:00", "Chihuahua, La Paz, Mazatlan", "America/Chihuahua"],
                    ["GMT-07:00", "Mountain Time (US & Canada)", "US/Mountain"],
                    ["GMT-06:00", "Central Time (US & Canada & Central America)", "US/Central"],
                    ["GMT-06:00", "Guadalajara, Mexico City, Monterrey", "America/Mexico_City"],
                    ["GMT-06:00", "Saskatchewan", "Canada/Saskatchewan"],
                    ["GMT-05:00", "Bogota, Lima, Quito", "America/Bogota"],
                    ["GMT-05:00", "Eastern Time (US & Canada)", "US/Eastern"],
                    ["GMT-05:00", "Indiana (East)", "US/East-Indiana"],
                    ["GMT-04:00", "Atlantic Time (Canada)", "Canada/Atlantic"],
                    ["GMT-04:00", "Caracas, La Paz", "America/Caracas"],
                    ["GMT-04:00", "Santiago", "America/Santiago"],
                    ["GMT-03:30", "Newfoundland", "Canada/Newfoundland"],
                    ["GMT-03:00", "Buenos Aires, Georgetown, Brasilia, Greenland", "America/Buenos_Aires"],
                    ["GMT-02:00", "Mid-Atlantic", "Etc/GMT-2"],
                    ["GMT-01:00", "Azores", "Atlantic/Azores"],
                    ["GMT-01:00", "Cape Verde Is.", "Atlantic/Cape_Verde"],
                    ["GMT", "Casablanca, Monrovia", "Africa/Casablanca"],
                    ["GMT", "Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London", "Etc/Greenwich"],
                    ["GMT+01:00", "Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna", "Europe/Amsterdam"],
                    ["GMT+01:00", "Belgrade, Bratislava, Budapest, Ljubljana, Prague", "Europe/Belgrade"],
                    ["GMT+01:00", "Brussels, Copenhagen, Madrid, Paris", "Europe/Brussels"],
                    ["GMT+01:00", "Sarajevo, Skopje, Warsaw, Zagreb", "Europe/Sarajevo"],
                    ["GMT+01:00", "West Central Africa", "Etc/GMT+1"],
                    ["GMT+02:00", "Athens, Beirut, Istanbul, Minsk", "Europe/Athens"],
                    ["GMT+02:00", "Bucharest", "Europe/Bucharest"],
                    ["GMT+02:00", "Cairo, Harare, Pretoria", "Africa/Cairo"],
                    ["GMT+02:00", "Helsinki, Kyiv, Riga, Sofia, Talinn, Vilnius", "Europe/Helsinki"],
                    ["GMT+02:00", "Jerusalem", "Asia/Jerusalem"],
                    ["GMT+03:00", "Baghdad", "Asia/Baghdad"],
                    ["GMT+03:00", "Kuwait, Riyadh", "Asia/Kuwait"],
                    ["GMT+03:00", "Moscow, St. Petersburg, Volgograd", "Europe/Moscow"],
                    ["GMT+03:00", "Nairobi", "Africa/Nairobi"],
                    ["GMT+03:30", "Tehran", "Asia/Tehran"],
                    ["GMT+04:00", "Abu Dhabi, Muscat", "Asia/Muscat"],
                    ["GMT+04:00", "Baku, Tbilsi, Yerevan", "Asia/Baku"],
                    ["GMT+04:30", "Kabul", "Asia/Kabul"],
                    ["GMT+05:00", "Ekaterinburg, Islamabad, Karachi, Tashkent", "Asia/Karachi"],
                    ["GMT+05:30", "Chennai, Kolkata, Mumbai, New Delhi", "Asia/Calcutta"], // NOTICE
                                                                                            // TWO
                                                                                            // SPELLINGS
                                                                                            // OF
                                                                                            // KOLKATA/CALCUTTA
                    ["GMT+05:45", "Kathmandu", "Asia/Katmandu"], // NOTICE
                                                                    // TWO
                                                                    // SPELLINGS
                                                                    // OF
                                                                    // KATMANDU/KATHMANDU
                    ["GMT+06:00", "Almaty, Novosibirsk", "Asia/Almaty"], ["GMT+06:00", "Astana, Dhaka", "Asia/Dhaka"],
                    ["GMT+06:30", "Rangoon", "Asia/Rangoon"], ["GMT+07:00", "Bangkok, Hanoi, Jakarta", "Asia/Bangkok"],
                    ["GMT+07:00", "Krasnoyarsk", "Asia/Krasnoyarsk"],
                    ["GMT+08:00", "Beijing, Chongqing, Hong Kong, Urumqi", "Asia/Hong_Kong"],
                    ["GMT+08:00", "Irkutsk, Ulaan Bataar", "Asia/Irkutsk"], ["GMT+08:00", "Kuala Lumpur, Singapore", "Asia/Kuala_Lumpur"],
                    ["GMT+08:00", "Perth", "Australia/Perth"], ["GMT+08:00", "Taipei", "Asia/Taipei"],
                    ["GMT+09:00", "Osaka, Sapporo, Tokyo", "Asia/Tokyo"], ["GMT+09:00", "Seoul", "Asia/Seoul"],
                    ["GMT+09:00", "Yakutsk", "Asia/Yakutsk"], ["GMT+09:30", "Adelaide", "Australia/Adelaide"],
                    ["GMT+09:30", "Darwin", "Australia/Darwin"], ["GMT+10:00", "Brisbane", "Australia/Brisbane"],
                    ["GMT+10:00", "Canberra, Melbourne, Sydney", "Australia/Canberra"],
                    ["GMT+10:00", "Guam, Port Moresby", "Pacific/Guam"], ["GMT+10:00", "Hobart", "Australia/Hobart"],
                    ["GMT+10:00", "Vladivostok", "Asia/Vladivostok"], ["GMT+11:00", "Magadan, Solomon Is., New Caledonia", "Asia/Magadan"],
                    ["GMT+12:00", "Auckland, Wellington", "Pacific/Auckland"],
                    ["GMT+12:00", "Fiji, Kamchatka, Marshall Is.", "Pacific/Fiji"]];
            var timeZones = [];
            for (var i = 0; i < timeZonesData.length; i++) {
                timeZones.push([timeZonesData[i][2], "(" + timeZonesData[i][0] + ") " + timeZonesData[i][1]]);
            }
            this.panelRegionalSettings = new Ext.Panel({
                // private fields
                name : 'panelRegionalSettings',
                parentId : this.getId(),
                title : this.i18n._('Regional Settings'),
                layout : "form",
                bodyStyle : 'padding:5px 5px 0px 5px;',
                autoScroll : true,
                defaults : {
                    xtype : 'fieldset',
                    autoHeight : true,
                    buttonAlign : 'left'
                },
                items : [{
                    title : this.i18n._('Current Time'),
                    defaults : {
                        border : false,
                        bodyStyle : 'padding:5px 5px 0px 5px'
                    },
                    items : [{
                        html : this.i18n._('time is automatically synced via NTP')
                    }, {
                        html : this.i18n.timestampFormat(rpc.adminManager.getDate())
                    }]
                }, {
                    title : this.i18n._('Timezone'),
                    items : [{
                        xtype : 'combo',
                        name : 'timezone',
                        editable : false,
                        store : timeZones,
                        width : 350,
                        hideLabel : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        value : this.getTimeZone(),
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.rpc.timeZone = newValue;
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Language'),
                    items : [{
                        id : 'system_language_combo',
                        xtype : 'combo',
                        name : "language",
                        store : languagesStore,
                        forceSelection : true,
                        displayField : 'name',
                        valueField : 'code',
                        typeAhead : true,
                        mode : 'local',
                        triggerAction : 'all',
                        listClass : 'x-combo-list-small',
                        selectOnFocus : true,
                        hideLabel : true,
                        listeners : {
                            "change" : {
                                fn : function(elem, newValue) {
                                    this.getLanguageSettings().language = newValue;
                                    Ext.MessageBox.alert(this.i18n._("Info"), this.i18n
                                            ._("Please note that you have to relogin after saving for the new language to take effect."));
                                }.createDelegate(this)
                            },
                            "render" : {
                                fn : function(elem) {
                                    languagesStore.load({
                                        callback : function(r, options, success) {
                                            if (success) {
                                                var languageComboCmp = Ext.getCmp("system_language_combo");
                                                if (languageComboCmp) {
                                                    languageComboCmp.setValue(this.getLanguageSettings().language);
                                                }
                                            }
                                        }.createDelegate(this)
                                    })
                                }.createDelegate(this)
                            }
                        }
                    }]
                }, {
                    title : this.i18n._('Upload New Language Pack'),
                    items : {
                        fileUpload : true,
                        xtype : 'form',
                        id : 'upload_language_form',
                        url : 'upload',
                        border : false,
                        items : [{
                            fieldLabel : 'File',
                            name : 'file',
                            inputType : 'file',
                            xtype : 'textfield',
                            allowBlank : false
                        }, {
                            xtype : 'hidden',
                            name : 'type',
                            value : 'language'
                        }]
                    },
                    buttons : [{
                        text : this.i18n._("Upload"),
                        handler : function() {
                            this.panelRegionalSettings.onUpload();
                        }.createDelegate(this)
                    }]
                }],
                onUpload : function() {
                    var prova = Ext.getCmp('upload_language_form');
                    var cmp = Ext.getCmp(this.parentId);

                    var form = prova.getForm();
                    form.submit({
                        parentId : cmp.getId(),
                        waitMsg : cmp.i18n._('Please wait while your language pack is uploaded...'),
                        success : function(form, action) {
                            languagesStore.load();
                            var cmp = Ext.getCmp(action.options.parentId);
                            Ext.MessageBox.alert(cmp.i18n._("Succeeded"), cmp.i18n._("Upload Language Pack Succeeded"));
                        },
                        failure : function(form, action) {
                            var cmp = Ext.getCmp(action.options.parentId);
                            if (action.result && action.result.msg) {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._(action.result.msg));
                            } else {
                                Ext.MessageBox.alert(cmp.i18n._("Failed"), cmp.i18n._("Upload Language Pack Failed"));
                            }
                        }
                    });
                }
            });

        },
        // save function
        saveAction : function() {
            if (this.validate()) {
                this.saveSemaphore = 3;
                // save language settings
                rpc.languageManager.setLanguageSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getLanguageSettings());
                
                // save network settings
                rpc.networkManager.setSettings(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.getAccessSettings(), this.getMiscSettings());
                
                //save timezone
                rpc.adminManager.setTimeZone(function(result, exception) {
                    if (exception) {
                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
                        return;
                    }
                    this.afterSave();
                }.createDelegate(this), this.rpc.timeZone);
            }
        },
        afterSave : function() {
            this.saveSemaphore--;
            if (this.saveSemaphore == 0) {
                this.cancelAction();
            }
        }

    });

}