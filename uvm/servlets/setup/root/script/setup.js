/*global
 Ext, Ung, Webui, rpc:true, i18n:true, JSONRpcClient, console, window
 */

// the main json rpc object
var rpc = {};

Ext.define('Ung.setupWizard', {
    statics: {
        labelWidth: 160,
        v4NetmaskList: [
            [32, '/32 - 255.255.255.255'],
            [31, '/31 - 255.255.255.254'],
            [30, '/30 - 255.255.255.252'],
            [29, '/29 - 255.255.255.248'],
            [28, '/28 - 255.255.255.240'],
            [27, '/27 - 255.255.255.224'],
            [26, '/26 - 255.255.255.192'],
            [25, '/25 - 255.255.255.128'],
            [24, '/24 - 255.255.255.0'],
            [23, '/23 - 255.255.254.0'],
            [22, '/22 - 255.255.252.0'],
            [21, '/21 - 255.255.248.0'],
            [20, '/20 - 255.255.240.0'],
            [19, '/19 - 255.255.224.0'],
            [18, '/18 - 255.255.192.0'],
            [17, '/17 - 255.255.128.0'],
            [16, '/16 - 255.255.0.0'],
            [15, '/15 - 255.254.0.0'],
            [14, '/14 - 255.252.0.0'],
            [13, '/13 - 255.248.0.0'],
            [12, '/12 - 255.240.0.0'],
            [11, '/11 - 255.224.0.0'],
            [10, '/10 - 255.192.0.0'],
            [9, '/9 - 255.128.0.0'],
            [8, '/8 - 255.0.0.0'],
            [7, '/7 - 254.0.0.0'],
            [6, '/6 - 252.0.0.0'],
            [5, '/5 - 248.0.0.0'],
            [4, '/4 - 240.0.0.0'],
            [3, '/3 - 224.0.0.0'],
            [2, '/2 - 192.0.0.0'],
            [1, '/1 - 128.0.0.0'],
            [0, '/0 - 0.0.0.0']
        ]
    }
});

// Setup Wizard - Welcome
Ext.define('Ung.setupWizard.Welcome', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.showResume = false;
        var items, continueStep = '';
        if (!rpc.wizardSettings.wizardComplete && rpc.wizardSettings.completedStep != null) {
            var completedStepIndex = rpc.wizardSettings.steps.indexOf(rpc.wizardSettings.completedStep);
            if (completedStepIndex > 0 && completedStepIndex < rpc.wizardSettings.steps.length - 1) {
                this.showResume = true;
                continueStep = rpc.wizardSettings.steps[completedStepIndex + 1];
            }
        }
        var initialConfig = {
            xtype: 'component',
            margin: '20 0 0 20',
            html: '<h3>' + Ext.String.format(i18n._('Thanks for choosing {0}!'), rpc.oemName) + '</h3>' +
                '<p>' + Ext.String.format(i18n._('This wizard will guide you through the initial setup and configuration of the {0} Server.'), rpc.oemName) + '</p>'
        };
        if (!this.showResume) {
            items = [initialConfig];
        } else {
            items = [{
                xtype: 'container',
                margin: '20 0 0 20',
                items: [{
                    xtype: 'radio',
                    name: 'restartWizardRadio',
                    inputValue: 'yes',
                    boxLabel: i18n._('Continue the Setup Wizard'),
                    cls: 'large-option',
                    hideLabel: 'true',
                    checked: true,
                    listeners: {
                        'change': {
                            fn: function (elem, checked) {
                                this.panel.down('field[name="login"]').setDisabled(!checked);
                                this.panel.down('field[name="password"]').setDisabled(!checked);
                            },
                            scope: this
                        }
                    }
                }, {
                    xtype: 'component',
                    margin: '0 0 0 20',
                    html: Ext.String.format(i18n._('The setup was started before and the last completed step is {0}{1}{2}.'), '<b>', Ung.Setup.stepsTitlesMap[rpc.wizardSettings.completedStep], '</b>')
                }, {
                    xtype: 'displayfield',
                    inputType: 'text',
                    fieldLabel: i18n._('Login'),
                    name: 'login',
                    value: 'admin',
                    labelWidth: 150,
                    style: {marginTop: '10px'}
                }, {
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Password'),
                    name: 'password',
                    msgTarget: 'side',
                    labelWidth: 150,
                    allowBlank: false
                }, {
                    xtype: 'component',
                    margin: '10 0 0 20',
                    html: Ext.String.format(i18n._('To continue with step {1}{0}{2} fill the admin password and press the {1}Next{2}.'), Ung.Setup.stepsTitlesMap[continueStep], '<b>', '</b>')
                }]
            }, {
                xtype: 'container',
                margin: '40 0 0 20',
                items: [{
                    xtype: 'radio',
                    name: 'restartWizardRadio',
                    inputValue: 'no',
                    boxLabel: i18n._('Restart the Setup Wizard'),
                    cls: 'large-option',
                    hideLabel: 'true'
                }, initialConfig]
            }];
        }
        this.panel = Ext.create('Ext.container.Container', {
            items: items
        });
        this.card = {
            title: i18n._('Welcome'),
            panel: this.panel,
            onNext: Ext.bind(function (handler) {
                var resumeWizard = this.showResume && (this.panel.down('radio[name="restartWizardRadio"]').getGroupValue() == 'yes');
                if (resumeWizard) {
                    var password = this.panel.down('field[name="password"]').getValue();
                    var afterFn = Ext.bind(function(handler){
                        this.panel.ownerCt.ownerCt.controller.loadPage(rpc.wizardSettings.steps.indexOf(rpc.wizardSettings.completedStep) + 1);
                    },this, [handler]);
                    Ung.Setup.authenticate(password, afterFn);
                } else {
                    handler();
                }
            }, this),
            onValidate: Ext.bind(function () {
                return Ung.Util.validate(this.panel);
            }, this)
        };
    }
});

// Setup Wizard - Step 1 (Password and Timezone)
Ext.define('Ung.setupWizard.ServerSettings', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.panel = Ext.create('Ext.container.Container', {
            defaults: {
                margin: '20 0 0 20'
            },
            items: [{
                xtype: 'container',
                defaults: {
                    msgTarget: 'side',
                    validationEvent: 'blur',
                    labelWidth: 150
                },
                items: [{
                    xtype: 'component',
                    html: '<h3>' + i18n._('Configure the Server') + '</h3>'
                }, {
                    xtype: 'component',
                    style: {
                        marginBottom: '10px'
                    },
                    html: '<b>' + i18n._('Choose a password for the admin account.') + '</b>'
                }, {
                    xtype: 'displayfield',
                    inputType: 'text',
                    fieldLabel: i18n._('Login'),
                    name: 'login',
                    value: 'admin',
                    readOnly: true
                }, {
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Password'),
                    name: 'password',
                    id: 'settings_password',
                    allowBlank: false,
                    minLength: 3,
                    minLengthText: i18n.sprintf(i18n._("The password is shorter than the minimum %d characters."), 3)
                }, {
                    xtype: 'textfield',
                    inputType: 'password',
                    fieldLabel: i18n._('Confirm Password'),
                    name: 'confirmPassword',
                    allowBlank: false,
                    comparePasswordField: 'settings_password',
                    vtype: 'passwordConfirmCheck'
                }]
            }, {
                xtype: 'container',
                items: [{
                    xtype: 'component',
                    html: '<b>' + i18n._('Configure administrator email.') + '</b>'
                }, {
                    xtype: 'textfield',
                    inputType: 'text',
                    fieldLabel: i18n._('Admin Email'),
                    name: 'adminEmail',
                    allowBlank: true,
                    width: 310,
                    vtype: 'email',
                    cls: 'small-top-margin'

                }]
            }, {
                xtype: 'container',
                items: [{
                    xtype: 'component',
                    html: '<b>' + i18n._('Select a timezone.') + '</b>'
                }, {
                    xtype: 'combo',
                    name: 'timezone',
                    editable: false,
                    store: Ung.setupWizard.TimeZoneStore,
                    width: 350,
                    hideLabel: true,
                    queryMode: 'local',
                    value: rpc.timezoneID,
                    cls: 'small-top-margin'
                }]
            }]
        });

        this.card = {
            title: i18n._('Settings'),
            panel: this.panel,
            onLoad: Ext.bind(function (complete) {
                var emailField = this.panel.query('field[name="adminEmail"]');
                if (emailField[0].getValue() == null || emailField[0].getValue() == '') {
                    emailField[0].setValue(rpc.adminEmail);
                }
                complete();
            }, this),
            onNext: Ext.bind(this.saveServerSettings, this),
            onValidate: Ext.bind(function () {
                return Ung.Util.validate(this.panel);
            }, this)
        };
    },
    saveServerSettings: function (handler) {
        Ext.MessageBox.wait(i18n._('Saving Settings'), i18n._('Please Wait'));
        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;
        this.password = this.panel.down('field[name="password"]').getValue();
        this.adminEmail = this.panel.down('field[name="adminEmail"]').getValue();

        var timezone = this.panel.down('field[name="timezone"]').getValue();
        var changed = (rpc.timezoneID != timezone);
        if (changed) {
            rpc.setup.setTimeZone(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception, i18n._('Unable to save Time Zone settings'))) {
                    return;
                }
                rpc.timezoneID = timezone;
                this.saveAdminPassword(handler);
            }, this), timezone);
        } else {
            this.saveAdminPassword(handler);
        }
    },
    saveAdminPassword: function (handler) {
        // New Password
        var password = this.panel.down('field[name="password"]').getValue();
        var adminEmail = this.panel.down('field[name="adminEmail"]').getValue();
        rpc.setup.setAdminPassword(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception, i18n._('Unable to save the admin password'))) {
                return;
            }
            var afterFn = Ext.bind(function (handler) {
                Ung.Setup.saveCurrentStep(this.stepName);
                handler();
            }, this, [handler]);
            Ung.Setup.authenticate(password, afterFn);
        }, this), password, adminEmail);
    }
});

// Setup Wizard - Step 2 (Remap Interfaces)
Ext.define('Ung.setupWizard.Interfaces', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.interfaceStore = Ext.create('Ext.data.JsonStore', {
            fields: [{ name: 'interfaceId' }, { name: 'name' }, { name: 'physicalDev' }, { name: 'deviceName' }, { name: 'macAddress' }, { name: 'connected' }, { name: 'duplex' }, { name: 'vendor' }, { name: 'mbit' }]
        });
        this.modelName = 'Ung.Model_Ung.setupWizard.Interfaces';
        this.deviceStore = Ext.create('Ext.data.JsonStore', {
            fields: [{ name: 'physicalDev' }]
        });
        this.enableAutoRefresh = true;
        this.networkSettings = null;
        this.interfaceGrid = Ext.create('Ext.grid.Panel', {
            flex: 1,
            store: this.interfaceStore,
            loadMask: true,
            stripeRows: true,
            cls: 'small-top-margin',
            enableColumnResize: true,
            enableColumnHide: false,
            enableColumnMove: false,
            selModel: Ext.create('Ext.selection.RowModel', {singleSelect: true}),
            plugins: [
                Ext.create('Ext.grid.plugin.CellEditing', {
                    clicksToEdit: 1
                })
            ],
            viewConfig: {
                forceFit: true,
                disableSelection: false,
                plugins: {
                    ptype: 'gridviewdragdrop',
                    dragText: i18n._('Drag and drop to reorganize')
                },
                listeners: {
                    'drop': {
                        fn: function (app, data, overModel, dropPosition, options) {
                            var i = 0;
                            var interfaceList = this.networkSettings.interfaces.list;
                            this.interfaceStore.each(function (currentRow) {
                                var intf = interfaceList[i];
                                currentRow.set({
                                    'interfaceId': intf.interfaceId,
                                    'name': intf.name
                                });
                                i += 1;
                            }, this);
                            return true;
                        },
                        scope: this
                    }
                }
            },
            height: 200,
            width: 585,
            columns: [{
                header: i18n._('Name'),
                dataIndex: 'name',
                sortable: false,
                width: 90,
                renderer: function (value) {
                    return i18n._(value);
                }
            }, {
                xtype: 'templatecolumn',
                menuDisabled: true,
                resizable: false,
                width: 30,
                tpl: '<img src="' + Ext.BLANK_IMAGE_URL + '" class="icon-drag"/>'
            }, {
                header: i18n._('Device'),
                tooltip: i18n._('Click on a Device to open a combo and choose the desired Device from a list. When anoter Device is selected the 2 Devices are swithced.'),
                tooltipType: 'title',
                dataIndex: 'deviceName',
                sortable: false,
                tdCls: 'ua-pointer',
                editor: {
                    xtype: 'combo',
                    store: this.deviceStore,
                    valueField: 'physicalDev',
                    displayField: 'physicalDev',
                    queryMode: 'local',
                    editable: false,
                    listeners: {
                        "change": {
                            fn: function (elem, newValue, oldValue) {
                                var sourceRecord = null;
                                var targetRecord = null;
                                this.interfaceStore.each(function (currentRow) {
                                    if (oldValue == currentRow.get('physicalDev')) {
                                        sourceRecord = currentRow;
                                    } else if (newValue == currentRow.get('physicalDev')) {
                                        targetRecord = currentRow;
                                    }
                                    if (sourceRecord != null && targetRecord != null) {
                                        return false;
                                    }
                                    return true;
                                });
                                if (sourceRecord == null || targetRecord == null || sourceRecord == targetRecord) {
                                    console.log(oldValue, newValue, sourceRecord, targetRecord);
                                    return false;
                                }
                                var soruceData = Ext.decode(Ext.encode(sourceRecord.data));
                                var targetData = Ext.decode(Ext.encode(targetRecord.data));
                                soruceData.deviceName = oldValue;
                                targetData.deviceName = newValue;
                                this.interfaceStore.beginUpdate();
                                sourceRecord.set({
                                    'physicalDev': targetData.physicalDev,
                                    'macAddress': targetData.macAddress,
                                    'duplex': targetData.duplex,
                                    'vendor': targetData.vendor,
                                    'mbit': targetData.mbit,
                                    'connected': targetData.connected
                                });
                                targetRecord.set({
                                    'deviceName': soruceData.deviceName,
                                    'physicalDev': soruceData.physicalDev,
                                    'macAddress': soruceData.macAddress,
                                    'duplex': soruceData.duplex,
                                    'vendor': soruceData.vendor,
                                    'mbit': soruceData.mbit,
                                    'connected': soruceData.connected
                                });
                                this.interfaceStore.endUpdate();
                                return true;
                            },
                            scope: this
                        }
                    }
                }
            }, {
                dataIndex: 'connected',
                sortable: false,
                resizable: false,
                tdCls: 'ua-draggable',
                width: 30,
                renderer: Ext.bind(function (value, metadata, record, rowIndex, colIndex, store, view) {
                    var divClass = (value == 'CONNECTED') ? 'ua-cell-enabled' : 'ua-cell-disabled';
                    return '<div class="' + divClass + '"><div>';
                }, this)
            }, {
                header: i18n._('Status'),
                dataIndex: 'connected',
                sortable: false,
                width: 213,
                flex: 1,
                tdCls: 'ua-draggable',
                renderer: Ext.bind(function (value, metadata, record, rowIndex, colIndex, store, view) {
                    var connected = record.get('connected');
                    var mbit = record.get('mbit');
                    var duplex = record.get('duplex');
                    var vendor = record.get('vendor');

                    var connectedStr = (connected == 'CONNECTED') ? i18n._('connected') : (connected == 'DISCONNECTED') ? i18n._('disconnected') : i18n._('unknown');
                    var duplexStr = (duplex == 'FULL_DUPLEX') ? i18n._('full-duplex') : (duplex == 'HALF_DUPLEX') ? i18n._('half-duplex') : i18n._('unknown');
                    return connectedStr + ' ' + mbit + ' ' + duplexStr + ' ' + vendor;
                }, this)
            }, {
                header: i18n._('MAC Address'),
                dataIndex: 'macAddress',
                sortable: false,
                width: 120,
                renderer: function (value, metadata, record, rowIndex, colIndex, store, view) {
                    var text = '';
                    if (value && value.length > 0) {
                        // Build the link for the mac address
                        text = '<a target="_blank" href="http://standards.ieee.org/cgi-bin/ouisearch?' +
                            value.substring(0, 8).replace(/:/g, '') + '">' + value + '</a>';
                    }
                    return text;
                }
            }]
        });

        var panel = Ext.create('Ext.container.Container', {
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype: 'component',
                margin: '20 0 0 20',
                html: '<h3>' + i18n._('Identify Network Cards') + '</h3>'
            }, {
                xtype: 'component',
                flex: 0,
                margin: '0 20',
                html: '<span style="color: red; font-weight: bold;">' + i18n._('Important:') + '</b></span>' +
                    ' ' + i18n._('This step identifies the external, internal, and other network cards.') + '<br/><br/>' +
                    '<b>' + i18n._('Step 1:') + '</b> ' +
                    i18n._('Plug an active cable into one network card to determine which network card it is.') + '<br/>' +
                    '<b>' + i18n._('Step 2:') + '</b> ' +
                    '<b>' + i18n._('Drag and drop') + '</b> ' + i18n._('the network card to map it to the desired interface.') + '<br/>' +
                    '<b>' + i18n._('Step 3:') + '</b> ' +
                    i18n._('Repeat steps 1 and 2 for each network card and then click <i>Next</i>.') + '<br/>'
            }, this.interfaceGrid]
        });

        this.card = {
            title: i18n._('Network Cards'),
            panel: panel,
            onLoad: Ext.bind(function (complete) {
                this.refreshInterfaces();
                this.enableAutoRefresh = true;
                Ext.defer(this.autoRefreshInterfaces, 3000, this);
                complete();
            }, this),
            onNext: Ext.bind(function (handler) {
                // disable auto refresh
                this.enableAutoRefresh = false;

                Ext.MessageBox.wait(i18n._('Saving Settings'), i18n._('Please Wait'));
                var interfacesMap = {};
                this.interfaceStore.each(function (currentRow) {
                    interfacesMap[currentRow.get('interfaceId')] = currentRow.get('physicalDev');
                });
                var changed = false, i, intf,
                    interfaceList = this.networkSettings.interfaces.list;
                for (i = 0; i < interfaceList.length; i += 1) {
                    intf = interfaceList[i];
                    if (!intf.isVlanInterface) {
                        if (intf['physicalDev'] != interfacesMap[intf['interfaceId']]) {
                            changed = true;
                        }
                        intf['physicalDev'] = interfacesMap[intf['interfaceId']];
                    }
                }
                var afterFn = Ext.bind(function (handler) {
                    Ung.Setup.saveCurrentStep(this.stepName);
                    handler();
                }, this, [handler]);
                if (changed) { //save netowrk changes only if maping is changed
                    rpc.networkManager.setNetworkSettings(Ext.bind(function (result, exception) {
                        if (Ung.Util.handleException(exception)) {
                            return;
                        }
                        Ext.MessageBox.hide();
                        afterFn();
                    }, this), this.networkSettings);
                } else {
                    Ext.MessageBox.hide();
                    afterFn();
                }
            }, this),
            onPrevious: Ext.bind(function (handler) {
                this.enableAutoRefresh = false;
                handler();
            }, this)
        };
    },

    createRecordsMap : function (recList, property) {
        var map = {}, i;
        for (i = 0; i < recList.length; i += 1) {
            map[recList[i][property]] = recList[i];
        }
        return map;
    },
    autoRefreshInterfaces: function () {
        if (!this.enableAutoRefresh) {
            return;
        }
        rpc.networkManager.getNetworkSettings(Ext.bind(function (result, exception) {
            if (!this.enableAutoRefresh) {
                return; // if auto refresh is now disabled, just return
            }
            if (Ung.Util.handleException(exception, i18n._('Unable to refresh the interfaces.'))) {
                return;
            }
            var interfaceList = [], i,
                allInterfaces = result.interfaces.list;
            for (i = 0; i < allInterfaces.length; i += 1) {
                if (!allInterfaces[i].isVlanInterface) {
                    interfaceList.push(allInterfaces[i]);
                }
            }

            if (interfaceList.length != this.interfaceStore.getCount()) {
                Ext.MessageBox.alert(i18n._('New interfaces'), i18n._('There are new interfaces, please restart the wizard.'), '');
                return;
            }
            rpc.networkManager.getDeviceStatus(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                if (result == null) {
                    return;
                }

                var deviceStatusMap = this.createRecordsMap((result == null ? [] : result.list), 'deviceName');
                //update device connected status
                this.interfaceStore.each(function (currentRow) {
                    var deviceStatus = deviceStatusMap[currentRow.get('physicalDev')];
                    if (deviceStatus != null) {
                        currentRow.set("connected", deviceStatus.connected);
                    }
                });
                if (this.enableAutoRefresh) {
                    Ext.defer(this.autoRefreshInterfaces, 3000, this);
                }
            }, this));
        }, this));
    },

    refreshInterfaces: function () {
        Ext.MessageBox.wait(i18n._('Refreshing Network Interfaces'), i18n._('Please Wait'));
        rpc.networkManager.getNetworkSettings(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception, i18n._('Unable to refresh the interfaces.'))) {
                return;
            }

            this.networkSettings = result;
            var interfaceList = [], i;
            var deviceList = [];
            var allInterfaces = result.interfaces.list;
            for (i = 0; i < allInterfaces.length; i += 1) {
                if (!allInterfaces[i].isVlanInterface) {
                    //interfaceList.push(allInterfaces[i]);
                    interfaceList.push(Ext.decode(Ext.encode(allInterfaces[i])));
                    deviceList.push({physicalDev: allInterfaces[i].physicalDev});
                }
            }
            rpc.networkManager.getDeviceStatus(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }

                Ext.MessageBox.hide();
                var intf, deviceStatus, deviceStatusMap = this.createRecordsMap((result == null ? [] : result.list), 'deviceName');
                for (i = 0; i < interfaceList.length; i += 1) {
                    intf = interfaceList[i];
                    deviceStatus = deviceStatusMap[intf.physicalDev];
                    Ext.applyIf(intf, deviceStatus);
                }
                this.interfaceStore.loadData(interfaceList);
                this.deviceStore.loadData(deviceList);
                if (interfaceList.length < 2) {
                    Ext.MessageBox.alert(i18n._('Missing interfaces'), i18n._('Untangle requires two or more network cards. Please reinstall with at least two network cards.'), '');
                }
            }, this));
        }, this));
    }
});

// Setup Wizard - Step 3 (Configure WAN)
Ext.define('Ung.setupWizard.Internet', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.v4ConfigTypes = [
            { name: 'AUTO', display: i18n._('Auto (DHCP)'), card: 0 },
            { name: 'STATIC', display: i18n._('Static'), card: 1 },
            { name: 'PPPOE', display: i18n._('PPPoE'), card: 2 }
        ];

        this.cards = [];

        // DHCP Panel
        this.dhcpPanel = Ext.create('Ext.panel.Panel', {
            saveData: Ext.bind(this.saveDHCP, this),
            items: [{
                xtype: 'component',
                margin: '10 0 0 50',
                html: '<b>' + i18n._('DHCP Status') + '</b>'
            }, {
                xtype: 'fieldset',
                border: false,
                defaultType: 'displayfield',
                defaults: {
                    labelWidth: Ung.setupWizard.labelWidth
                },
                items: [{
                    name: 'ip',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Current IP Address')
                }, {
                    name: 'netmask',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Current Netmask')
                }, {
                    name: 'gateway',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Current Gateway')
                }, {
                    name: 'dns1',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Current Primary DNS')
                }, {
                    name: 'dns2',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Current Secondary DNS')
                }]
            }],
            buttonAlign: 'left',
            buttons: [{
                xtype: 'button',
                style: {marginLeft: '30px'},
                iconCls: 'icon-refresh',
                text: i18n._('Refresh'),
                handler: Ext.bind(this.refresh, this)
            }, {
                xtype: 'button',
                iconCls: 'icon-test-connectivity',
                text: i18n._('Test Connectivity'),
                handler: Ext.bind(this.testConnectivity, this, [null])
            }]
        });
        this.cards.push(this.dhcpPanel);

        // Static Panel
        this.staticPanel = Ext.create('Ext.panel.Panel', {
            saveData: Ext.bind(this.saveStatic, this),
            items: [{
                xtype: 'fieldset',
                border: false,
                defaultType: 'textfield',
                defaults: {
                    labelWidth: Ung.setupWizard.labelWidth,
                    disabled: false,
                    msgTarget: 'side',
                    validationEvent: 'blur',
                    maskRe: /(\d+|\.)/,
                    vtype: 'ipAddress'
                },
                items: [{
                    name: 'ip',
                    fieldLabel: i18n._('IP Address'),
                    allowBlank: false
                }, {
                    name: 'prefix',
                    fieldLabel: i18n._('Netmask'),
                    width: 340,
                    xtype: 'combo',
                    store: Ung.setupWizard.v4NetmaskList,
                    queryMode: 'local',
                    triggerAction: 'all',
                    value: 24,
                    editable: false,
                    allowBlank: false
                }, {
                    name: 'gateway',
                    fieldLabel: i18n._('Gateway'),
                    allowBlank: false
                }, {
                    name: 'dns1',
                    fieldLabel: i18n._('Primary DNS'),
                    allowBlank: false
                }, {
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 0 0',
                    items: [{
                        xtype: 'textfield',
                        labelWidth: Ung.setupWizard.labelWidth,
                        disabled: false,
                        msgTarget: 'side',
                        validationEvent: 'blur',
                        maskRe: /(\d+|\.)/,
                        vtype: 'ipAddress',
                        name: 'dns2',
                        fieldLabel: i18n._('Secondary DNS'),
                        allowBlank: true
                    }, {
                        xtype: 'label',
                        html: i18n._('(optional)'),
                        cls: 'boxlabel'
                    }]
                }]
            }],
            buttonAlign: 'left',
            buttons: [{
                xtype: 'button',
                style: {marginLeft: '30px'},
                iconCls: 'icon-test-connectivity',
                text: i18n._('Test Connectivity'),
                handler: Ext.bind(this.testConnectivity, this, [null])
            }]
        });
        this.cards.push(this.staticPanel);

        // PPPoE Panel
        this.pppoePanel = Ext.create('Ext.container.Container', {
            saveData: Ext.bind(this.savePPPoE, this),
            items: [{
                xtype: 'component',
                margin: '10 0 0 50',
                html: '<b>' + i18n._('PPPoE Settings') + '</b>'
            }, {
                xtype: 'fieldset',
                border: false,
                defaultType: 'textfield',
                defaults: {
                    labelWidth: Ung.setupWizard.labelWidth
                },
                items: [{
                    fieldLabel: i18n._('Username'),
                    name: 'username',
                    disabled: false,
                    readOnly: false
                }, {
                    name: 'password',
                    inputType: 'password',
                    fieldLabel: i18n._('Password'),
                    disabled: false,
                    readOnly: false
                }]
            }, {
                xtype: 'component',
                margin: '0 0 0 50',
                html: '<b>' + i18n._('PPPoE Status') + '</b>'
            }, {
                xtype: 'fieldset',
                border: false,
                defaultType: 'displayfield',
                defaults: {
                    labelWidth: Ung.setupWizard.labelWidth
                },
                items: [{
                    fieldLabel: i18n._('IP Address'),
                    margin: '0 0 0 0',
                    name: 'ip'
                }, {
                    fieldLabel: i18n._('Netmask'),
                    margin: '0 0 0 0',
                    name: 'prefix'
                }, {
                    name: 'gateway',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Gateway')
                }, {
                    name: 'dns1',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Primary DNS')
                }, {
                    name: 'dns2',
                    margin: '0 0 0 0',
                    fieldLabel: i18n._('Secondary DNS')
                }]
            }],
            buttonAlign: 'left',
            buttons: [{
                xtype: 'button',
                style: {marginLeft: '30px'},
                iconCls: 'icon-refresh',
                text: i18n._('Refresh'),
                handler: Ext.bind(this.refresh, this)
            }, {
                xtype: 'button',
                iconCls: 'icon-test-connectivity',
                text: i18n._('Test Connectivity'),
                handler: Ext.bind(this.testConnectivity, this, [null])
            }]
        });
        this.cards.push(this.pppoePanel);

        this.panel = Ext.create('Ext.container.Container', {
            labelWidth: Ung.setupWizard.labelWidth,
            items: [{
                xtype: 'component',
                margin: '20 0 0 20',
                html: '<h3>' + i18n._('Configure the Internet Connection') + '<h3>'
            }, {
                xtype: 'combo',
                fieldLabel: i18n._('Configuration Type'),
                name: 'v4ConfigType',
                editable: false,
                store: Ext.create('Ext.data.Store', {
                    fields: ['name', 'display', 'card'],
                    data: this.v4ConfigTypes
                }),
                displayField: 'display',
                valueField: 'name',
                labelWidth: Ung.setupWizard.labelWidth,
                queryMode: 'local',
                listeners: {
                    'select': {
                        fn: Ext.bind(function (combo, record) {
                            this.cardPanelLayout.setActiveItem(record.get('card'));
                        }, this)
                    }
                },
                value: this.v4ConfigTypes[0].name
            }, {
                xtype: 'container',
                name: 'cardPanel',
                layout: 'card',
                items: this.cards,
                activePanel: 0,
                defaults: {
                    border: false,
                    margin: '10 0 0 0'
                }
            }]
        });

        this.cardPanelLayout = this.panel.down('container[name="cardPanel"]').getLayout();
        this.isInitialized = false;

        this.card = {
            title: i18n._('Internet Connection'),
            panel: this.panel,
            onLoad: Ext.bind(function (complete) {
                if (!this.isInitialized) {
                    this.cardPanelLayout.setActiveItem(0);
                }

                this.refreshNetworkDisplay();
                this.isInitialized = true;
                complete();
            }, this),
            onNext: Ext.bind(this.testConnectivity, this),
            onValidate: Ext.bind(this.validateInternetConnection, this)
        };
    },
    validateInternetConnection: function () {
        return Ung.Util.validate(this.cardPanelLayout.getActiveItem());
    },

    clearInterfaceSettings: function (wanSettings) {
        // delete unused stuff
        delete wanSettings.v4StaticAddress;
        delete wanSettings.v4StaticPrefix;
        delete wanSettings.v4StaticGateway;
        delete wanSettings.v4StaticDns1;
        delete wanSettings.v4StaticDns2;
    },

    saveDHCP: function (handler) {
        var wanSettings = this.getFirstWanSettings(rpc.networkSettings);
        this.clearInterfaceSettings(wanSettings);

        wanSettings.v4ConfigType = 'AUTO';
        wanSettings.v4NatEgressTraffic = true;

        this.setFirstWanSettings(rpc.networkSettings, wanSettings);

        var complete = Ext.bind(this.complete, this, [handler], true);
        rpc.networkManager.setNetworkSettings(complete, rpc.networkSettings);
    },

    saveStatic: function (handler) {
        var wanSettings = this.getFirstWanSettings(rpc.networkSettings);
        this.clearInterfaceSettings(wanSettings);

        wanSettings.v4ConfigType = 'STATIC';
        wanSettings.v4NatEgressTraffic = true;
        wanSettings.v4StaticAddress = this.staticPanel.down('field[name="ip"]').getValue();
        wanSettings.v4StaticPrefix = this.staticPanel.down('field[name="prefix"]').getValue();
        wanSettings.v4StaticGateway = this.staticPanel.down('field[name="gateway"]').getValue();
        wanSettings.v4StaticDns1 = this.staticPanel.down('field[name="dns1"]').getValue();
        wanSettings.v4StaticDns2 = this.staticPanel.down('field[name="dns2"]').getValue();
        if (wanSettings.v4StaticDns2.length <= 0) {
            wanSettings.v4StaticDns2 = null;
        } //ignore empty box

        this.setFirstWanSettings(rpc.networkSettings, wanSettings);

        var complete = Ext.bind(this.complete, this, [handler], true);
        rpc.networkManager.setNetworkSettings(complete, rpc.networkSettings);
    },

    savePPPoE: function (handler) {
        var wanSettings = this.getFirstWanSettings(rpc.networkSettings);
        this.clearInterfaceSettings(wanSettings);

        wanSettings.v4ConfigType = 'PPPOE';
        wanSettings.v4NatEgressTraffic = true;
        wanSettings.v4PPPoEUsePeerDns = true;
        wanSettings.v4PPPoEUsername = this.pppoePanel.down('field[name="username"]').getValue();
        wanSettings.v4PPPoEPassword = this.pppoePanel.down('field[name="password"]').getValue();

        this.setFirstWanSettings(rpc.networkSettings, wanSettings);

        var complete = Ext.bind(this.complete, this, [handler], true);
        rpc.networkManager.setNetworkSettings(complete, rpc.networkSettings);
    },

    complete: function (result, exception, foo, handler) {
        if (Ung.Util.handleException(exception)) {
            return;
        }
        if (handler != null) {
            handler();
        }
    },

    // Refresh the current network settings (lease or whatever)
    refresh: function () {
        Ext.MessageBox.wait(i18n._('Refreshing...'), i18n._('Please Wait'));
        var handler = Ext.bind(function () {
            //redresh network data
            this.refreshNetworkDisplay();
            Ext.MessageBox.hide();
        }, this);

        this.saveData(handler);
    },

    testConnectivity: function (handler) {
        if (!this.validateInternetConnection()) {
            Ext.MessageBox.show({
                title: i18n._('Unable to Test Connectivity'),
                msg: i18n._('Please complete all of the required fields.'),
                width: 300,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
            return;
        }

        Ext.MessageBox.wait(i18n._('Saving Settings...'), i18n._('Please Wait'));
        var afterFn1 = null;
        if (handler) {
            afterFn1 = Ext.bind(function (handler) {
                Ung.Setup.saveCurrentStep(this.stepName);
                handler();
            }, this, [handler]);
        }
        var afterFn = Ext.bind(this.execConnectivityTest, this, [afterFn1]);

        this.saveData(afterFn);
    },

    saveData: function (handler) {
        this.cardPanelLayout.getActiveItem().saveData(handler);
    },

    completeConnectivityTest: function (result, exception, foo, handler) {
        if (Ext.MessageBox.rendered) {
            Ext.MessageBox.hide();
        }

        if (exception) {
            Ext.MessageBox.show({
                title: i18n._('Network Settings'),
                msg: i18n._('Unable to complete connectivity test, please try again.'),
                width: 300,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
            return;
        }

        var message = '';

        // If handler is null then this is just a manual connectivity test, so just show a pop-up
        if (handler == null) {
            if ((result.tcpWorking == false)  && (result.dnsWorking == false)) {
                message = i18n._('Warning! Internet and DNS tests failed.');
            } else if (result.tcpWorking == false) {
                message = i18n._('Warning! DNS tests succeeded, but Internet tests failed.');
            } else if (result.dnsWorking == false) {
                message = i18n._('Warning! Internet tests succeeded, but DNS tests failed.');
            } else {
                message = i18n._('Success!');
            }
            Ext.MessageBox.show({
                title: i18n._('Internet Status'),
                msg: message,
                width: 300,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
        } else {
            // If handler is not null, then "Next" has been pushed.
            // If connectivity is not valid, then display a warning, otherwise just continue
            if ((result.tcpWorking == false)  && (result.dnsWorking == false)) {
                message = i18n._('Warning! Internet tests and DNS tests failed.');
            } else if (result.tcpWorking == false) {
                message = i18n._('Warning! DNS tests succeeded, but Internet tests failed.');
            } else if (result.dnsWorking == false) {
                message = i18n._('Warning! Internet tests succeeded, but DNS tests failed.');
            } else {
                message = null;
            }

            // if the test passed, just continue
            if (message == null) {
                handler();
                return;
            }

            var warningText = message + '<br/><br/>' + i18n._('It is recommended to configure valid internet settings before continuing. Try again?');
            Ext.Msg.confirm(i18n._('Warning:'), warningText, Ext.bind(function (btn, text) {
                if (btn == 'yes') {
                    return;
                }
                handler();
            }, this));
        }
    },

    execConnectivityTest: function (handler) {
        Ext.MessageBox.wait(i18n._('Testing Connectivity...'), i18n._('Please Wait'));
        rpc.connectivityTester.getStatus(Ext.bind(this.completeConnectivityTest, this, [handler], true));
    },

    getFirstWanSettings: function (networkSettings) {
        var c;
        for (c = 0; c < networkSettings['interfaces']['list'].length; c += 1) {
            if (networkSettings['interfaces']['list'][c]['configType'] == 'DISABLED') {
                continue;
            }
            if (networkSettings['interfaces']['list'][c]['isWan']) {
                return networkSettings['interfaces']['list'][c];
            }
        }
        return null;
    },

    setFirstWanSettings: function (networkSettings, firstWanSettings) {
        var c;
        for (c = 0; c < networkSettings['interfaces']['list'].length; c += 1) {
            if (firstWanSettings['interfaceId'] == networkSettings['interfaces']['list'][c]['interfaceId']) {
                networkSettings['interfaces']['list'][c] = firstWanSettings;
            }
        }
    },

    // This does not reload the settings, it just updates what is
    // displayed inside of the User Interface.
    refreshNetworkDisplay: function () {
        var c = 0;
        var networkSettings, firstWanStatus;
        try {
            networkSettings = rpc.networkManager.getNetworkSettings();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        rpc.networkSettings = networkSettings;

        var firstWan = this.getFirstWanSettings(networkSettings);
        try {
            firstWanStatus = rpc.networkManager.getInterfaceStatus(firstWan.interfaceId);
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        if (networkSettings['interfaces'] == null && networkSettings['interfaces']['list'] == null) {
            console.error('Missing interface information.');
            return;
        }

        var isConfigured = (firstWan.configType != null && firstWan.v4ConfigType != null);
        var card;
        if (isConfigured) {
            for (c = 0; c < this.v4ConfigTypes.length; c += 1) {
                if (this.v4ConfigTypes[c].name == firstWan.v4ConfigType) {
                    this.cardPanelLayout.setActiveItem(c);
                }
            }
            this.updateValue(this.card.panel.down('combo[name="v4ConfigType"]'), firstWan.v4ConfigType);
            this.updateValue(this.card.panel.down('field[name="username"]'), firstWan.v4PPPoEUsername);
            this.updateValue(this.card.panel.down('field[name="password"]'), firstWan.v4PPPoEPassword);

            for (c = 0; c < this.cards.length; c += 1) {
                card = this.cards[c];
                this.updateValue(card.down('field[name="ip"]'), firstWanStatus.v4Address);
                this.updateValue(card.down('field[name="prefix"]'), firstWanStatus.v4PrefixLength);
                this.updateValue(card.down('field[name="netmask"]'), firstWanStatus.v4Netmask);
                this.updateValue(card.down('field[name="gateway"]'), firstWanStatus.v4Gateway);
                this.updateValue(card.down('field[name="dns1"]'), firstWanStatus.v4Dns1);
                this.updateValue(card.down('field[name="dns2"]'), firstWanStatus.v4Dns2);
            }
        } else { // not configured
            for (c = 0; c < this.cards.length; c += 1) {
                card = this.cards[c];
                this.updateValue(card.down('field[name="ip"]'), '');
                this.updateValue(card.down('field[name="prefix"]'), '');
                this.updateValue(card.down('field[name="gateway"]'), '');
                this.updateValue(card.down('field[name="dns1"]'), '');
                this.updateValue(card.down('field[name="dns2"]'), '');
            }
        }
    },

    // Guard the field to shield strange values from the user.
    updateValue: function (field, value) {
        if (field == null) {
            return;
        }
        if (value == null || value == '0.0.0.0') {
            value = '';
        }

        field.setValue(value);
    }

});

// Setup Wizard - Step 4 (Configure Internal)
Ext.define('Ung.setupWizard.InternalNetwork', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                margin: '20 20 0 20',
                html: '<h3>' + i18n._('Configure the Internal Network Interface') + '</h3>'
            }, {
                xtype: 'container',
                margin: '40 0 0 20',
                layout: 'column',
                items: [{
                    xtype: 'container',
                    columnWidth: 0.60,
                    items: [{
                        xtype: 'radio',
                        name: 'bridgeOrRouter',
                        inputValue: 'router',
                        boxLabel: i18n._('Router'),
                        cls: 'large-option',
                        hideLabel: 'true',
                        listeners: {
                            change: {
                                fn: Ext.bind(function (checkbox, checked) {
                                    this.onSetRouter(checked);
                                }, this)
                            }
                        }
                    }, {
                        xtype: 'component',
                        style: {marginLeft: '20px'},
                        html: i18n._('This is recommended if the external port is plugged into the internet connection. This enables NAT and DHCP.')
                    }, {
                        name: 'network',
                        xtype: 'textfield',
                        margin: '7 0 0 10',
                        width: 330,
                        labelWidth: 140,
                        fieldLabel: i18n._('Internal Address'),
                        vText: i18n._('Please enter a valid Network  Address'),
                        vtype: 'ipAddress',
                        allowBlank: false,
                        msgTarget: 'side',
                        maskRe: /(\d+|\.)/,
                        disabled: true,
                        value: '192.168.1.1',
                        validationEvent: 'blur'
                    }, {
                        name: 'prefix',
                        margin: '5 0 0 10',
                        width: 330,
                        labelWidth: 140,
                        fieldLabel: i18n._('Internal Netmask'),
                        xtype: 'combo',
                        store: Ung.setupWizard.v4NetmaskList,
                        queryMode: 'local',
                        triggerAction: 'all',
                        value: 24,
                        disabled: true,
                        editable: false
                    }, {
                        xtype: 'checkbox',
                        hideLabel: true,
                        checked: true,
                        disabled: true,
                        name: 'enableDhcpServer',
                        margin: '0 0 0 20',
                        boxLabel: i18n._('Enable DHCP Server (default)')
                    }]
                }, {
                    xtype: 'component',
                    margin: '40 0 0 20',
                    columnWidth: 0.40,
                    html: '<img class="wizard-network-img" src="/skins/' + rpc.skinName + '/images/admin/wizard/router.png"/>'
                }]
            }, {
                xtype: 'container',
                margin: '50 0 0 20',
                layout: 'column',
                items: [{
                    xtype: 'container',
                    columnWidth: 0.60,
                    items: [{
                        xtype: 'radio',
                        name: 'bridgeOrRouter',
                        inputValue: 'BRIDGED',
                        boxLabel: i18n._('Transparent Bridge'),
                        cls: 'large-option',
                        hideLabel: 'true',
                        checked: true
                    }, {
                        xtype: 'component',
                        style: {marginLeft: '20px'},
                        html: i18n._('This is recommended if the external port is plugged into a firewall/router. This bridges Internal and External and disables DHCP.')
                    }]
                }, {
                    xtype: 'component',
                    margin: '0 0 0 20',
                    columnWidth: 0.40,
                    html: '<img class="wizard-network-img" src="/skins/' + rpc.skinName + '/images/admin/wizard/bridge.png"/>'
                }]
            }]
        });

        this.card = {
            title: i18n._('Internal Network'),
            panel: this.panel,
            onLoad: Ext.bind(this.onLoadInternalSuggestion, this),
            onNext: Ext.bind(this.saveInternalNetwork, this),
            onValidate: Ext.bind(this.validateInternalNetwork, this)
        };
    },
    onLoadInternalSuggestion: function (complete) {
        if (!rpc.networkSettings) {
            try {
                rpc.networkSettings = rpc.networkManager.getNetworkSettings();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }

        var networkSettings = rpc.networkSettings;
        // find the internal interface and see if its currently set to static.
        // if so change the default to router
        if (networkSettings != null && networkSettings['interfaces'] != null && networkSettings['interfaces']['list'] != null) {
            var intfs = networkSettings['interfaces']['list'], bridgeOrRouterRadio, c;
            for (c = 0; c < intfs.length; c += 1) {
                // find first non-WAN
                if (intfs[c]['isWan'] != null && intfs[c]['isWan']) {
                    continue;
                }

                bridgeOrRouterRadio = this.panel.query('radio[name="bridgeOrRouter"]');
                if (intfs[c].configType == 'BRIDGED') {
                    bridgeOrRouterRadio[0].setValue(false);
                    bridgeOrRouterRadio[1].setValue(true);
                } else { // ADDRESSED or DISABLED
                    bridgeOrRouterRadio[0].setValue(true);
                    bridgeOrRouterRadio[1].setValue(false);
                }

                if (intfs[c]['v4StaticAddress'] != null && intfs[c]['v4StaticPrefix'] != null) {
                    this.panel.down('field[name="network"]').setValue(intfs[c]['v4StaticAddress']);
                    this.panel.down('combo[name="prefix"]').setValue(intfs[c]['v4StaticPrefix']);
                }
                break;
            }
        }

        complete();
    },
    onSetRouter: function (isSet) {
        var i, ar = [this.panel.down('field[name="network"]'), this.panel.down('combo[name="prefix"]'), this.panel.down('checkbox[name="enableDhcpServer"]')];
        for (i = 0; i < ar.length; i += 1) {
            ar[i].setDisabled(!isSet);
        }
        this.panel.down('field[name="network"]').clearInvalid();
    },

    getFirstNonWanSettings: function (networkSettings) {
        var c;
        for (c = 0; c < networkSettings['interfaces']['list'].length; c += 1) {
            if (!networkSettings['interfaces']['list'][c]['isWan']) {
                return networkSettings['interfaces']['list'][c];
            }
        }
        return null;
    },
    getFirstWanSettings: function (networkSettings) {
        var c;
        for (c = 0; c < networkSettings['interfaces']['list'].length; c += 1) {
            if (networkSettings['interfaces']['list'][c]['configType'] == "DISABLED") {
                continue;
            }
            if (networkSettings['interfaces']['list'][c]['isWan']) {
                return networkSettings['interfaces']['list'][c];
            }
        }
        return null;
    },

    validateInternalNetwork: function () {
        var rv = true, nic = false, i,
            bridgeOrRouterRadio = this.panel.query('radio[name="bridgeOrRouter"]');
        for (i = 0; i < bridgeOrRouterRadio.length; i += 1) {
            if (bridgeOrRouterRadio[i].getValue()) {
                nic = bridgeOrRouterRadio[i].inputValue;
                break;
            }
        }
        if (nic == 'router') {
            rv = Ung.Util.validate(this.panel);
        }
        return rv;
    },

    saveInternalNetwork: function (handler) {
        var value = this.panel.down('radio[name="bridgeOrRouter"]').getGroupValue();

        if (value == null) {
            Ext.MessageBox.alert(i18n._('Select a value'), i18n._('Please choose bridge or router.'));
            return;
        }

        Ext.MessageBox.wait(i18n._('Saving Internal Network Settings'), i18n._('Please Wait'));
        var afterFn = Ext.bind(function (handler) {
            Ung.Setup.saveCurrentStep(this.stepName);
            handler();
        }, this, [handler]);

        var delegate = Ext.bind(function (result, exception, foo, handler) {
            if (Ung.Util.handleException(exception, i18n._('Unable to save Local Network Settings'))) {
                return;
            }
            Ext.MessageBox.hide();
            handler();
        }, this, [ afterFn ], true );
        var firstNonWan = this.getFirstNonWanSettings(rpc.networkSettings);
        var changed;
        if (value == 'BRIDGED') {
            changed = (firstNonWan['configType'] != 'BRIDGED');
            if (changed) {
                firstNonWan['configType'] = 'BRIDGED';
                //If using Internal Address and it is changed in this step redirect to new address
                if (window.location.hostname == firstNonWan['v4StaticAddress']) {
                    var firstWan = this.getFirstWanSettings(rpc.networkSettings);
                    if (firstWan && firstWan.interfaceId) {
                        var firstWanStatus;
                        try {
                            firstWanStatus = rpc.networkManager.getInterfaceStatus(firstWan.interfaceId);
                        } catch (e) {
                            Ung.Util.rpcExHandler(e);
                        }
                        if (firstWanStatus.v4Address) {
                            //Use Internal Address instead of External Address
                            var newSetupLocation = window.location.href.replace(firstNonWan['v4StaticAddress'], firstWanStatus.v4Address);
                            delegate = function () {}; // no delegate
                            rpc.keepAlive = function () {}; // prevent keep alive
                            Ext.defer(function () {
                                Ext.MessageBox.confirm(i18n._('Redirect to the new setup address?'),
                                    Ext.String.format(i18n._('When switching from Router to Transparent Bridge the setup is no longer accessible using Internal Address. Instead it could be accessible using the External Address: {0}'), firstWanStatus.v4Address) + '<br/><br/>' +
                                    Ext.String.format(i18n._('If you want to be redirected to the new setup address: {0} please reinitialize your Network Settings and press Yes.'), '<a href="' + newSetupLocation + '">' + newSetupLocation + '</a>') + '<br/><br/>' +
                                    i18n._('Clicking No will prevent redirection and will try to continue setup using the current address, but it might no longer be accessible.'),
                                    function (btn) {
                                        if (btn == 'yes') {
                                            window.location.href = newSetupLocation;
                                        } else {
                                            rpc.tolerateKeepAliveExceptions = false;
                                            afterFn();
                                        }
                                    }, this);
                            }, 5000, this);
                        }
                    }
                }
                rpc.networkManager.setNetworkSettings(delegate, rpc.networkSettings);
            } else {
                Ext.MessageBox.hide();
                afterFn();
            }
        } else {
            var initialNetwork = firstNonWan['v4StaticAddress'];
            var initialConfigType = firstNonWan['configType'];
            var network = this.panel.down('field[name="network"]').getValue();
            var prefix = this.panel.down('combo[name="prefix"]').getValue();
            var enableDhcpServer = this.panel.down('checkbox[name="enableDhcpServer"]').getValue();
            changed = (firstNonWan['configType'] != 'ADDRESSED' || firstNonWan['v4ConfigType'] != 'STATIC' || firstNonWan['v4StaticAddress'] != network || firstNonWan['v4StaticPrefix'] != prefix || firstNonWan['dhcpEnabled'] != enableDhcpServer);
            if (changed) {
                firstNonWan['configType'] = 'ADDRESSED';
                firstNonWan['v4ConfigType'] = 'STATIC';
                firstNonWan['v4StaticAddress'] = network;
                firstNonWan['v4StaticPrefix'] = prefix;
                firstNonWan['dhcpEnabled'] = enableDhcpServer;
                delete firstNonWan.dhcpRangeStart; // new ones will be chosen
                delete firstNonWan.dhcpRangeEnd; // new ones will be chosen
                if (window.location.hostname != 'localhost') {
                    if (initialConfigType == 'BRIDGED') {
                        //If the Transparent Bridge mode was used test if the external address was used, and offer to redirect Internal address
                        var firstWanB = this.getFirstWanSettings(rpc.networkSettings);
                        if (firstWanB && firstWanB.interfaceId) {
                            var firstWanStatusB;
                            try {
                                firstWanStatusB = rpc.networkManager.getInterfaceStatus(firstWanB.interfaceId);
                            } catch (e) {
                                Ung.Util.rpcExHandler(e);
                            }
                            if (window.location.hostname == firstWanStatusB.v4Address) {
                                //Use External Address instead of Internal Address
                                var newSetupLocationB = window.location.href.replace(firstWanStatusB.v4Address, network);
                                delegate = function () {}; // no delegate
                                rpc.tolerateKeepAliveExceptions = true; // prevent keep alive exceptions
                                Ext.defer(function () {
                                    Ext.MessageBox.confirm(i18n._('Redirect to the new setup address?'),
                                        Ext.String.format(i18n._('When switching to from Transparent Bridge to Router the setup might no longer accessible using External Address. Instead it could be accessible using the Internal Address: {0}'), network) + '<br/><br/>' +
                                        Ext.String.format(i18n._('If you want to be redirected to the new setup address: {0} please reinitialize your Network Settings and press Yes.'), '<a href="' + newSetupLocationB + '">' + newSetupLocationB + '</a>') + '<br/><br/>' +
                                        i18n._('Clicking No will prevent redirection and will try to continue setup using the current address, but it might no longer be accessible.'),
                                        function (btn) {
                                            if (btn == 'yes') {
                                                window.location.href = newSetupLocationB;
                                            } else {
                                                rpc.tolerateKeepAliveExceptions = false;
                                                afterFn();
                                            }
                                        }, this);
                                }, 5000, this);
                            }
                        }
                    } else if (window.location.hostname == initialNetwork && initialNetwork != network) {
                        //If using internal address and it is changed in this step redirect to new internal address
                        var newSetupLocationC = window.location.href.replace(initialNetwork, network);
                        delegate = function () {}; // no delegate
                        rpc.keepAlive = function () {}; // prevent keep alive
                        Ext.MessageBox.wait(i18n._('Saving Internal Network Settings') + '<br/><br/>' +
                            Ext.String.format(i18n._('The Internal Address is changed to: {0}'), network) + '<br/>' +
                            Ext.String.format(i18n._('The changes are applied and you will be redirected to the new setup address: {0}'), '<a href="' + newSetupLocationC + '">' + newSetupLocationC + '</a>') + '<br/><br/>' +
                            i18n._('If the new location is not loaded after 30 seconds please reinitialize your Network Settings and try again.'), i18n._('Please Wait'));
                        Ext.defer(function () { window.location.href = newSetupLocationC; }, 30000, this);
                    }
                }
                rpc.networkManager.setNetworkSettings(delegate, rpc.networkSettings);
            } else {
                Ext.MessageBox.hide();
                afterFn();
            }
        }
    }
});

// Setup Wizard - Step 4.1 (Wireless)
Ext.define('Ung.setupWizard.Wireless', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                margin: '20 20 0 20',
                html: '<h3>' + i18n._('Configure Wireless Settings') + '</h3>'
            }, {
                xtype: 'container',
                margin: '30 0 0 20',
                items: [{
                    xtype:'textfield',
                    name: "wirelessSsid",
                    fieldLabel: i18n._('Network Name (SSID)'),
                    labelWidth: 150,
                    maxLength: 30,
                    maskRe: /[a-zA-Z0-9\-_=]/
                },{
                    xtype: "combo",
                    name: "wirelessEncryption",
                    fieldLabel: i18n._("Encryption"),
                    labelWidth: 150,
                    editable: false,
                    store: [["NONE", i18n._('None')], ["WPA1", i18n._('WPA')], ["WPA12", i18n._('WPA / WPA2')], ["WPA2", i18n._('WPA2')]]
                },{
                    xtype:'textfield',
                    name: "wirelessPassword",
                    fieldLabel: i18n._('Password'),
                    labelWidth: 150,
                    maxLength: 63,
                    minLength: 8,
                    maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/
                },{
                    xtype: 'component',
                    margin: '0 0 0 150',
                    html: "<i>" + i18n._('The wireless password must be at least 8 characters.') + "</i>"
                }]
            }]
        });

        this.card = {
            title: i18n._('Wireless'),
            panel: this.panel,
            onValidate: Ext.bind(this.validateWireless, this),
            onLoad: Ext.bind(function (complete) {
                complete();
                Ext.MessageBox.wait(i18n._('Loading Wireless Settings'), i18n._('Please Wait'));
                rpc.networkManager.getWirelessSsid(Ext.bind(function (result, exception) {
                    if (Ung.Util.handleException(exception)) return;
                    var element = this.panel.down('textfield[name="wirelessSsid"]');
                    element.setValue( result );
                    this.initialSsid = result;

                    rpc.networkManager.getWirelessEncryption(Ext.bind(function (result, exception) {
                        if (Ung.Util.handleException(exception)) return;
                        var element = this.panel.down('combo[name="wirelessEncryption"]');
                        element.setValue( result );
                        this.initialEncryption = result;

                        rpc.networkManager.getWirelessPassword(Ext.bind(function (result, exception) {
                            if (Ung.Util.handleException(exception)) return;
                            var element = this.panel.down('textfield[name="wirelessPassword"]');
                            if ( result != "12345678" ) // this is the default password, do not autofill
                                element.setValue( result );
                            this.initialPassword = result;

                            Ext.MessageBox.hide();
                        }, this));

                    }, this));

                }, this));
            }, this),
            onNext: Ext.bind(this.saveWireless, this)
        };
    },
    validateWireless: function () {
        var element = this.panel.down('textfield[name="wirelessPassword"]');
        var password = element.getValue();

        if ( password == null || password.length < 8 ) {
            Ext.MessageBox.show({
                title: i18n._('Wireless Password'),
                msg: i18n._('The wireless password must be at least 8 characters.'),
                width: 300,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
            return false;
        }
        if ( password == "12345678" ) {
            Ext.MessageBox.show({
                title: i18n._('Wireless Password'),
                msg: i18n._('You must choose a new and different wireless password.'),
                width: 300,
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.INFO
            });
            return false;
        }

        return Ung.Util.validate(this.panel);
    },
    saveWireless: function (handler) {
        var wirelessSsidValue = this.panel.down('textfield[name="wirelessSsid"]').getValue();
        var wirelessEncryptionValue = this.panel.down('combo[name="wirelessEncryption"]').getValue();
        var wirelessPasswordValue = this.panel.down('textfield[name="wirelessPassword"]').getValue();
        var afterFn = Ext.bind(function (handler) {
            Ung.Setup.saveCurrentStep(this.stepName);
            handler();
        }, this, [handler]);

        var changed = (this.initialSsid != wirelessSsidValue) || (this.initialEncryption != wirelessEncryptionValue) || (this.initialPassword != wirelessPasswordValue);
        if (changed) {
            Ext.MessageBox.wait(i18n._('Saving Settings'), i18n._('Please Wait'));
            var delegate = Ext.bind(function (result, exception, foo, handler) {
                if (Ung.Util.handleException(exception, i18n._('Problem'))) {
                    return;
                }
                Ext.MessageBox.hide();
                handler();
            }, this, [ afterFn ], true);
            rpc.networkManager.setWirelessSettings( delegate, wirelessSsidValue, wirelessEncryptionValue, wirelessPasswordValue );
        } else {
            afterFn();
        }
    }
});

// Setup Wizard - Step 5 (Configure Upgrades)
Ext.define('Ung.setupWizard.AutoUpgrades', {
    constructor: function (config) {
        Ext.apply(this, config);
        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                margin: '20 20 0 20',
                html: '<h3>' + i18n._('Configure Automatic Upgrade Settings') + '</h3>'
            }, {
                xtype: 'container',
                margin: '30 0 0 20',
                items: [{
                    xtype: 'checkbox',
                    name: 'autoUpgradesCheckbox',
                    checked: true,
                    boxLabel: i18n._('Automatically Install Upgrades'),
                    cls: 'large-option',
                    hideLabel: 'true'
                }, {
                    xtype: 'component',
                    margin: '0 0 0 20',
                    html: i18n._('Automatically install new versions of the software when available.') + '<br/>' +
                    i18n._('This is the recommended choice for most sites.')
                }]
            }, {
                xtype: 'container',
                margin: '30 0 0 20',
                items: [{
                    xtype: 'checkbox',
                    name: 'cloudEnabledCheckbox',
                    checked: true,
                    boxLabel: Ext.String.format(i18n._('Connect to {0} Cloud'), rpc.oemName),
                    cls: 'large-option',
                    hideLabel: 'true'
                }, {
                    xtype: 'component',
                    margin: '0 0 0 20',
                    html: Ext.String.format(i18n._('Remain securely connected to the {0} cloud for cloud management, hot fixes, and support access.'), rpc.oemName) + '<br/>' +
                    i18n._('This is the recommended choice for most sites.')
                }]
            }]
        });

        this.card = {
            title: i18n._('Automatic Upgrades'),
            panel: this.panel,
            onLoad: Ext.bind(function (complete) {
                complete();
                Ext.MessageBox.wait(i18n._('Loading Automatic Upgrades Settings'), i18n._('Please Wait'));
                rpc.systemManager.getSettings(Ext.bind(function (result, exception) {
                    if (Ung.Util.handleException(exception)) {
                        return;
                    }
                    this.initialAutoUpgrade = result.autoUpgrade;
                    this.initialCloudEnabled = result.cloudEnabled;
                    if (!result.autoUpgrade) {
                        var autoUpgradesCheckbox = this.panel.down('checkbox[name="autoUpgradesCheckbox"]');
                        autoUpgradesCheckbox.setValue(false);
                    }
                    if (!result.cloudEnabled) {
                        var cloudEnabledCheckbox = this.panel.down('checkbox[name="cloudEnabledCheckbox"]');
                        cloudEnabledCheckbox.setValue(false);
                    }
                    Ext.MessageBox.hide();
                }, this));
            }, this),
            onNext: Ext.bind(this.saveAutoUpgrades, this)
        };
    },
    saveAutoUpgrades: function (handler) {
        var autoUpgradeValue = this.panel.down('checkbox[name="autoUpgradesCheckbox"]').getValue();
        var cloudEnabledValue = this.panel.down('checkbox[name="cloudEnabledCheckbox"]').getValue();
        var afterFn = Ext.bind(function (handler) {
            Ung.Setup.saveCurrentStep(this.stepName);
            handler();
        }, this, [handler]);

        var changed = (this.initialAutoUpgrade != autoUpgradeValue) || (this.initialCloudEnabled != cloudEnabledValue);
        if (changed) {
            Ext.MessageBox.wait(i18n._('Saving Settings'), i18n._('Please Wait'));
            var delegate = Ext.bind(function (result, exception, foo, handler) {
                if (Ung.Util.handleException(exception, i18n._('Unable to save Settings'))) {
                    return;
                }
                Ext.MessageBox.hide();
                handler();
            }, this, [ afterFn ], true);
            rpc.systemManager.getSettings(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                var systemSettings = result;
                systemSettings.autoUpgrade = autoUpgradeValue;
                systemSettings.cloudEnabled = cloudEnabledValue;
                if ( cloudEnabledValue )
                    systemSettings.supportEnabled = cloudEnabledValue;
                rpc.systemManager.setSettings(delegate, systemSettings);
            }, this));
        } else {
            afterFn();
        }
    }
});

// Setup Wizard - Step 6 (Complete)
Ext.define('Ung.setupWizard.Complete', {
    constructor: function (config) {
        Ext.apply(this, config);
        var panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                margin: '20',
                html: Ext.String.format(i18n._('<b>The {0} Server is now configured.</b><br/><br/>You are now ready to configure the applications.'), rpc.oemName)
            }]
        });

        this.card = {
            title: i18n._('Finished'),
            panel: panel,
            onNext: Ext.bind(this.openUserInterface, this)
        };
    },
    openUserInterface: function (handler) {
        Ext.MessageBox.wait(i18n._('Loading User Interface...'), i18n._('Please Wait'));

        //and set a flag so the wizard wont run again
        rpc.jsonrpc.UvmContext.wizardComplete(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) { return; }
            //now open the UI
            window.location.href = "/webui/startPage.do";
        }, this));
    }
});

Ext.define('Ung.Setup', {
    singleton: true,
    init: function () {
        JSONRpcClient.toplevel_ex_handler = Ung.Util.rpcExHandler;

        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;
        //Initialize startup info (skinName, timezoneID, timezones, oemName, adminEmail, translations, wizardSettings)
        rpc.setup.getSetupWizardStartupInfo(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) { return; }
            Ext.applyIf(rpc, result);
            this.initComplete();
        }, this));

        Ext.apply(Ext.form.field.VTypes, {
            ipAddress: function (val, field) {
                return val.match(this.ipAddressRegex);
            },
            ipAddressText: 'Please enter a valid IP Address',
            ipAddressRegex: /\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b/,

            passwordConfirmCheck: function (val, field) {
                var pass_original = Ext.getCmp(field.comparePasswordField);
                return val == pass_original.getValue();
            },
            passwordConfirmCheckText: 'Passwords do not match'
        });
    },
    initComplete: function () {
        if (Ext.isEmpty(rpc.oemName)) {
            rpc.oemName = 'Untangle';
        }
        Ung.setupWizard.TimeZoneStore = [];
        var timeZoneData = eval(rpc.timezones), i;
        for (i = 0; i < timeZoneData.length; i += 1) {
            Ung.setupWizard.TimeZoneStore.push([timeZoneData[i][0], "(" + timeZoneData[i][1] + ") " + timeZoneData[i][0]]);
        }
        i18n = new Ung.I18N({ 'map': rpc.translations });
        window.document.title = i18n._('Setup Wizard');
        //Ext.get('container').setStyle('width', '800px');

        this.stepsTitlesMap = {
            'Welcome': i18n._('Welcome'),
            'ServerSettings': i18n._('Settings'),
            'Interfaces': i18n._('Network Cards'),
            'Internet': i18n._('Internet Connection'),
            'InternalNetwork': i18n._('Internal Network'),
            'Wireless': i18n._('Wireless'),
            'AutoUpgrades': i18n._('Automatic Upgrades'),
            'Complete': i18n._('Finished')
        };

        // just in case rpc.wizardSettings.steps are not set
        if (!rpc.wizardSettings.steps) {
            rpc.wizardSettings.steps = ['Welcome', 'ServerSettings', 'Interfaces', 'Internet', 'InternalNetwork', 'AutoUpgrades', 'Complete'];
        }

        //--- DEBUGGING CODE (Enable wizard resuming to any step)---
        //rpc.wizardSettings.wizardComplete=false; //Force wizard completed flag to false
        //rpc.wizardSettings.completedStep="AutoUpgrades"; //wizard will resume after this step
        //rpc.wizardSettings.steps = ['Welcome','ServerSettings','Wireless','Complete']; //Force different Steps for configurations
        //rpc.wizardSettings.steps = ['Welcome','ServerSettings','AutoUpgrades','Complete']; //Force different Steps for configurations
        //rpc.wizardSettings.steps = ['Welcome','ServerSettings','Interfaces','Internet','InternalNetwork','AutoUpgrades','Complete']; //Force different Steps for configurations
        //------------------

        var cards = [], j, className, clazz;
        for (j = 0; j < rpc.wizardSettings.steps.length; j += 1) {
            className = 'Ung.setupWizard.' + rpc.wizardSettings.steps[j];
            clazz = Ext.create(className, {stepName: rpc.wizardSettings.steps[j]});
            cards.push(clazz.card);
        }

        Ext.create('Ext.container.Viewport', {
            layout: 'auto',
            border: false,
            items: Ext.create('Ung.Wizard', {
                maxWidth: 800,
                minWidth: 320,
                showLogo: true,
                cardDefaults: {
                    labelWidth: Ung.setupWizard.labelWidth,
                    padding: 5
                },
                cards: cards
            })
        });

    },
    authenticate: function (password, handler) {
        Ext.MessageBox.wait(i18n._('Authenticating'), i18n._('Please Wait'));
        Ext.Ajax.request({
            url: '/auth/login?url=/webui&realm=Administrator',
            params: {
                username: 'admin',
                password: password
            },
            // If it uses the default type then this will not work
            // because the authentication handler does not like utf8
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            },
            success: function (response) {
                if (response.responseText && response.responseText.indexOf('loginPage') != -1) {
                    Ext.MessageBox.alert(i18n._('Authentication failed'), i18n._('Invalid password.'));
                    return;
                }

                var setupInfo;
                rpc.jsonrpc = new JSONRpcClient('/webui/JSON-RPC');

                try {
                    setupInfo = rpc.jsonrpc.UvmContext.getSetupStartupInfo();
                } catch (e) {
                    Ung.Util.handleException(e);
                }

                Ext.applyIf(rpc, setupInfo);
                rpc.tolerateKeepAliveExceptions = false;

                rpc.keepAlive = function() {
                    rpc.jsonrpc.UvmContext.getFullVersion(Ext.bind(function (result, exception) {
                        if (!rpc.tolerateKeepAliveExceptions) {
                            if (Ung.Util.handleException(exception)) { return; }
                        }
                        Ext.defer(rpc.keepAlive, 300000);
                    }, this));
                };
                rpc.keepAlive();

                if (Ext.MessageBox.rendered) {
                    Ext.MessageBox.hide();
                }
                handler();
            },
            failure: function (response) {
                Ext.MessageBox.alert(i18n._('Authenticatication failed'), i18n._('The authentication request has failed.'));
            }
        });
    },
    saveCurrentStep: function (stepName) {
        if (!rpc.wizardSettings.wizardComplete) {
            rpc.wizardSettings.completedStep = stepName;
            rpc.jsonrpc.UvmContext.setWizardSettings(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) { return; }
            }, this), rpc.wizardSettings);
        }
    }
});
