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
    },
    handleException : function(exception) {
        if (exception) {
            if (console) {
                console.error("handleException:", exception);
            }
            if (exception.message == null) {
                exception.message = "";
            }
            var message = null;

            // handle connection lost
            if (exception.code == 550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
            // handle connection lost (this happens on windows only for some
            // reason)
            (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
            // special text for "method not found" and "Service Temporarily
            // Unavailable"
            exception.message.indexOf("method not found") != -1 || exception.message.indexOf("Service Unavailable") != -1 || exception.message.indexOf("Service Temporarily Unavailable") != -1 || exception.message.indexOf("This application is not currently available") != -1) {
                message = i18n._("The connection to the server has been lost.") + "<br/>";

            }
            // worst case - just say something
            if (message == null) {
                if (exception && exception.message) {
                    message = i18n._("An error has occurred") + ":" + "<br/>" + exception.message;
                } else {
                    message = i18n._("An error has occurred.");
                }
            }

            var details = "";
            if (exception.javaStack)
                // override poor jsonrpc.js naming
                exception.name = exception.javaStack.split('\n')[0];
            if (exception.name)
                details += "<b>" + i18n._("Exception name") + ":</b> " + exception.name + "<br/><br/>";
            if (exception.code)
                details += "<b>" + i18n._("Exception code") + ":</b> " + exception.code + "<br/><br/>";
            if (exception.message)
                details += "<b>" + i18n._("Exception message") + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
            if (exception.javaStack)
                details += "<b>" + i18n._("Exception java stack") + ":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
            if (exception.stack)
                details += "<b>" + i18n._("Exception js stack") + ":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
            details += "<b>" + i18n._("Timestamp") + ":&nbsp;</b>" + (new Date()).toString() + "<br/>";
            Ext.MessageBox.alert(message, details);
            return true;
        }
        return false;
    },

    showMessage : function(message) {
        this.messageCount++;

        this.messageDisplayTip.add({
            xtype : 'component',
            html : message + "<br/>",
            padding : 2,
            style : {
                fontSize : '13px'
            }
        });
        this.messageDisplayTip.show();

        setTimeout(Ext.bind(this.hideMessageTip, this), 5000);
    },
    hideMessageTip : function() {
        this.messageDisplayTip.remove(0);
        this.messageCount--;

        if (this.messageCount <= 0) {
            this.messageDisplayTip.hide();
            this.messageCount = 0;
        } else {
            // This updates the shadow
            this.messageDisplayTip.show();
        }
    },

    releaseOrDelete : function(actionFn, actionStr) {
        Ext.MessageBox.wait(actionStr, i18n._("Please wait"));
        var mids = [];
        var selections = this.gridQuarantine.getSelectionModel().getSelection();
        Ext.each(selections, function(item) {
            mids.push(item.data.mailID);
        });
        this.gridQuarantine.getSelectionModel().deselectAll();
        this.updateQuarantineButtons(false);
        actionFn(Ext.bind(this.refreshGridSafelist, this), this.token, mids);
    },

    releaseAndSafelist : function(addresses) {
        Ext.MessageBox.wait(i18n._("Releasing and adding Senders to Safe List..."), i18n._("Please wait"));
        if (addresses == null) {
            addresses = [];
        }
        var selections = this.gridQuarantine.getSelectionModel().getSelection();
        Ext.each(selections, function(item) {
            if (item.data.sender != null)
                addresses.push(item.data.sender);
        });

        this.gridQuarantine.getSelectionModel().deselectAll();
        this.updateQuarantineButtons(false);
        Ext.Function.defer(function() {
            if (addresses.length == 0) {
                Ext.MessageBox.alert(i18n._("An error has occurred."), i18n._("No sender address to be added to safelist."));
                return;
            }
            this.gridSafelist.getView().setLoading(true);
            rpc.safelist(Ext.bind(this.refreshGridSafelist, this), this.token, addresses);
        }, 1, this);
    },
    refreshGridSafelist : function(result, exception) {
        Ext.MessageBox.hide();
        if (this.handleException(exception)) {
            this.gridSafelist.getView().setLoading(false);
            return;
        }
        this.refreshGridQuarantie();
        var messages = [];
        if (result.purgeCount > 0) {
            messages.push(i18n.pluralise(i18n._("Deleted one Message"), Ext.String.format(i18n._("Deleted {0} Messages"), result.purgeCount), result.purgeCount));
        }

        if (result.releaseCount > 0) {
            messages.push(i18n.pluralise(i18n._("Released one Message"), Ext.String.format(i18n._("Released {0} Messages"), result.releaseCount), result.releaseCount));
        }

        if (result.safelistCount > 0) {
            messages.push(i18n.pluralise(i18n._("Safelisted one Address"), Ext.String.format(i18n._("Safelisted {0} Addresses"), result.safelistCount), result.safelistCount));
        }
        if (messages.length > 0) {
            this.showMessage(messages.join("<br/>"));
        }

        // Refresh the safelist table
        if (result.safelist) {
            var sl = result.safelist;
            // Build a new set of data
            for ( var c = 0; c < sl.length; c++) {
                sl[c] = [ sl[c] ];
            }
            this.gridSafelist.getStore().loadData(sl);
        }
        this.gridSafelist.getView().setLoading(false);
    },
    updateQuarantineButtons : function(enabled) {
        var releaseButton = this.gridQuarantine.down("button[name=releaseButton]");
        var deleteButton = this.gridQuarantine.down("button[name=deleteButton]");
        var safelistButton = this.gridQuarantine.down("button[name=safelistButton]");
        releaseButton.setDisabled(!enabled);
        deleteButton.setDisabled(!enabled);
        safelistButton.setDisabled(!enabled);
    },
    refreshGridQuarantie : function() {
        this.gridQuarantine.getView().setLoading(true);
        rpc.getInboxRecords(Ext.bind(function(result, exception) {
            if (exception) {
                var message = exception.message;
                if (exception.name == "com.untangle.app.smtp.quarantine.NoSuchInboxException") {
                    message = Ext.String.format(i18n._("The account {0} doesn't have any quarantined messages."), this.address);
                }
                if (message == null || message == "Unknown") {
                    message = i18n._("Please Try Again");
                }
                Ext.MessageBox.alert("Failed", message);
                this.gridQuarantine.getStore().getProxy().setData([]);
                this.gridQuarantine.getStore().load({
                    callback : function() {
                        this.gridQuarantine.getView().setLoading(false);
                    },
                    scope : this
                });
                return;
            }

            var mails = [], mail, i;
            if (result) {
                for (i = 0; i < result.list.length; i++) {
                    result.list[i].time = result.list[i].internDate; // preserve time in a different prop
                    mail = result.list[i];
                    Ext.apply(mail, result.list[i].mailSummary);
                    mails.push(mail);
                }
            }
            if (testMode) {
                var getTestRecord = function(index) {
                    return {
                        recipients : 'recipients' + index,
                        sender : "sender" + (index % 10) + "@test.com",
                        mailID : 'mailID' + index,
                        internDate : 10000 * index,
                        size : 500 * index,
                        attachmentCount : 1000 - index,
                        quarantineDetail : parseFloat(index) / 100,
                        truncatedSubject : "subject spam" + index
                    };
                };
                var length = Math.floor((Math.random() * 5000));
                for (i = parseInt(length / 3, 10); i < length; i++) {
                    mails.push(getTestRecord(i));
                }
            }
            this.gridQuarantine.getStore().getProxy().setData(mails);
            this.gridQuarantine.getStore().load({
                callback : function() {
                    this.gridQuarantine.getView().setLoading(false);
                },
                scope : this
            });
        }, this), this.token);
    },
    buildQuarantine : function() {
        this.filterFeature = Ext.create('Ung.grid.feature.GlobalFilter', {});
        var updateQuarantineActionItems = function(selModel, selected, eOpts) {
            var count = selModel.getCount();
            this.updateQuarantineButtons(count > 0);
        };
        this.gridQuarantine = Ext.create('Ext.grid.Panel', {
            flex : 1,
            enableColumnHide : false,
            enableColumnMove : false,
            plugins : [ 'gridfilters' ],
            features : [ this.filterFeature ],
            selModel : Ext.create('Ext.selection.CheckboxModel', {
                listeners : {
                    "selectionchange" : {
                        fn : updateQuarantineActionItems,
                        scope : this
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
                        this.releaseAndSafelist();
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
                    iconCls : 'icon-delete',
                    text : i18n._("Delete"),
                    disabled : true
                } ]
            }, {
                xtype : 'toolbar',
                dock : 'bottom',
                items : [ i18n._('Filter:'), {
                    xtype : 'textfield',
                    name : 'searchField',
                    hideLabel : true,
                    width : 130,
                    listeners : {
                        change : {
                            fn : function() {
                                this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                            },
                            scope : this,
                            buffer : 600
                        }
                    }
                }, {
                    xtype : 'checkbox',
                    name : 'caseSensitive',
                    hideLabel : true,
                    margin : '0 4px 0 4px',
                    boxLabel : i18n._('Case sensitive'),
                    handler : function() {
                        this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                    },
                    scope : this
                }, {
                    xtype : 'button',
                    iconCls : 'icon-clear-filter',
                    text : i18n._('Clear Filters'),
                    tooltip : i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                    tooltipType : 'title',
                    handler : Ext.bind(function() {
                        this.gridQuarantine.clearFilters();
                        this.searchField.setValue("");
                    }, this)
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
                    name : 'internDate',
                    convert: function(value) {
                        var date = new Date();
                        date.setTime(value);
                        d = Ext.util.Format.date(date, 'm/d/Y');
                        t = Ext.util.Format.date(date, 'g:i a');
                        return d + ' ' + t;
                    }
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
                header : i18n._("Attachments"),
                dataIndex : 'attachmentCount',
                width : 90,
                tooltip : i18n._("Number of Attachments in the email."),
                tooltipType : 'title',
                align : 'center',
                renderer : function(value) {
                    return value != 0 ? value : "";
                },
                filter : {
                    type : 'numeric'
                }
            }, {
                header : i18n._("Score"),
                dataIndex : 'quarantineDetail',
                width : 65,
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
                width : 140,
                filter : {
                    type : 'string'
                },
                sorter: function (rec1, rec2) {
                    var t1 = rec1.getData().time, t2 = rec2.getData().time;
                    return (t1 > t2) ? 1 : (t1 === t2) ? 0 : -1;
                }
            }, {
                header : i18n._("Size (KB)"),
                dataIndex : 'quarantineSize',
                renderer : function(value) {
                    return Math.round(((value + 0.0) / 1024) * 10) / 10;
                },
                width : 70,
                filter : {
                    type : 'numeric'
                }
            } ]
        });

        this.panelQuarantine = {
            xtype : 'panel',
            bodyPadding : 10,
            title : i18n._("Quarantined Messages"),
            layout : {
                type : 'vbox',
                align : 'stretch'
            },
            items : [ {
                xtype : 'component',
                cls: 'warning-message',
                flex : 0,
                html : Ext.String.format(i18n._("The messages below were quarantined and will be deleted after {0} days."), this.quarantineDays),
                border : true,
                margin : '0 0 10 0'

            }, this.gridQuarantine ]
        };

        this.searchField = this.gridQuarantine.down('textfield[name=searchField]');
        this.caseSensitive = this.gridQuarantine.down('checkbox[name=caseSensitive]');
    },
    getAddToSafelistWindow : function() {
        if (this.addToSafelistWindow == null) {
            this.addToSafelistWindow = Ext.create('Ext.window.Window', {
                width : 500,
                height : 200,
                title : i18n._('Add an Email Address to Safelist'),
                closeAction : 'hide',
                modal : true,
                layout : 'fit',
                items : {
                    xtype : 'panel',
                    bodyPadding : 10,
                    items : [ {
                        xtype : 'textfield',
                        width : 420,
                        fieldLabel : i18n._("Email Address"),
                        name : "emailAddress"
                    } ]
                },
                buttons : [ {
                    text : i18n._('Save'),
                    handler : function() {
                        var field = this.addToSafelistWindow.down('textfield[name="emailAddress"]');
                        var email = field.getValue();
                        field.setValue("");
                        this.releaseAndSafelist([ email ]);
                        this.addToSafelistWindow.hide();
                    },
                    scope : this
                }, {
                    text : i18n._('Cancel'),
                    handler : function() {
                        this.addToSafelistWindow.hide();
                    },
                    scope : this
                } ]
            });
        }

        return this.addToSafelistWindow;
    },
    buildSafelist : function() {
        var updateActionItems = function(selModel, selected, eOpts) {
            var count = selModel.getCount();
            var deleteButton = this.gridSafelist.down("button[name=deleteButton]");
            var text = i18n.pluralise(i18n._("Delete one Address"), Ext.String.format(i18n._("Delete {0} Addresses"), count), count);
            if (count > 0) {
                deleteButton.setDisabled(false);
            } else {
                deleteButton.setDisabled(true);
                text = i18n._("Delete Addresses");
            }
            deleteButton.setText(text);
        };
        var addButtonHandler = function() {
            var window = this.getAddToSafelistWindow();
            window.show();
        };
        var deleteButtonHandler = function(button) {
            var addresses = [];
            var selections = this.gridSafelist.getSelectionModel().getSelection();
            for ( var i = 0; i < selections.length; i++) {
                addresses.push(selections[i].get("emailAddress"));
            }
            this.gridSafelist.getSelectionModel().deselectAll();
            button.setText(i18n._("Delete Addresses"));
            button.setDisabled(true);

            rpc.deleteAddressesFromSafelist(Ext.bind(function(result, exception, foo) {
                var message;
                if (this.handleException(exception))
                    return;

                var count = result.safelistCount;
                count = -count;

                message = i18n.pluralise(i18n._("Deleted one address"), Ext.String.format(i18n._("Deleted {0} addresses"), count), count);
                this.showMessage(message);

                var sl = result.safelist;
                // Build a new set of data
                for ( var c = 0; c < sl.length; c++) {
                    sl[c] = [ sl[c] ];
                }
                this.gridSafelist.getStore().loadData(sl);
            }, this), this.token, addresses);
        };
        this.gridSafelist = Ext.create('Ext.grid.Panel', {
            flex : 1,
            enableColumnHide : false,
            enableColumnMove : false,
            selModel : Ext.create('Ext.selection.CheckboxModel', {
                listeners : {
                    "selectionchange" : {
                        fn : updateActionItems,
                        scope : this
                    }
                }
            }),
            tbar : [ {
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
                iconCls : 'icon-delete',
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
        this.panelSafelist = {
            xtype : 'panel',
            bodyPadding : 10,
            title : i18n._("Safe List"),
            layout : {
                type : 'vbox',
                align : 'stretch'
            },
            items : [ {
                xtype : 'component',
                flex : 0,
                cls: 'warning-message',
                html : i18n._("You can use the Safe List to make sure that messages from these senders are never quarantined."),
                border : true,
                margin : '0 0 10 0'
            }, this.gridSafelist ]
        };
    },
    buildRemaps : function() {
        var changeForwardAdress = function(button) {
            var email = this.panelRemaps.down("textfield[name=emailAddress]").getValue();
            if (email != "") {
                rpc.setRemap(Ext.bind(function(result, exception) {
                    if (this.handleException(exception))
                        return;
                    var message = i18n._("Updated forward address");
                    this.showMessage(message);
                    this.forwardAddress = email;
                }, this), this.token, email);
            }
        };

        var deleteForwardAdress = function(button) {
            if (this.forwardAddress != "") {
                var email = this.panelRemaps.down("textfield[name=emailAddress]").getValue();
                rpc.deleteRemap(Ext.bind(function(result, exception) {
                    if (this.handleException(exception))
                        return;
                    this.forwardAddress = "";
                    this.panelRemaps.down("textfield[name=emailAddress]").setValue("");
                    var message = i18n._("Deleted forward address");
                    this.showMessage(message);
                }, this), this.token, this.forwardAddress);

            }
        };
        var deleteButtonHandler = function(button) {
            var grid = button.up("grid");
            var addresses = [];
            var selections = grid.getSelectionModel().getSelection();
            for ( var i = 0; i < selections.length; i++) {
                addresses.push(selections[i].get("emailAddress"));
            }
            grid.getSelectionModel().deselectAll();
            button.setText(i18n._("Delete Addresses"));
            button.setDisabled(true);
            var count = addresses.length;
            rpc.deleteRemaps(Ext.bind(function(result, exception) {
                if (this.handleException(exception))
                    return;

                message = i18n.pluralise(i18n._("Deleted one Remap"), Ext.String.format(i18n._("Deleted {0} Remaps"), count), count);
                this.showMessage(message);
                // Build a new set of data
                for ( var c = 0; c < result.length; c++) {
                    result[c] = [ result[c] ];
                }
                grid.getStore().loadData(result);
            }, this), this.token, addresses);
        };

        var updateActionItems = function(selModel, selected, eOpts) {
            var count = selModel.getCount();
            var deleteButton = this.gridRemaps.down("button[name=deleteButton]");
            var text = i18n.pluralise(i18n._("Delete one Address"), Ext.String.format(i18n._("Delete {0} Addresses"), count), count);
            if (count > 0) {
                deleteButton.setDisabled(false);
            } else {
                deleteButton.setDisabled(true);
                text = i18n._("Delete Addresses");
            }
            deleteButton.setText(text);
        };

        this.gridRemaps = Ext.create('Ext.grid.Panel', {
            enableColumnHide : false,
            enableColumnMove : false,
            flex : 1,
            store : Ext.create('Ext.data.ArrayStore', {
                fields : [ {
                    name : 'emailAddress'
                } ],
                data : this.remapsData
            }),
            columns : [ {
                header : i18n._("Email Address"),
                dataIndex : 'emailAddress',
                flex : 1,
                menuDisabled : true
            } ],
            clicksToEdit : 1,
            height : 200,
            width : 400,
            selModel : Ext.create('Ext.selection.CheckboxModel', {
                listeners : {
                    "selectionchange" : {
                        fn : updateActionItems,
                        scope : this
                    }
                }
            }),
            tbar : [ {
                text : i18n._("Delete Addresses"),
                name : 'deleteButton',
                disabled : true,
                iconCls : 'icon-delete',
                handler : deleteButtonHandler,
                scope : this
            } ]
        });

        this.panelRemaps = Ext.create({
            xtype : 'panel',
            bodyPadding : 10,
            title : i18n._("Forward or Receive Quarantines"),
            layout : {
                type : 'vbox',
                align : 'stretch'
            },
            items : [ {
                xtype : 'container',
                flex : 0,
                items : [ {
                    xtype : 'textfield',
                    name : "emailAddress",
                    fieldLabel : i18n._("Forward Quarantined Messages To"),
                    labelAlign : 'top',
                    width : 300,
                    flex : 0,
                    value : this.forwardAddress
                } ]
            }, {
                xtype : 'container',
                flex : 0,
                layout : {
                    type : 'hbox',
                    align : 'left'
                },
                items : [ {
                    xtype : 'button',
                    name : 'changeAddress',
                    text : i18n._("Set forward address"),
                    handler : changeForwardAdress,
                    scope : this
                }, {
                    xtype : 'button',
                    name : 'changeAddress',
                    iconCls : 'icon-delete',
                    margin : '0 0 0 20',
                    text : i18n._("Delete forward address"),
                    handler : deleteForwardAdress,
                    scope : this
                } ]
            }, {
                xtype : 'component',
                html : i18n._("Received Quarantined Messages From:"),
                margin : '30 0 10 0',
                flex : 0
            }, this.gridRemaps ]
        });
    },
    startApplication : function() {
        document.title = Ext.String.format(i18n._("{0} | Quarantine Digest for: {1}"), this.companyName, this.currentAddress);
        this.messageDisplayTip = Ext.create('Ext.tip.Tip', {
            target : Ext.getBody(),
            defaultAlign : 'c-c?'
        });
        this.messageCount = 0;
        this.hideTimeout = null;

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
                    html : '<img src="/images/BrandingLogo.png" border="0" height="60"/>',
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
                items : [ this.panelQuarantine, this.panelSafelist, this.panelRemaps ]
            } ]
        });

        this.refreshGridQuarantie();
    }
});

Ext.define("Ung.grid.feature.GlobalFilter", {
    extend : "Ext.grid.feature.Feature",
    useVisibleColumns : true,
    useFields : null,
    init : function(grid) {
        this.grid = grid;

        this.globalFilter = Ext.create('Ext.util.Filter', {
            regExpProtect : /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
            disabled : true,
            regExpMode : false,
            caseSensitive : false,
            regExp : null,
            stateId : 'globalFilter',
            searchFields : {},
            filterFn : function(record) {
                if (!this.regExp) {
                    return true;
                }
                var datas = record.getData(), key, val;
                for (key in this.searchFields) {
                    if (datas[key] !== undefined) {
                        val = datas[key];
                        if (val == null) {
                            continue;
                        }
                        if (typeof val == 'boolean' || typeof val == 'number') {
                            val = val.toString();
                        } else if (typeof val == 'object') {
                            if (val.time != null) {
                                val = i18n.timestampFormat(val);
                            }
                        }
                        if (typeof val == 'string') {
                            if (this.regExp.test(val)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            },
            getSearchValue : function(value) {
                if (value === '' || value === '^' || value === '$') {
                    return null;
                }
                if (!this.regExpMode) {
                    value = value.replace(this.regExpProtect, function(m) {
                        return '\\' + m;
                    });
                } else {
                    try {
                        new RegExp(value);
                    } catch (error) {
                        return null;
                    }
                }
                return value;
            },
            buildSearch : function(value, caseSensitive, searchFields) {
                this.searchFields = searchFields;
                this.setCaseSensitive(caseSensitive);
                var searchValue = this.getSearchValue(value);
                this.regExp = searchValue == null ? null : new RegExp(searchValue, 'g' + (caseSensitive ? '' : 'i'));
                this.setDisabled(this.regExp == null);
            }
        });

        this.grid.on("afterrender", Ext.bind(function() {
            this.grid.getStore().addFilter(this.globalFilter);
        }, this));
        this.grid.on("beforedestroy", Ext.bind(function() {
            this.grid.getStore().removeFilter(this.globalFilter);
            Ext.destroy(this.globalFilter);
        }, this));
        this.callParent(arguments);
    },
    updateGlobalFilter : function(value, caseSensitive) {
        var searchFields = {}, i, col;
        if (this.useVisibleColumns) {
            var visibleColumns = this.grid.getVisibleColumns();
            for (i = 0; i < visibleColumns.length; i++) {
                col = visibleColumns[i];
                if (col.dataIndex) {
                    searchFields[col.dataIndex] = true;
                }
            }
        } else if (this.searchFields != null) {
            for (i = 0; i < this.searchFields.length; i++) {
                searchFields[this.searchFields[i]] = true;
            }
        }
        this.globalFilter.buildSearch(value, caseSensitive, searchFields);
        this.grid.getStore().getFilters().notify('endupdate');
    }
});