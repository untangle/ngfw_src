var rpc = {}; // the main json rpc object
var testMode = false;

Ext.define("Ung.Inbox", {
    singleton : true,
    viewport : null,
    init : function(config) {
        Ext.apply(this, config);
        Ext.Ajax.request({
            url : 'i18n',
            success : Ext.bind(function(response, options) {
                i18n = Ext.create('Ung.I18N', {
                    map : Ext.decode(response.responseText)
                });
                rpc = new JSONRpcClient("/quarantine/JSON-RPC").Quarantine;
                this.startApplication();
            }, this),
            method : "GET",
            failure : function() {
                Ext.MessageBox.alert("Error", "Unable to load the language pack.");
            },
            params : {
                module : 'untangle'
            }
        });
        this.startApplication();
    },
    releaseOrDelete: function( actionFn, actionStr ) {
        Ext.MessageBox.wait( actionStr , i18n._("Please wait"));
        var mids = [];
        var selections = this.gridQuarantine.getSelectionModel().selected;
        Ext.each(selections, function(item) {
            mids.push(item.data.mailID);
        });

        this.gridQuarantine.getSelectionModel().deselectAll();
        this.gridQuarantine.setDisabled( true );
        actionFn( Ext.bind(this.refreshTable, this ), this.token, mids );
    },

    safelist: function( addresses ) {
        Ext.MessageBox.wait( i18n._("Releasing and adding Senders to Safe List...") , i18n._("Please wait"));
        if(addresses == null) {
            addresses = [];
        }
        var selections = this.gridQuarantine.getSelectionModel().selected;
        Ext.each(selections, function(item) {
            if(item.data.sender != null)
                addresses.push(item.data.sender);
        });
        
        this.gridQuarantine.getSelectionModel().deselectAll();
        this.gridQuarantine.setDisabled( true );
        Ext.Function.defer(function() {
            if (addresses.length == 0){
                Ext.MessageBox.alert(i18n._("An error has occurred."), i18n._("No sender address to be added to safelist."));
                this.gridQuarantine.setDisabled( false );
                return;
            }
            rpc.safelist( Ext.bind(this.refreshTable, this ), inboxDetails.token, addresses );
        }, 1 ,this);
    },
    refreshTable: function( result, exception ) {
        Ext.MessageBox.hide();
        
        if(this.handleException(exception)) return;

        try {
            /* to refresh the buttons at the bottom */
            this.updateButtons(false);

            /* Reload the data */
            var store=this.gridQuarantine.getStore();
            store.getProxy().data=store.refresh();
            
            store.load();

            /* Refresh the table */
            this.gridQuarantine.setDisabled( false );

            /* Refresh the safelist table */
            if ( result.safelist != null ) {
                var sl = result.safelist;
                /* Build a new set of data */
                for ( var c = 0 ; c < sl.length ; c++ ) {
                    sl[c] = [sl[c]];
                }
                safelist.store.loadData( sl );
            }

        } catch ( e ) {
            alert( "Unable to refresh table: " + e );
        }
    },
    updateQuarantineButtons: function(enabled) {
        var releaseButton = this.gridQuarantine.down("button[name=releaseButton]");
        var deleteButton = this.gridQuarantine.down("button[name=releaseButton]");
        var safelistButton = this.gridQuarantine.down("button[name=releaseButton]");
        this.releaseButton.setDisabled( !enabled );
        this.deleteButton.setDisabled( !enabled );
        this.safelistButton.setDisabled( !enabled );
    },
    buildQuarantine : function() {
        var updateQuarantineActionItems = function(selModel, selected, eOpts) {
            var count = selModel.getCount();
            this.updateQuarantineButtons(count>0);
        };
        this.gridQuarantine = Ext.create('Ext.grid.Panel', {
            flex : 1,
            enableColumnHide : false,
            enableColumnMove : false,
            selModel : Ext.create('Ext.selection.CheckboxModel', {
                listeners: {
                    "selectionchange": {
                        fn: updateQuarantineActionItems,
                        scope: this
                    }
                }
            }),
            dockedItems : [ {
                xtype : 'toolbar',
                dock : 'top',
                items : [ {
                    xtype : 'button',
                    name : 'releaseButton',
                    handler : Ext.bind(function() {
                        this.releaseOrDelete(rpc.releaseMessages, i18n._("Releasing..."));
                    }, this),
                    iconCls : 'icon-move-mails',
                    text : i18n._("Release to Inbox"),
                    disabled : true
                }, {
                    xtype : 'button',
                    name : 'safelistButton',
                    handler : Ext.bind(function() {
                        this.safelist();
                    }, this),
                    iconCls : 'icon-safe-list',
                    text : i18n._("Release to Inbox & Add Senders to Safe List"),
                    disabled : true
                }, {
                    xtype : 'button',
                    name : 'deleteButton',
                    handler : Ext.bind(function() {
                        this.releaseOrDelete(rpc.purgeMessages, i18n._("Deleting..."));
                    }, this),
                    iconCls : 'icon-delete-row',
                    text : i18n._("Delete"),
                    disabled : true
                } ]
            } ],
            store : Ext.create('Ext.data.Store', {
                sortOnLoad : true,
                sorters : {
                    property : 'internDate',
                    direction : 'DESC'
                },
                fields : [ {
                    name : 'recipients'
                }, {
                    name : 'mailID'
                }, {
                    name : 'internDate'
                }, {
                    name : 'size'
                }, {
                    name : 'attachmentCount'
                }, {
                    name : 'truncatedSender'
                }, {
                    name : 'sender',
                    sortType : Ext.data.SortTypes.asUCString
                }, {
                    name : 'truncatedSubject'
                }, {
                    name : 'subject'
                }, {
                    name : 'quarantineCategory'
                }, {
                    name : 'quarantineDetail',
                    sortType : 'asFloat'
                }, {
                    name : 'quarantineSize'
                } ]
            }),
            columns : [ {
                header : i18n._("From"),
                dataIndex : 'sender',
                width : 250,
                filter : {
                    type : 'string'
                }
            }, {
                header : "<div class='quarantine-attachment-header'>&nbsp</div>",
                dataIndex : 'attachmentCount',
                width : 60,
                tooltip : i18n._("Number of Attachments in the email."),
                align : 'center',
                filter : {
                    type : 'numeric'
                }
            }, {
                header : i18n._("Score"),
                dataIndex : 'quarantineDetail',
                width : 60,
                align : 'center',
                filter : {
                    type : 'numeric'
                }
            }, {
                header : i18n._("Subject"),
                dataIndex : 'truncatedSubject',
                flex : 1,
                width : 250,
                filter : {
                    type : 'string'
                }
            }, {
                header : i18n._("Date"),
                dataIndex : 'internDate',
                width : 135,
                renderer : function(value) {
                    var date = new Date();
                    date.setTime(value);
                    d = Ext.util.Format.date(date, 'm/d/Y');
                    t = Ext.util.Format.date(date, 'g:i a');
                    return d + ' ' + t;
                },
                filter : {
                    type : 'datetime',
                    dataIndex : 'internDate',
                    date : {
                        format : 'm/d/Y'
                    },
                    time : {
                        format : 'g:i a',
                        increment : 1
                    },
                    validateRecord : function(record) {
                        var me = this, key, pickerValue, val1 = record.get(me.dataIndex);

                        var val = new Date(val1.time);
                        if (!Ext.isDate(val)) {
                            return false;
                        }
                        val = val.getTime();

                        for (key in me.fields) {
                            if (me.fields[key].checked) {
                                pickerValue = me.getFieldValue(key).getTime();
                                if (key == 'before' && pickerValue <= val) {
                                    return false;
                                }
                                if (key == 'after' && pickerValue >= val) {
                                    return false;
                                }
                                if (key == 'on' && pickerValue != val) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                }
            }, {
                header : i18n._("Size (KB)"),
                dataIndex : 'size',
                renderer : function(value) {
                    return Math.round(((value + 0.0) / 1024) * 10) / 10;
                },
                width : 60,
                filter : {
                    type : 'numeric'
                }
            } ]
        });

        this.panelQuarantine = {
            xtype : 'panel',
            title : i18n._("Quarantined Messages"),
            layout : {
                type : 'vbox',
                align : 'stretch'
            },
            items : [ {
                xtype : 'component',
                flex : 0,
                html : Ext.String.format(i18n._("The messages below were quarantined and will be deleted after {0} days."), this.quarantineDays),
                border : true,
                margin : 5

            }, this.gridQuarantine ]
        };
    },
    getAddToSafelistWindow : function() {
        if ( this.addToSafelistWindow == null ) {
            this.addToSafelistWindow = Ext.create('Ext.window.Window', {
                width:500,
                height:250,
                title: i18n._('Add an Email Address to Safelist'),
                closeAction:'hide',
                modal: true,
                layout: 'fit',
                items: {
                    xtype: 'panel',
                    bodyPadding: 10,
                    items: [{
                        xtype: 'textfield',
                        width: 420,
                        fieldLabel : i18n._( "Email Address" ),
                        name : "email_address"
                    }]
                },
                buttons: [{
                    text : i18n._( 'Save' ),
                    handler: function() {
                        var field = this.addToSafelistWindow.down('textfield[name="email_address"]');
                        var email = field.getValue();
                        field.setValue( "" );
                        this.safelist( [ email ] );
                        this.addToSafelistWindow.hide();
                    },
                    scope : this
                },{
                    text : i18n._( 'Cancel' ),
                    handler: function() {
                        this.addToSafelistWindow.hide();
                    },
                    scope : this
                }]
            } );
        }

        return this.addToSafelistWindow;
    },
    buildSafeList : function() {
        var updateActionItems = function(selModel, selected, eOpts) {
            var count = selModel.getCount();
            var deleteButton = this.gridSafeList.down("button[name=deleteButton]");
            var text = i18n.pluralise( i18n._( "Delete one Address" ), Ext.String.format( i18n._( "Delete {0} Addresses" ), count ), count );
            if ( count > 0 ) {
                deleteButton.setDisabled( false );
            } else {
                deleteButton.setDisabled( true );
                text = i18n._( "Delete Addresses" );
            }
            deleteButton.setText( text );
        };
        var addButtonHandler = function() {
            var window = this.getAddToSafelistWindow();
            window.show();
        };
        var deleteButtonHandler = function(button) {
            var addresses = [];
            var selectedRecords = this.selectionModel.getSelection();
            for ( var i = 0; i < selectedRecords.length; i++) {
                addresses.push(selectedRecords[i].data.emailAddress);
            }
            this.gridSafeList.setDisabled(true);
            this.selectionModel.deselectAll();
            button.setText(i18n._("Delete Addresses"));
            button.setDisabled(true);

            rpc.deleteAddressesFromSafelist(Ext.bind(this.deleteAddresses, this), inboxDetails.token, addresses);
        };
        this.gridSafeList = Ext.create('Ext.grid.Panel', {
            flex : 1,
            enableColumnHide : false,
            enableColumnMove : false,
            selModel : Ext.create('Ext.selection.CheckboxModel', {
                listeners: {
                    "selectionchange": {
                        fn: updateActionItems,
                        scope: this
                    }
                }
            }),
            tbar : [{
                xtype : 'button',
                name : 'addButton',
                iconCls : 'icon-add-row',
                text : i18n._("Add"),
                handler : addButtonHandler,
                scope : this
            }, {
                text : i18n._("Delete Addresses"),
                name : 'deleteButton',
                disabled : true,
                iconCls : 'icon-delete-row',
                handler : deleteButtonHandler,
                scope : this
            } ],
            store : Ext.create('Ext.data.ArrayStore', {
                fields : [ {
                    name : 'emailAddress'
                } ],
                data : this.safelistData
            }),
            columns : [ {
                header : i18n._("Email Address"),
                dataIndex : 'emailAddress',
                flex : 1,
                menuDisabled : true,
                field : {
                    xtype : 'textfield'
                }
            } ]
        });
        this.panelSafeList = {
            xtype : 'panel',
            title : i18n._("Safe List"),
            layout : {
                type : 'vbox',
                align : 'stretch'
            },
            items : [ {
                xtype : 'component',
                flex : 0,
                html : i18n._("You can use the Safe List to make sure that messages from these senders are never quarantined."),
                border : true,
                margin : 5
            }, this.gridSafeList ]
        };
    },
    buildRemaps : function() {
        this.panelRemaps = {
            xtype : 'panel',
            title : i18n._("Forward or Receive Quarantines"),
            items : [{
                xtype: 'component',
                html: 'ToDo'
            }]
        };
    },
    startApplication : function() {
        document.title = Ext.String.format(i18n._("{0} | Quarantine Digest for: {1}"), this.companyName, this.currentAddress);
        this.buildQuarantine();
        this.buildSafelist();
        this.buildRemaps();

        this.viewport = Ext.create('Ext.container.Viewport', {
            layout : 'border',
            items : [ {
                region : 'north',
                padding : 5,
                height : 70,
                xtype : 'container',
                layout : {
                    type : 'hbox',
                    align : 'stretch'
                },
                items : [ {
                    xtype : 'container',
                    html : '<img src="/images/BrandingLogo.png?' + (new Date()).getTime() + '" border="0" height="60"/>',
                    width : 100,
                    flex : 0
                }, {
                    xtype : 'component',
                    padding : '27 10 0 10',
                    style : 'text-align:right; font-family: sans-serif; font-weight:bold;font-size:18px;',
                    flex : 1,
                    html : Ext.String.format(i18n._("Quarantine Digest for: {0}"), this.currentAddress)
                } ]
            }, {
                xtype : 'tabpanel',
                region : 'center',
                activeTab : 0,
                deferredRender : false,
                border : false,
                plain : true,
                flex : 1,
                items : [ this.panelQuarantine, this.panelSafeList, this.panelRemaps ]
            } ]
        });
    }
});