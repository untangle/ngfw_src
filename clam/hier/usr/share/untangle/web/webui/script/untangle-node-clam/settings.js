if (!Ung.hasResource["Ung.Clam"]) {
    Ung.hasResource["Ung.Clam"] = true;
    Ung.Settings.registerClassName('untangle-node-clam', 'Ung.Clam');

    Ung.Clam = Ext.extend(Ung.Settings, {
        gridExceptions : null,
        gridEventLog : null,
        // called when the component is rendered
        onRender : function(container, position) {
            // call superclass renderer first
            Ung.Clam.superclass.onRender.call(this, container, position);
            // builds the 4 tabs
//            this.buildWeb();
//            this.buildEmail();
//            this.buildFtp();
            this.buildEventLog();
            // builds the tab panel with the tabs
            this.buildTabPanel([/*this.panelWeb, this.panelEmail, this.panelFtp,*/ this.gridEventLog]);
        },
        // Event Log
        buildEventLog : function() {
            this.gridEventLog = new Ung.GridEventLog({
                settingsCmp : this,
                //autoExpandColumn: 'timeStamp',

                // the list of fields
                fields : [{
                    name : 'timeStamp'
                }, {
                    name : 'type'
                }, {
                    name : 'actionType'
                }, {
                    name : 'pipelineEndpoints'
                }, {
                    name : 'traffic'
                }, {
                    name : 'infected'
                }],
                // the list of columns
                columns : [{
                    header : this.i18n._("timestamp"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'timeStamp',
                    renderer : function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header : this.i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'actionType',
                    renderer : function(value, metadata, record ) {
						switch (record.data.type) {
							case 'HTTP' :
							case 'FTP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("clean");
									case 1 : // CLEANED
										return this.i18n._("cleaned");
									default :
									case 2 : // BLOCKED
										return this.i18n._("blocked");
								}
								break;
							case 'POP/IMAP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("pass message");
									default :
									case 1 : // CLEANED
										return this.i18n._("remove infection");
								}
								break;
							case 'SMTP' :
								switch (value) {
									case 0 : // PASSED
										return this.i18n._("pass message");
									case 1 : // CLEANED
										return this.i18n._("remove infection");
									default :
									case 2 : // BLOCKED
										return this.i18n._("block message");
								}
                                break;
						}
						return "";
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("client"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.CClientAddr.hostAddress + ":" + value.CClientPort;
                    }
                }, {
                    header : this.i18n._("traffic"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'traffic'
                }, {
                    header : this.i18n._("reason for") + "<br>" + this.i18n._("action"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'infected',
                    renderer : function(value) {
                        return value ? this.i18n._("virus found") : this.i18n._("no virus found");
                    }.createDelegate(this)
                }, {
                    header : this.i18n._("server"),
                    width : 120,
                    sortable : true,
                    dataIndex : 'pipelineEndpoints',
                    renderer : function(value) {
                        return value === null ? "" : value.SServerAddr.hostAddress + ":" + value.SServerPort;
                    }
                }]
            });
        },
        // save function
        save : function() {
//            if (this.validate()) {
//                // disable tabs during save
//                this.tabs.disable();
//                this.getRpcNode().updateAll(function(result, exception) {
//                    // re-enable tabs
//                    this.tabs.enable();
//                    if (exception) {
//                        Ext.MessageBox.alert(i18n._("Failed"), exception.message);
//                        return;
//                    }
//                    // exit settings screen
//                    this.cancelAction();
//                }.createDelegate(this), this.getBaseSettings(), this.gridPassedClients ? this.gridPassedClients.getSaveList() : null,
//                        this.gridPassedUrls ? this.gridPassedUrls.getSaveList() : null,
//                        this.gridBlockedUrls ? this.gridBlockedUrls.getSaveList() : null,
//                        this.gridBlockedMimeTypes ? this.gridBlockedMimeTypes.getSaveList() : null,
//                        this.gridBlockedExtensions ? this.gridBlockedExtensions.getSaveList() : null,
//                        this.gridBlacklistCategories ? this.gridBlacklistCategories.getSaveList() : null);
//            }
        }
    });
}