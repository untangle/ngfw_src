/**
 * Util singleton
 */
Ext.define('Ung.Setup.Util', {
    alternateClassName: 'Util',
    singleton: true,

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
    ],

    handleException: function (exception) {
        if (Util.ignoreExceptions)
            return;

        var message = null;
        var details = "";

        if ( !exception ) {
            console.error("Null Exception!");
            return;
        } else {
            console.error(exception);
        }

        if ( exception.javaStack )
            exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
        if ( exception.name )
            details += "<b>" + "Exception name".t() +":</b> " + exception.name + "<br/><br/>";
        if ( exception.code )
            details += "<b>" + "Exception code".t() +":</b> " + exception.code + "<br/><br/>";
        if ( exception.message )
            details += "<b>" + "Exception message".t() + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.javaStack )
            details += "<b>" + "Exception java stack".t() +":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.stack )
            details += "<b>" + "Exception js stack".t() +":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( rpc.fullVersionAndRevision != null )
            details += "<b>" + "Build".t() +":&nbsp;</b>" + rpc.fullVersionAndRevision + "<br/><br/>";
        details +="<b>" + "Timestamp".t() +":&nbsp;</b>" + (new Date()).toString() + "<br/><br/>";
        if ( exception.response )
            details += "<b>" + "Exception response".t() +":</b> " + Ext.util.Format.stripTags(exception.response).replace(/\s+/g,'<br/>') + "<br/><br/>";

        /* handle authorization lost */
        if( exception.response && exception.response.includes("loginPage") ) {
            message  = "Session timed out.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        /* handle connection lost */
        if( exception.code==550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
            /* handle connection lost (this happens on windows only for some reason) */
            (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
            /* special text for "method not found" and "Service Temporarily Unavailable" */
            (exception.message && exception.message.indexOf("method not found") != -1) ||
            (exception.message && exception.message.indexOf("Service Unavailable") != -1) ||
            (exception.message && exception.message.indexOf("Service Temporarily Unavailable") != -1) ||
            (exception.message && exception.message.indexOf("This application is not currently available") != -1)) {
            message  = "The connection to the server has been lost.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        if (typeof exception === 'string') {
            Util.showWarningMessage(exception, '', Util.goToStartPage);
        } else {
            Util.showWarningMessage(exception.message, details, Util.goToStartPage);
        }
    },

    showWarningMessage: function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: 'Warning'.t(),
            modal:true,
            closable:false,
            layout: "fit",
            setSizeToRack: function () {
                if(Ung.Main && Ung.Main.viewport) {
                    var objSize = Ung.Main.viewport.getSize();
                    objSize.height = objSize.height - 66;
                    this.setPosition(0, 66);
                    this.setSize(objSize);
                } else {
                    this.maximize();
                }
            },
            doSize: function() {
                var detailsComp = this.down('fieldset[name="details"]');
                if(!detailsComp.isHidden()) {
                    this.setSizeToRack();
                } else {
                    this.center();
                }
            },
            items: {
                xtype: "panel",
                minWidth: 350,
                autoScroll: true,
                defaults: {
                    border: false
                },
                items: [{
                    xtype: "fieldset",
                    padding: 10,
                    items: [{
                        xtype: "label",
                        html: message,
                    }]
                }, {
                    xtype: "fieldset",
                    hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                    items: [{
                        xtype: "button",
                        name: "details_button",
                        text: "Show details".t(),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                wnd.initialHeight = wnd.getHeight();
                                wnd.initialWidth = wnd.getWidth();
                                detailsComp.show();
                                detailsButton.setText('Hide details'.t());
                                wnd.setSizeToRack();
                            } else {
                                detailsComp.hide();
                                detailsButton.setText('Show details'.t());
                                wnd.restore();
                                wnd.setHeight(wnd.initialHeight);
                                wnd.setWidth(wnd.initialWidth);
                                wnd.center();
                            }
                        },
                        scope : this
                    }]
                }, {
                    xtype: "fieldset",
                    name: "details",
                    hidden: true,
                    html: details!=null ? details : ''
                }]
            },
            buttons: [{
                text: 'OK'.t(),
                hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                handler: function() {
                    if ( errorHandler) {
                        errorHandler();
                    } else {
                        wnd.close();
                    }
                }
            }]
        });
        wnd.show();
        if(Ext.MessageBox.rendered) {
            Ext.MessageBox.hide();
        }
    },

    goToStartPage: function () {
        Ext.MessageBox.wait("Redirecting to the start page...".t(), "Please wait".t());
        location.reload();
    },
});




/**
 * For each step (card) the widget alias name corresponds to the wizardSettings steps
 */

/**
 * Welcome card
 */
Ext.define('Ung.Setup.Welcome', {
    extend: 'Ext.panel.Panel',
    title: 'Welcome'.t(),
    alias: 'widget.Welcome',
    controller: 'welcome',
    viewModel: {
        formulas: {
            completedStepText: function (get) {
                return Ext.String.format('The setup was started before and the last completed step is {0}{1}{2}.'.t(), '<b>', get('completedStep') , '</b>')
            },
            nextStepText: function (get) {
                return Ext.String.format('To continue with step {1}{0}{2} fill the admin password and press the {1}Next{2}.'.t(), get('nextStep'), '<b>', '</b>')
            }
        }
    },

    layout: 'card',
    activeItem: null,

    // display Welcome text or resuming options
    bind: {
        activeItem: '{ resuming ? 1 : 0 }'
    },

    items: [{
        layout: 'center',
        border: false,
        items: [{
            xtype: 'component',
            style: { textAlign: 'center' },
            html: '<h1>' + Ext.String.format('Thanks for choosing {0}!'.t(), rpc.oemName) + '</h1>' +
                '<p>' + Ext.String.format('This wizard will guide you through the initial setup and configuration of the {0} Server.'.t(), rpc.oemName) + '</p>'
        }]
    }, {
        xtype: 'form',
        border: false,
        margin: '10 0 0 0',
        defaults: {
            labelWidth: 150,
            labelAlign: 'right',
            msgTarget: 'side'
        },
        items: [{
            xtype: 'radio',
            name: 'continueRadio',
            reference: 'continueSetup',
            inputValue: 'yes',
            boxLabel: '<strong>' + 'Continue the Setup Wizard'.t() + '</strong>',
            checked: true,
            listeners: {
                change: 'setContinue'
            }
        }, {
            xtype: 'component',
            margin: '0 0 20 20',
            bind: { html: '{completedStepText}' }
        }, {
            xtype: 'displayfield',
            margin: 0,
            inputType: 'text',
            fieldLabel: 'Login'.t(),
            value: 'admin',
            labelWidth: 150,
            bind: {
                disabled: '{!continueSetup.checked}'
            }
        }, {
            xtype: 'textfield',
            inputType: 'password',
            fieldLabel: 'Password'.t(),
            name: 'password',
            allowBlank: false,
            bind: {
                disabled: '{!continueSetup.checked}'
            }
        }, {
            xtype: 'component',
            margin: '20 0 0 20',
            bind: { html: '{nextStepText}' }
        }, {
            xtype: 'radio',
            margin: '20 0 0 0',
            name: 'continueRadio',
            inputValue: 'no',
            boxLabel: '<strong>' + 'Restart the Setup Wizard'.t() + '</strong>',
            listeners: {
                change: 'setContinue'
            }
        }]
    }]
});

Ext.define('Ung.Setup.WelcomeController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.welcome',

    control: {
        '#': { activate: 'checkResuming' }
    },

    checkResuming: function () {
        var me = this, vm = this.getViewModel();
        // check if resuming
        if (!rpc.wizardSettings.wizardComplete && rpc.wizardSettings.completedStep != null) {
            var steps = rpc.wizardSettings.steps;
            var nextStepIndex = steps.indexOf(rpc.wizardSettings.completedStep) + 1;
            var completedStepCmp = Ung.app.getMainView().down(rpc.wizardSettings.completedStep);
            var nextStepCmp = Ung.app.getMainView().down(steps[nextStepIndex]);
            vm.set({
                resuming: true,
                completedStep: completedStepCmp.getTitle(),
                nextStep: nextStepCmp.getTitle(),
                nextStepIndex: nextStepIndex
            });

            Ext.defer(function () {
                Ung.app.getMainView().lookup('nextBtn').setText('<strong>' + nextStepCmp.getTitle() + '</strong>&nbsp;&nbsp;<i class="fa fa-arrow-circle-right fa-lg"></i>');
            }, 100);
        } else {
            vm.set('resuming', false);
        }
    },

    // updates the Next button
    setContinue: function (ck) {
        var me = this, vm = this.getViewModel();
        if (ck.getValue()) {
            if (ck.inputValue === 'no') {
                Ung.app.getMainView().lookup('nextBtn').setText('<strong>' + 'Settings'.t() + '</strong>&nbsp;&nbsp;<i class="fa fa-arrow-circle-right fa-lg"></i>');
            } else {
                Ung.app.getMainView().lookup('nextBtn').setText('<strong>' + vm.get('nextStep') + '</strong>&nbsp;&nbsp;<i class="fa fa-arrow-circle-right fa-lg"></i>');
            }
        }
    },

    save: function (cb) {
        var me = this, vm = me.getViewModel();

        if (vm.get('resuming')) {
            var form = me.getView().down('form');

            if (!form.isValid()) { return; }

            if (form.getValues().continueRadio === 'yes') {
                Ung.app.authenticate(form.getValues().password, function () {
                    Ung.app.getMainView().down('#wizard').setActiveItem(vm.get('nextStepIndex'));
                });
            } else {
                cb();
            }
        } else {
            cb();
        }


    }
});

/**
 * Server Settings card
 */
Ext.define('Ung.Setup.ServerSettings', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ServerSettings',
    // controller: 'serversettings',
    title: 'Settings'.t(),
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Configure the Server'.t() + '</h3>'
    }, {
        xtype: 'container',
        defaults: {
            labelAlign: 'right',
            labelWidth: 150,
            msgTarget: 'side',
            validationEvent: 'blur',
        },
        // layout: {
        //     type: 'vbox',
        //     align: 'stretch'
        // },
        items: [{
            xtype: 'component',
            margin: '0 0 10 0',
            html: '<strong>' + 'Choose a password for the admin account.'.t() + '</strong>'
        }, {
            xtype: 'displayfield',
            fieldLabel: 'Login'.t(),
            value: 'admin',
            margin: 0
        }, {
            xtype: 'textfield',
            inputType: 'password',
            width: 350,
            fieldLabel: 'Password'.t(),
            name: 'password',
            id: 'settings_password',
            allowBlank: false,
            minLength: 3,
            minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3)
        }, {
            xtype: 'textfield',
            inputType: 'password',
            width: 350,
            fieldLabel: 'Confirm Password'.t(),
            name: 'confirmPassword',
            allowBlank: false,
            comparePasswordField: 'settings_password',
            vtype: 'passwordConfirmCheck'
        }, {
            xtype: 'fieldcontainer',
            layout: { type: 'hbox', align: 'middle' },
            items: [{
                xtype: 'textfield',
                inputType: 'text',
                name: 'adminEmail',
                fieldLabel: 'Admin Email'.t(),
                labelAlign: 'right',
                labelWidth: 150,
                width: 400,
                allowBlank: true,
                vtype: 'email',
                value: rpc.adminEmail
            }, {
                xtype: 'label',
                margin: '0 0 0 5',
                style: { color: '#999', fontSize: '11px' },
                html: 'Administrators receive email alerts and report summaries.'.t()
            }]

        }, {
            xtype: 'fieldset',
            margin: '20 0 0 0',
            padding: 10,
            title: '<strong>' + 'Install Type'.t() + '</strong>',
            items: [{
                xtype: 'component',
                margin: '0 0 10 0',
                html: 'Install type determines the optimal default settings for this deployment.'.t()
            }, {
                xtype: 'radiogroup',
                name: 'installType',
                simpleValue: true,
                value: null,
                columns: 4,
                vertical: true,
                defaults: {
                    width: 150
                },
                items: [
                    { boxLabel: '<b>' + 'Business'.t() + '</b>', inputValue: 'business' },
                    { boxLabel: '<b>' + 'School'.t() + '</b>', inputValue: 'school' },
                    { boxLabel: '<b>' + 'College/University'.t() + '</b>', inputValue: 'college' },
                    { boxLabel: '<b>' + 'Government'.t() + '</b>', inputValue: 'government' },
                    { boxLabel: '<b>' + 'Non-Profit'.t() + '</b>', inputValue: 'nonprofit' },
                    { boxLabel: '<b>' + 'Home'.t() + '</b>', inputValue: 'home' },
                    { boxLabel: '<b>' + 'Other'.t() + '</b>', inputValue: 'other' }
                ]
            }]
        }, {
            xtype: 'fieldset',
            margin: '20 0 0 0',
            padding: 10,
            title: '<strong>' + 'Timezone'.t() + '</strong>',
            items: [{
                xtype: 'combo',
                name: 'timezone',
                editable: false,
                store: rpc.timezones,
                width: 350,
                queryMode: 'local',
                value: rpc.timezoneID
            }]
        }]
    }],

    controller: {
        save: function (cb) {
            var me = this, form = me.getView(), values = form.getValues();
            if (!form.isValid()) { return; }

            Ung.app.loading('Saving Settings'.t());
            // rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;

            // set timezone
            if (rpc.timezoneID !== values.timezone) {
                rpc.setup.setTimeZone(function (result, ex) {
                    if (ex) { Ung.app.loading(false); Util.handleException(ex); return; }
                    rpc.timezoneID = values.timezone;
                    me.saveAdminPassword(function () { cb(); });
                }, values.timezone);
            } else {
                me.saveAdminPassword(function () { cb(); });
            }
        },

        saveAdminPassword: function (cb) {
            var values = this.getView().getValues();
            rpc.setup.setAdminPassword(function (result, ex) {
                if (ex) { Util.handleException(ex); return; } // Unable to save the admin password
                Ung.app.authenticate(values.password, function () { cb(); });
            }, values.password, values.adminEmail, values.installType);
        }
    }
});

/**
 * Interfaces
 */
Ext.define('Ung.Setup.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Interfaces',
    controller: 'interfaces',
    viewModel: {
        stores: {
            interfaces: {
                data: '{interfacesData}'
            }
        }
    },
    title: 'Network Cards'.t(),
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Identify Network Cards'.t() + '</h3>'
    }, {
        xtype: 'component',
        html: '<i class="fa fa-exclamation-triangle fa-orange"></i> ' + 'This step identifies the external, internal, and other network cards.'.t() + '<br/><br/>' +
            '<strong>' + 'Step 1:'.t() + '</strong> ' +
            'Plug an active cable into one network card to determine which network card it is.'.t() + '<br/>' +
            '<strong>' + 'Step 2:'.t() + '</strong> ' +
            '<strong>' + 'Drag and drop'.t() + '</strong> ' + 'the network card to map it to the desired interface.'.t() + '<br/>' +
            '<strong>' + 'Step 3:'.t() + '</strong> ' +
            'Repeat steps 1 and 2 for each network card and then click <i>Next</i>.'.t()
    }, {
        xtype: 'grid',
        margin: '20 0 0 0',
        flex: 1,
        sortableColumns: false,
        enableColumnResize: true,
        enableColumnHide: false,
        enableColumnMove: false,
        plugins: {
            ptype: 'cellediting',
            clicksToEdit: 1
        },

        bind: {
            store: '{interfaces}'
        },

        viewConfig: {
            plugins: {
                ptype: 'gridviewdragdrop',
                dragText: 'Drag and drop to reorganize'.t(),
                dragZone: {
                    onBeforeDrag: function (data, e) {
                        return Ext.get(e.target).hasCls('fa-arrows');
                    }
                }
            },
            listeners: {
                drop: 'onDropInterface'
            }
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            sortable: false,
            width: 90,
            renderer: function (value) {
                return value.t();
            }
        }, {
            xtype: 'gridcolumn',
            header: '<i class="fa fa-sort"></i>',
            align: 'center',
            width: 30,
            resizable: false,
            tdCls: 'action-cell',
            renderer: function() {
                return '<i class="fa fa-arrows" style="cursor: move;"></i>';
            },
        }, {
            header: 'Device'.t(),
            tooltip: 'Click on a Device to open a combo and choose the desired Device from a list. When anoter Device is selected the 2 Devices are swithced.'.t(),
            tooltipType: 'title',
            dataIndex: 'deviceName',
            editor: {
                xtype: 'combo',
                bind: {
                    store: '{deviceStore}'
                },
                // store: physicalDevsStore,
                editable: false,
                valueField: 'physicalDev',
                displayField: 'physicalDev',
                queryMode: 'local',
                listeners: {
                    change: 'setInterfacesMap'
                }
            }
        }, {
            dataIndex: 'connected',
            sortable: false,
            resizable: false,
            width: 30,
            align: 'center',
            renderer: function (value) {
                switch (value) {
                case 'CONNECTED': return '<i class="fa fa-circle fa-green"></i>';
                case 'DISCONNECTED': return '<i class="fa fa-circle fa-gray"></i>';
                case 'MISSING': return '<i class="fa fa-exclamation-triangle fa-orange"></i>';
                default: return '<i class="fa fa-question-circle fa-gray"></i>';
                }
            }
        }, {
            header: 'Status'.t(),
            dataIndex: 'connected',
            sortable: false,
            flex: 1,
            renderer: Ext.bind(function (value, metadata, record, rowIndex, colIndex, store, view) {
                var connected = record.get('connected');
                var mbit = record.get('mbit');
                var duplex = record.get('duplex');
                var vendor = record.get('vendor');

                var connectedStr = (connected == 'CONNECTED') ? 'connected'.t() : (connected == 'DISCONNECTED') ? 'disconnected'.t() : 'unknown'.t();
                var duplexStr = (duplex == 'FULL_DUPLEX') ? 'full-duplex'.t() : (duplex == 'HALF_DUPLEX') ? 'half-duplex'.t() : 'unknown'.t();
                return connectedStr + ' ' + mbit + ' ' + duplexStr + ' ' + vendor;
            }, this)
        }, {
            header: 'MAC Address'.t(),
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
    }]
});

Ext.define('Ung.Setup.InterfacesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.interfaces',

    control: {
        '#': { activate: 'getInterfaces' }
    },

    getInterfaces: function () {

        var me = this, grid = me.getView().down('grid'), vm = me.getViewModel();

        me.physicalDevsStore = [];
        me.intfOrderArr = [];

        Ung.app.loading('Fetch interfaces...'.t());
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            Ung.app.loading(false);
            if (ex) { Util.handleException('Unable to refresh the interfaces.'.t()); return; }

            vm.set('networkSettings', result);
            var interfaces = [], devices = [];

            Ext.Array.each(result.interfaces.list, function (intf) {
                if (!intf.isVlanInterface) {
                    interfaces.push(intf);
                    devices.push({physicalDev: intf.physicalDev});
                }
            });

            rpc.networkManager.getDeviceStatus(function (result2, ex2) {
                Ung.app.loading(false);
                if (ex2) { Util.handleException(ex2); return; }

                var deviceStatusMap = Ext.Array.toValueMap(result2.list, 'deviceName');
                Ext.Array.forEach(interfaces, function (intf) {
                    Ext.applyIf(intf, deviceStatusMap[intf.physicalDev]);
                });

                vm.set('interfacesData', interfaces);

                Ext.Array.each(interfaces, function (intf) {
                    me.physicalDevsStore.push([intf.physicalDev, intf.physicalDev]);
                    me.intfOrderArr.push(Ext.clone(intf));
                });
                vm.set('deviceStore', me.physicalDevsStore);
                if (interfaces.length < 2) {
                    Ext.MessageBox.alert('Missing interfaces'.t(), 'Untangle requires two or more network cards. Please reinstall with at least two network cards.'.t(), '');
                }
            });
        });
    },

    // used when mapping by dragging
    onDropInterface: function (app, data, overModel, dropPosition, eOpts) {
        var me = this, vm = me.getViewModel(), i = 0;

        vm.getStore('interfaces').each(function( currentRow ) {
            var intf = me.intfOrderArr[i];
            currentRow.set({
                interfaceId: intf.interfaceId,
                name: intf.name
            });
            i++;
        });
    },

    // used when mapping from comboboxes
    setInterfacesMap: function (elem, newValue, oldValue) {
        var vm = this.getViewModel(), sourceRecord = null, targetRecord = null;

        vm.getStore('interfaces').each( function( currentRow ) {
            if (oldValue === currentRow.get('physicalDev')) {
                sourceRecord = currentRow;
            } else if (newValue === currentRow.get('physicalDev')) {
                targetRecord = currentRow;
            }
        });
        // make sure sourceRecord & targetRecord are defined
        if (sourceRecord === null || targetRecord === null || sourceRecord === targetRecord) {
            return;
        }

        // clone phantom records to manipulate (switch) data properly
        var sourceRecordCopy = sourceRecord.copy(null),
            targetRecordCopy = targetRecord.copy(null);

        // switch data between records (interfaces) - remapping
        sourceRecord.set({
            deviceName: newValue,
            physicalDev: targetRecordCopy.get('physicalDev'),
            systemDev:   targetRecordCopy.get('systemDev'),
            symbolicDev: targetRecordCopy.get('symbolicDev'),
            macAddress:  targetRecordCopy.get('macAddress'),
            duplex:      targetRecordCopy.get('duplex'),
            vendor:      targetRecordCopy.get('vendor'),
            mbit:        targetRecordCopy.get('mbit'),
            connected:   targetRecordCopy.get('connected')
        });
        targetRecord.set({
            deviceName: oldValue,
            physicalDev: sourceRecordCopy.get('physicalDev'),
            systemDev:   sourceRecordCopy.get('systemDev'),
            symbolicDev: sourceRecordCopy.get('symbolicDev'),
            macAddress:  sourceRecordCopy.get('macAddress'),
            duplex:      sourceRecordCopy.get('duplex'),
            vendor:      sourceRecordCopy.get('vendor'),
            mbit:        sourceRecordCopy.get('mbit'),
            connected:   sourceRecordCopy.get('connected')
        });
    },

    save: function (cb) {
        var me = this, vm = me.getViewModel(), grid = me.getView().down('grid');

        // if no remapping just skip to next step
        if (grid.getStore().getModifiedRecords().length === 0) {
            cb(); return;
        }

        Ung.app.loading('Saving Settings'.t());
        rpc.networkManager.setNetworkSettings(function (result, ex) {
            Ung.app.loading(false);
            if (ex) { Util.handleException(ex); return; }
            cb();
        }, vm.get('networkSettings'));
    }
});

/**
 * Internet Connection
 */
Ext.define('Ung.Setup.Internet', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Internet',
    controller: 'internet',
    viewModel: {
        data: {
            wan: null
        }
    },
    title: 'Internet Connection'.t(),
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Configure the Internet Connection'.t() + '</h3>'
    }, {
        xtype: 'radiogroup',
        fieldLabel: 'Configuration Type'.t(),
        labelWidth: 160,
        labelAlign: 'right',
        simpleValue: true,
        layout: { type: 'hbox' },
        defaults: { padding: '1 15 1 0' },
        items: [
            { boxLabel: '<strong>' + 'Auto (DHCP)'.t() + '</strong>', inputValue: 'AUTO' },
            { boxLabel: '<strong>' + 'Static'.t() + '</strong>', inputValue: 'STATIC' },
            { boxLabel: '<strong>' + 'PPPoE'.t() + '</strong>', inputValue: 'PPPoE' }
        ],
        bind: {
            value: '{wan.v4ConfigType}'
        }
    }, {
        xtype: 'panel',
        layout: 'card',
        border: false,
        flex: 1,
        margin: '10 0 0 0',
        defaults: {
            xtype: 'form',
            border: false,
            bodyBorder: false,
            // margin: '10 0 0 0'
        },
        bind: {
            activeItem: '{wan.v4ConfigType}'
        },
        items: [{
            itemId: 'AUTO',
            items: [{
                xtype: 'fieldset',
                title: 'DHCP Status'.t(),
                padding: 10,
                defaults: {
                    xtype: 'displayfield',
                    labelWidth: 150,
                    labelAlign: 'right',
                    margin: 0
                },
                items: [{
                    fieldLabel: 'Current IP Address'.t(),
                    bind: { value: '{wan.v4Address}' }
                }, {
                    fieldLabel: 'Current Netmask'.t(),
                    bind: { value: '{wan.v4Netmask}' }
                }, {
                    fieldLabel: 'Current Gateway'.t(),
                    bind: { value: '{wan.v4Gateway}' }
                }, {
                    fieldLabel: 'Current Primary DNS'.t(),
                    bind: { value: '{wan.v4Dns1}' }
                }, {
                    fieldLabel: 'Current Secondary DNS'.t(),
                    bind: { value: '{wan.v4Dns2}' }
                }]
            }]
        }, {
            itemId: 'STATIC',
            items: [{
                xtype: 'fieldset',
                title: 'Static'.t(),
                padding: 10,
                defaults: {
                    xtype: 'textfield',
                    labelWidth: 150,
                    width: 350,
                    labelAlign: 'right',
                    msgTarget: 'side',
                    validationEvent: 'blur',
                    maskRe: /(\d+|\.)/,
                    vtype: 'ipAddress'
                },
                items: [{
                    fieldLabel: 'IP Address'.t(),
                    allowBlank: false,
                    bind: { value: '{wan.v4StaticAddress}', emptyText: '{wan.v4Address}' }
                }, {
                    fieldLabel: 'Netmask'.t(),
                    xtype: 'combo',
                    store: Util.v4NetmaskList,
                    queryMode: 'local',
                    triggerAction: 'all',
                    value: 24,
                    bind: { value: '{wan.v4StaticPrefix}', emptyText: '/{wan.v4PrefixLength} - {wan.v4Netmask}' },
                    editable: false,
                    allowBlank: false
                }, {
                    fieldLabel: 'Gateway'.t(),
                    allowBlank: false,
                    bind: { value: '{wan.v4StaticGateway}', emptyText: '{wan.v4Gateway}' }
                }, {
                    fieldLabel: 'Primary DNS'.t(),
                    allowBlank: false,
                    bind: { value: '{wan.v4StaticDns1}', emptyText: '{wan.v4Dns1}' }
                }, {
                    xtype: 'fieldcontainer',
                    width: 'auto',
                    layout: { type: 'hbox', align: 'middle' },
                    items: [{
                        xtype: 'textfield',
                        labelWidth: 150,
                        width: 350,
                        labelAlign: 'right',
                        msgTarget: 'side',
                        validationEvent: 'blur',
                        maskRe: /(\d+|\.)/,
                        vtype: 'ipAddress',
                        name: 'dns2',
                        fieldLabel: 'Secondary DNS'.t(),
                        allowBlank: true,
                        bind: { value: '{wan.v4StaticDns2}', emptyText: '{wan.v4Dns2}' }
                    }, {
                        xtype: 'label',
                        margin: '0 0 0 5',
                        style: { color: '#999', fontSize: '11px' },
                        html: '(optional)'.t()
                    }]
                }]
            }]
        }, {
            itemId: 'PPPoE',
            items: [{
                xtype: 'fieldset',
                title: 'PPPoE Settings'.t(),
                padding: 10,
                defaults: {
                    xtype: 'textfield',
                    labelWidth: 150,
                    width: 350,
                    labelAlign: 'right'
                },
                items: [{
                    fieldLabel: 'Username'.t(),
                    bind: { value: '{wan.v4PPPoEUsername}' }
                }, {
                    inputType: 'password',
                    fieldLabel: 'Password'.t(),
                    bind: { value: '{wan.v4PPPoEPassword}' }
                }]
            }, {
                xtype: 'fieldset',
                title: 'PPPoE Status'.t(),
                padding: 10,
                defaults: {
                    xtype: 'displayfield',
                    labelWidth: 150,
                    labelAlign: 'right',
                    margin: 0
                },
                items: [
                    { fieldLabel: 'IP Address'.t(),    bind: { value: '{wan.v4Address}' } },
                    { fieldLabel: 'Netmask'.t(),       bind: { value: '{wan.v4Netmask}' } },
                    { fieldLabel: 'Gateway'.t(),       bind: { value: '{wan.v4Gateway}' } },
                    { fieldLabel: 'Primary DNS'.t(),   bind: { value: '{wan.v4Dns1}' } },
                    { fieldLabel: 'Secondary DNS'.t(), bind: { value: '{wan.v4Dns2}' } }
                ]
            }]
        }]
    }, {
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'middle',
            pack: 'center'
        },
        defaults: {
            xtype: 'button',
            margin: '0 5'
        },
        items: [{
            text: 'Renew DHCP'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refresh', // refetch settings to refresh
            bind: {
                hidden: '{wan.v4ConfigType !== "AUTO"}'
            }
        }, {
            text: 'Test Connectivity'.t(),
            iconCls: 'fa fa-compress',
            handler: 'save', // save is called because connectivity test is done inside of it
            bind: {
                hidden: '{wan.v4ConfigType !== "AUTO" && wan.v4ConfigType !== "STATIC" }'
            }
        }]
    }],
});

Ext.define('Ung.Setup.InternetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.internet',
    control: {
        '#': { activate: 'getSettings' }
    },

    // getNetworkSettings
    getSettings: function () {
        // Ext.MessageBox.wait('Refreshing...'.t(), 'Please Wait'.t());

        var me = this, vm = me.getViewModel(), networkSettings = null, firstWan = null, firstWanStatus;
        try {
            networkSettings = rpc.networkManager.getNetworkSettings();
        } catch (e) {
            // @todo: proper handle exceptions
            Util.handleException(e);
        }
        if (networkSettings) {
            vm.set('networkSettings', networkSettings);
        } else {
            Util.handleException('Unable to fetch network settings!'); return;
        }

        // get the first wan
        firstWan = Ext.Array.findBy(networkSettings.interfaces.list, function (intf) {
            return (intf.isWan && intf.configType !== 'DISABLED');
        });

        // get first wan status
        try {
            firstWanStatus = rpc.networkManager.getInterfaceStatus(firstWan.interfaceId);
        } catch (e) { Util.handleException(e); }

        if (firstWanStatus) {
            Ext.applyIf(firstWan, firstWanStatus); // apply status on wan
        } else {
            Util.handleException('Unable to fetch WAN interface status!');
        }

        vm.set('wan', firstWan);
    },

    save: function (cb) {
        var me = this, vm = this.getViewModel();

        // validate any current form first
        if (!me.getView().down('panel').getLayout().getActiveItem().isValid()) {
            return;
        }

        // before saving clear unwanted settings
        var wan = vm.get('wan');
        if (wan.v4ConfigType === 'AUTO' || wan.v4ConfigType === 'PPPoE') {
            wan.v4StaticAddress = null;
            wan.v4StaticPrefix = null;
            wan.v4StaticGateway = null;
            wan.v4StaticDns1 = null;
            wan.v4StaticDns2 = null;
        }
        if (wan.v4ConfigType === 'STATIC') {
            wan.v4NatEgressTraffic = true;
        }
        if (wan.v4ConfigType === 'PPPoE') {
            wan.v4NatEgressTraffic = true;
            wan.v4PPPoEUsePeerDns = true;
        }

        // save
        Ung.app.loading('Saving settings ...'.t());
        rpc.networkManager.setNetworkSettings(function (response, ex) {
            if (ex) { Util.handleException(ex); return; }
            me.testConnectivity(Ext.isFunction(cb) ? 'auto' : 'manual', function () {
                cb();
            });
        }, vm.get('networkSettings'));
    },

    testConnectivity: function (testType, cb) {
        var me = this, vm = me.getViewModel();

        Ung.app.loading('Testing Connectivity...'.t());
        rpc.connectivityTester.getStatus(function (result, ex) {
            Ung.app.loading(false);
            if (ex) {
                Ext.MessageBox.show({
                    title: 'Network Settings'.t(),
                    msg: 'Unable to complete connectivity test, please try again.'.t(),
                    width: 300,
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.INFO
                });
                return;
            }

            // build test fail message if any
            var message = null;
            if (result.tcpWorking === false  && result.dnsWorking === false) {
                message = 'Warning! Internet tests and DNS tests failed.'.t();
            } else if (result.tcpWorking === false) {
                message = 'Warning! DNS tests succeeded, but Internet tests failed.'.t();
            } else if (result.dnsWorking === false) {
                message = 'Warning! Internet tests succeeded, but DNS tests failed.'.t();
            } else {
                message = null;
            }

            if (testType === 'manual') {
                // on manual test just show the message
                Ext.MessageBox.show({
                    title: 'Internet Status'.t(),
                    msg: message || 'Success!'.t(),
                    width: 300,
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.INFO
                });
            } else {
                // on next step just move forward if no failures
                if (!message) { cb(); return; }

                // otherwise show a warning message
                var warningText = message + '<br/><br/>' + 'It is recommended to configure valid internet settings before continuing. Try again?'.t();
                Ext.Msg.confirm('Warning:'.t(), warningText, function (btn, text) {
                    if (btn === 'yes') { return; }
                    cb();
                });
            }
        });

    },

    refresh: function () {
        var me = this, vm = me.getViewModel();
        Ung.app.loading('Saving settings ...'.t());
        // save settings before
        rpc.networkManager.setNetworkSettings(function (response, ex) {
            if (ex) { Util.handleException(ex); return; }
            Ung.app.loading(false);
            me.getSettings();
        }, vm.get('networkSettings'));
    }

});

/**
 * Internal Network
 */
Ext.define('Ung.Setup.InternalNetwork', {
    extend: 'Ext.form.Panel',
    alias: 'widget.InternalNetwork',
    controller: 'internalnetwork',
    title: 'Internal Network'.t(),
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Configure the Internal Network Interface'.t() + '</h3>'
    }, {
        xtype: 'container',
        layout: {
            type: 'column',
        },
        items: [{
            xtype: 'container',
            columnWidth: 0.7,
            defaults: {
                padding: '0 0 0 10'
            },
            items: [{
                xtype: 'radio',
                reference: 'routerRadio',
                name: 'configType',
                inputValue: 'ROUTER',
                boxLabel: '<strong>' + 'Router'.t() + '</strong>',
                padding: 0,
                bind: {
                    value: '{nonWan.configType !== "BRIDGED"}'
                },
                listeners: {
                    change: 'setConfigType'
                }
            }, {
                xtype: 'component',
                margin: '0 0 10 0',
                html: 'This is recommended if the external port is plugged into the internet connection. This enables NAT and DHCP.'.t()
            }, {
                xtype: 'textfield',
                labelWidth: 150,
                width: 350,
                labelAlign: 'right',
                fieldLabel: 'Internal Address'.t(),
                vText: 'Please enter a valid Network  Address'.t(),
                vtype: 'ipAddress',
                allowBlank: false,
                msgTarget: 'side',
                maskRe: /(\d+|\.)/,
                disabled: true,
                value: '192.168.1.1',
                validationEvent: 'blur',
                bind: { value: '{nonWan.v4StaticAddress}', disabled: '{!routerRadio.checked}' }
            }, {
                labelWidth: 150,
                width: 350,
                labelAlign: 'right',
                fieldLabel: 'Internal Netmask'.t(),
                xtype: 'combo',
                store: Util.v4NetmaskList,
                queryMode: 'local',
                triggerAction: 'all',
                disabled: true,
                editable: false,
                bind: { value: '{nonWan.v4StaticPrefix || 24}', disabled: '{!routerRadio.checked}' }
            }, {
                xtype: 'checkbox',
                margin: '0 0 0 155',
                disabled: true,
                boxLabel: 'Enable DHCP Server (default)'.t(),
                bind: { value: '{nonWan.dhcpEnabled}', disabled: '{!routerRadio.checked}' }
            }]
        }, {
            xtype: 'component',
            columnWidth: 0.3,
            margin: 20,
            html: '<img src="/skins/' + rpc.skinName + '/images/admin/wizard/router.png"/>'
        }]
    }, {
        xtype: 'container',
        margin: '10 0 0 0',
        layout: {
            type: 'column',
        },
        items: [{
            xtype: 'container',
            columnWidth: 0.7,
            defaults: {
                padding: '0 0 0 10'
            },
            items: [{
                xtype: 'radio',
                reference: 'bridgeRadio',
                name: 'configType',
                inputValue: 'BRIDGED',
                boxLabel: '<strong>' + 'Transparent Bridge'.t() + '</strong>',
                padding: 0,
                bind: {
                    value: '{nonWan.configType === "BRIDGED"}'
                },
                listeners: {
                    change: 'setConfigType'
                }
            }, {
                xtype: 'component',
                margin: '0 0 10 0',
                html: 'This is recommended if the external port is plugged into a firewall/router. This bridges Internal and External and disables DHCP.'.t()
            }]
        }, {
            xtype: 'component',
            columnWidth: 0.3,
            margin: 20,
            html: '<img src="/skins/' + rpc.skinName + '/images/admin/wizard/bridge.png"/>'
        }]
    }],
});

Ext.define('Ung.Setup.InternalNetworkController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.internalnetwork',
    control: {
        '#': { activate: 'getSettings' }
    },

    getSettings: function () {
        var me = this, vm = me.getViewModel(), networkSettings = null, nonWan;
        try {
            networkSettings = rpc.networkManager.getNetworkSettings();
        } catch (e) {
            // @todo: proper handle exceptions
            Util.handleException(e);
        }
        if (networkSettings && networkSettings.interfaces && networkSettings.interfaces.list) {
            vm.set('networkSettings', networkSettings);
        } else {
            Util.handleException('Unable to fetch network settings!'); return;
        }

        // find first non wan
        nonWan = Ext.Array.findBy(networkSettings.interfaces.list, function (intf) {
            return !intf.isWan;
        });

        if (nonWan) {
            vm.set('nonWan', nonWan);
        } else {
            Util.handleException('Unable to find internal network!');
            return;
        }

        me.initialConfigType = nonWan.configType;
        me.initialv4Address = nonWan.v4StaticAddress;
        me.initialv4Prefix = nonWan.v4StaticPrefix;
        me.initialDhcpEnabled = nonWan.dhcpEnabled;

    },

    setConfigType: function (radio) {
        var me = this, vm = me.getViewModel();
        if (radio.checked) {
            if (radio.inputValue === 'BRIDGED') {
                vm.set('nonWan.configType', 'BRIDGED');
            } else {
                vm.set('nonWan.configType', 'ADDRESSED');
                vm.set('nonWan.v4configType', 'STATIC');
            }
        }
    },

    save: function (cb) {
        var me = this, form = me.getView(), vm = me.getViewModel(),
            firstWan, firstWanStatus, newSetupLocation;
        if (!form.isValid()) { return; }

        if ( // no changes made
            me.initialConfigType === vm.get('nonWan.configType') &&
            me.initialv4Address === vm.get('nonWan.v4StaticAddress') &&
            me.initialv4Prefix === vm.get('nonWan.v4StaticPrefix') &&
            me.initialDhcpEnabled === vm.get('nonWan.dhcpEnabled')
        ) {
            cb();
            return;
        }


        // BRIDGED (bridge mode)
        if (vm.get('nonWan.configType') === 'BRIDGED') {
            if (window.location.hostname === vm.get('nonWan.v4StaticAddress')) {
                // get firstWan settings & status
                firstWan = Ext.Array.findBy(vm.get('networkSettings').interfaces.list, function (intf) {
                    return intf.isWan && intf.configType !== 'DISABLED';
                });

                // if we found the first WAN
                if (firstWan && firstWan.interfaceId) {
                    try {
                        firstWanStatus = rpc.networkManager.getInterfaceStatus(firstWan.interfaceId);
                    } catch (e) {
                        Util.handleException(e);
                    }
                    // and the first WAN has a address
                    if (firstWanStatus.v4Address) {
                        //Use Internal Address instead of External Address
                        newSetupLocation = window.location.href.replace(vm.get('nonWan.v4StaticAddress'), firstWanStatus.v4Address);
                        rpc.keepAlive = function () {}; // prevent keep alive
                        Ext.defer(function () {
                            Ext.MessageBox.confirm('Redirect to the new setup address?'.t(),
                                                   Ext.String.format('This change will alter the internal IP address and the setup wizard is no longer accessible using the Internal Address. Instead it could be accessible using the External Address: {0}'.t(), firstWanStatus.v4Address) + '<br/><br/>' +
                                                   Ext.String.format('To redirect to the new setup address: {0} press Yes.'.t(), '<a href="' + newSetupLocation + '">' + newSetupLocation + '</a>') + '<br/><br/>' +
                                                   'To continue setup using the current address click No, but setup might no longer be accessible.'.t(),
                                                   function (btn) {
                                                       if (btn == 'yes') {
                                                           console.log("Redirecting to " + newSetupLocation);
                                                           window.location.href = newSetupLocation;
                                                       } else {
                                                           rpc.tolerateKeepAliveExceptions = false;
                                                       }
                                                   });
                        }, 5000);
                    }
                }
            }
            Ung.app.loading('Saving Internal Network Settings'.t());
            rpc.networkManager.setNetworkSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('networkSettings'));
        } else { // ADDRESSED (router)
            vm.set('nonWan.dhcpRangeStart', null);
            vm.set('nonWan.dhcpRangeEnd', null);
            if (window.location.hostname !== 'localhost') {
                if (window.location.hostname == me.initialv4Address && me.initialv4Address != vm.get('nonWan.v4StaticAddress')) {
                    //If using internal address and it is changed in this step redirect to new internal address
                    newSetupLocation = window.location.href.replace(me.initialv4Address, vm.get('nonWan.v4StaticAddress'));
                    rpc.keepAlive = function () {}; // prevent keep alive
                    Ext.MessageBox.wait('Saving Internal Network Settings'.t() + '<br/><br/>' +
                                        Ext.String.format('The Internal Address is changed to: {0}'.t(), vm.get('nonWan.v4StaticAddress')) + '<br/>' +
                                        Ext.String.format('The changes are applied and you will be redirected to the new setup address: {0}'.t(), '<a href="' + newSetupLocation + '">' + newSetupLocation + '</a>') + '<br/><br/>' +
                                        'If the new location is not loaded after 30 seconds please reinitialize your local network address and try again.'.t(), 'Please Wait'.t());
                    Ext.defer(function () {
                        window.location.href = newSetupLocation;
                    }, 30000);
                }
            }
            Ung.app.loading('Saving Internal Network Settings'.t());
            rpc.networkManager.setNetworkSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('networkSettings'));
        }
    }

});

/**
 * Wireless
 */
Ext.define('Ung.Setup.Wireless', {
    extend: 'Ext.form.Panel',
    alias: 'widget.Wireless',
    controller: 'wireless',
    viewModel: true,
    title: 'Wireless'.t(),
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Configure Wireless Settings'.t() + '</h3>'
    }, {
        xtype: 'container',
        defaults: {
            labelAlign: 'right',
            labelWidth: 150,
            msgTarget: 'side',
            validationEvent: 'blur',
        },
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Network Name (SSID)'.t(),
            width: 350,
            maxLength: 30,
            maskRe: /[a-zA-Z0-9\-_=]/,
            bind: {
                value: '{wirelessSettings.ssid}'
            },
            allowBlank: false
        }, {
            xtype: 'combo',
            fieldLabel: 'Encryption'.t(),
            width: 300,
            editable: false,
            store: [['NONE', 'None'.t()], ['WPA1', 'WPA'.t()], ['WPA12', 'WPA / WPA2'.t()], ['WPA2', 'WPA2'.t()]],
            bind: {
                value: '{wirelessSettings.encryption}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            width: 350,
            maxLength: 63,
            minLength: 8,
            maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/,
            bind: {
                value: '{wirelessSettings.password}'
            },
            validator: function (val) {
                if (!val || val.length < 8) {
                    return 'The wireless password must be at least 8 characters.'.t();
                }
                if (val === '12345678') {
                    return 'You must choose a new and different wireless password.'.t();
                }
                return true;
            }
        }]
    }]
});


Ext.define('Ung.Setup.WirelessController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.wireless',
    control: {
        '#': { activate: 'getSettings' }
    },

    getSettings: function () {
        var me = this, vm = this.getViewModel();
        Ung.app.loading('Loading Wireless Settings');
        Ext.Deferred.sequence([
            me.getSsid,
            me.getEncryption,
            me.getPassword
        ]).then(function (result) {
            vm.set('wirelessSettings', {
                ssid: result[0] || '',
                encryption: result[1] || 'NONE',
                password: (!result[2] || result[2] === '12345678') ? '' : result[2]
            });

            me.initialSettings = Ext.clone(vm.get('wirelessSettings'));

        }, function (ex) {
            Util.handleException(ex);
        }).always(function(){
            Ung.app.loading(false);
        });
    },

    getSsid: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getWirelessSsid(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },

    getEncryption: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getWirelessEncryption(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },

    getPassword: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getWirelessPassword(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },

    save: function (cb) {
        me = this, form = me.getView(), vm = me.getViewModel();

        // if invalid form or no changes
        if (!form.isValid() || Ext.Object.equals(me.initialSettings, vm.get('wirelessSettings'))) {
            return;
        }

        Ung.app.loading('Saving Settings'.t());
        rpc.networkManager.setWirelessSettings(function (result, ex) {
            Ung.app.loading(false);
            if (ex) { Util.handleException(ex); return; }
            cb();
        }, vm.get('wirelessSettings.ssid'),
           vm.get('wirelessSettings.encryption'),
           vm.get('wirelessSettings.password'));
    }
});


/**
 * AutoUpgrades
 */
Ext.define('Ung.Setup.AutoUpgrades', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.AutoUpgrades',
    controller: 'autoupgrades',
    viewModel: true,
    title: 'Automatic Upgrades'.t(),
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [{
        xtype: 'component',
        html: '<h3>' + 'Configure Automatic Upgrade Settings'.t() + '</h3>'
    }, {
        xtype: 'container',
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + 'Automatically Install Upgrades'.t() + '</strong>',
            bind: { value: '{systemSettings.autoUpgrade}' }
        }, {
            xtype: 'component',
            margin: '0 0 0 20',
            html: 'Automatically install new versions of the software when available.'.t() + '<br/>' +
                'This is the recommended choice for most sites.'.t()
        }]
    }, {
        xtype: 'container',
        margin: '20 0 0 0',
        items: [{
            xtype: 'checkbox',
            boxLabel: '<strong>' + Ext.String.format('Connect to {0} Cloud'.t(), rpc.oemName) + '</strong>',
            bind: { value: '{systemSettings.cloudEnabled}' }
        }, {
            xtype: 'component',
            margin: '0 0 0 20',
            html: Ext.String.format('Remain securely connected to the {0} cloud for cloud management, hot fixes, and support access.'.t(), rpc.oemName) + '<br/>' +
                'This is the recommended choice for most sites.'.t()
        }]
    }]
});

Ext.define('Ung.Setup.AutoUpgradesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.autoupgrades',
    control: {
        '#': { activate: 'getSettings' }
    },

    getSettings: function () {
        var me = this, vm = me.getViewModel();

        Ung.app.loading('Loading Automatic Upgrades Settings'.t());
        rpc.systemManager.getSettings(function (result, ex) {
            Ung.app.loading(false);
            if (ex) { Util.handleException(ex); return; }
            vm.set('systemSettings', result);

            // keep initial values for checking changes
            me.initialValues = {
                autoUpgrade: result.autoUpgrade,
                cloudEnabled: result.cloudEnabled
            };
        });
    },

    save: function (cb) {
        var me = this, vm = me.getViewModel();
        // if no changes skip to next step
        if (
            me.initialValues.autoUpgrade === vm.get('systemSettings.autoUpgrade') &&
            me.initialValues.cloudEnabled === vm.get('systemSettings.cloudEnabled')
        ) { cb(); return; }

        // if cloud enabled, enable support also
        if (vm.get('systemSettings.cloudEnabled')) {
            vm.set('systemSettings.supportEnabled', true);
        }

        Ung.app.loading('Saving Settings...'.t());
        rpc.systemManager.setSettings(function (result, ex) {
            Ung.app.loading(false);
            if (ex) { Util.handleException(ex); return; }
            cb();
        }, vm.get('systemSettings'));
    }
});

/**
 * Complete
 */
Ext.define('Ung.Setup.Complete', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Complete',
    title: 'Finished'.t(),
    layout: 'center',
    items: [{
        xtype: 'container',
        layout: {
            type: 'vbox',
            align: 'middle'
        },
        items: [{
            xtype: 'component',
            style: { textAlign: 'center' },
            html: Ext.String.format('<b>The {0} Server is now configured.</b><br/><br/>You are now ready to configure the applications.'.t(), rpc.oemName)
        }, {
            xtype: 'button',
            margin: '30 0 0 0',
            text: 'Done'.t(),
            iconCls: 'fa fa-check',
            handler: function () {
                Ext.MessageBox.wait('Loading User Interface...'.t(), 'Please Wait'.t());
                //and set a flag so the wizard wont run again
                rpc.jsonrpc.UvmContext.wizardComplete(function (result, ex) {
                    if (ex) { Util.handleException(ex); return; }
                    window.location.href = "/admin/index.do";
                });
            }
        }]
    }]
});

/**
 * Main View
 */
Ext.define('Ung.Setup.Main', {
    extend: 'Ext.container.Viewport',
    controller: 'main',

    viewModel: true,
    // layout: 'center',
    // cardDefaults: {
    //     labelWidth: Ung.setupWizard.labelWidth,
    //     padding: 5
    // },
    // cards: cards
    layout: 'center',
    items: {
        xtype: 'container',
        itemId: 'main',
        border: false,
        width: 800,
        height: 520,
        layout: 'fit',
        items: [{
            xtype: 'panel',
            layout: 'card',
            itemId: 'wizard',
            title: 'Test',
            defaults: {
                header: false, // header is managed by the wizard container
                border: false,
                bodyPadding: 10
            },
            activeItem: null,
            items: [],
            bbar: [{
                    itemId: 'prevBtn',
                    reference: 'prevBtn',
                    scale: 'medium',
                    handler: 'onPrev'
                }, '->', {
                    itemId: 'nextBtn',
                    reference: 'nextBtn',
                    scale: 'medium',
                    handler: 'onNext'
                }]
        }]
    },
});

Ext.define('Ung.Setup.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.main',
    viewModel: true,
    control: {
        '#wizard > panel': { activate: 'onActivate' }
    },

    onActivate: function (panel) {
        var me = this, wizard = me.getView().down('#wizard'), layout = wizard.getLayout();

        wizard.setTitle(panel.getTitle());

        if (layout.getPrev()) {
            me.lookup('prevBtn').setText('<i class="fa fa-arrow-circle-left fa-lg"></i>&nbsp;&nbsp;<strong>' + layout.getPrev().getTitle() + '</strong>');
            me.lookup('prevBtn').setHidden(false);
        } else {
            me.lookup('prevBtn').setHidden(true);
        }

        if (layout.getNext()) {
            me.lookup('nextBtn').setText('<strong>' + layout.getNext().getTitle() + '</strong>&nbsp;&nbsp;<i class="fa fa-arrow-circle-right fa-lg"></i>');
            me.lookup('nextBtn').setHidden(false);
        } else {
            me.lookup('nextBtn').setHidden(true);
        }

        // update the progress steps
        Ext.Array.each(wizard.tools, function (step, idx) {
            if (idx < wizard.items.indexOf(layout.getActiveItem())) {
                step.setHtml('<i class="fa fa-check-square-o" style="color: green;"></i>');
            } else {
                step.setHtml('<i class="fa fa-square-o"></i>');
            }
        });
    },

    onPrev: function () {
        var wizard = this.getView().down('#wizard');
        // console.log('on next');
        wizard.getLayout().prev();
    },

    onNext: function () {
        var me = this, layout = this.getView().down('#wizard').getLayout();

        layout.getActiveItem().getController().save(function () {
            // update completed step

            var stepName = layout.getActiveItem().getXType();

            if (!rpc.wizardSettings.wizardComplete && stepName !== 'Welcome') {
                rpc.wizardSettings.completedStep = layout.getActiveItem().getXType();
                rpc.jsonrpc.UvmContext.setWizardSettings(function (result, ex) {
                    if (ex) { Util.handleException(ex); return; }
                }, rpc.wizardSettings);
            }

            // move to next step
            layout.next();
        });
    }
});


Ext.define('Ung.Setup', {
    extend: 'Ext.app.Application',
    mainView: 'Ung.Setup.Main',

    launch: function () {
        var cards = [], steps = [], wizard = this.getMainView().down('#wizard');
        Ext.Array.each(rpc.wizardSettings.steps, function (step) {
            cards.push({ xtype: step });
            steps.push({ margin: '0 2' });
        });
        steps.pop(); // remove one element from steps to skip welcome

        // add vtypes
        Ext.apply(Ext.form.field.VTypes, {
            ipAddress: function (val, field) {
                return val.match(this.ipAddressRegex);
            },
            ipAddressText: 'Please enter a valid IP Address',
            ipAddressRegex: /\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b/,

            passwordConfirmCheck: function (val, field) {
                var pass_original = Ext.getCmp(field.comparePasswordField);
                return val === pass_original.getValue();
            },
            passwordConfirmCheckText: 'Passwords do not match'.t()
        });

        wizard.addTool(steps); // add progress steps
        wizard.add(cards); // add cards
        wizard.setActiveItem(0); // trigger the activate event on welcome
    },

    authenticate: function (password, cb) {
        Ung.app.loading('Authenticating'.t());
        Ext.Ajax.request({
            url: '/auth/login?url=/admin&realm=Administrator',
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
                Ung.app.loading(false);
                if (response.responseText && response.responseText.indexOf('loginPage') != -1) {
                    Ext.MessageBox.alert('Authentication failed'.t(), 'Invalid password.'.t());
                    return;
                }
                var setupInfo;
                rpc.jsonrpc = new JSONRpcClient('/admin/JSON-RPC');
                try {
                    setupInfo = rpc.jsonrpc.UvmContext.getSetupStartupInfo();
                } catch (e) {
                    Util.handleException(e);
                    // Ung.Util.handleException(e);
                }
                Ext.applyIf(rpc, setupInfo);

                rpc.tolerateKeepAliveExceptions = false;
                rpc.keepAlive = function() {
                    rpc.jsonrpc.UvmContext.getFullVersion(function (result, exception) {
                        if (!rpc.tolerateKeepAliveExceptions) {
                            if (exception) { Util.handleException(exception); return; }
                            // if (Ung.Util.handleException(exception)) { return; }
                        }
                        Ext.defer(rpc.keepAlive, 300000);
                    });
                };
                rpc.keepAlive();
                cb();
            },
            failure: function (response) {
                Ung.app.loading(false);
                Ext.MessageBox.alert('Authenticatication failed'.t(), 'The authentication request has failed.'.t());
            }
        });
    },

    loading: function (msg) {
        this.getMainView().down('#main').setLoading(msg);
    }
});

