Ext.define('Webui.untangle-node-web-cache.settings', {
    extend: 'Ung.NodeWin',
    gridProtocolList: null,
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._("Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.");
    },
    initComponent: function() {
        this.statistics = this.getRpcNode().getStatistics();
        this.buildGridRules();

        // builds the tab panel with the tabs
        this.buildTabPanel([this.gridRules]);
        this.callParent(arguments);
    },
    statFormat: function(input) {
         var s = input.toString(), l = s.length, o = '';
         while (l > 3) {
             var c = s.substr(l - 3, 3);
             o = ',' + c + o;
             s = s.replace(c, '');
             l -= 3;
         }
         o = s + o;
         return o;
    },
    // Status Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status',{
            settingsCmp: this,
            helpSource: 'web_cache_status',
            itemsAfterLicense: [{
                title: i18n._('Statistics'),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 230
                },
                items: [{
                    fieldLabel: i18n._('Cache Hit Count'),
                    name: 'hitCount',
                    value: this.statFormat(this.statistics.hitCount)
                },{
                    fieldLabel: i18n._('Cache Miss Count'),
                    name: 'missCount',
                    value: this.statFormat(this.statistics.missCount)
                },{
                    fieldLabel: i18n._('Cache Hit Bytes'),
                    name: 'hitBytes',
                    value: this.statFormat(this.statistics.hitBytes)
                },{
                    fieldLabel: i18n._('Cache Miss Bytes'),
                    name: 'missBytes',
                    value: this.statFormat(this.statistics.missBytes)
                },{
                    fieldLabel: i18n._('User Bypass Count'),
                    name: 'bypassCount',
                    value: this.statFormat(this.statistics.bypassCount)
                },{
                    fieldLabel: i18n._('System Bypass Count'),
                    name: 'systemCount',
                    value: this.statFormat(this.statistics.systemCount)
                }]
            },{
                title: i18n._('Clear Cache'),
                items: [{
                    xtype: 'component',
                    html: i18n._('If content stored in the cache somehow becomes stale or corrupt, the cache can be cleared with the') + " <b>" +
                          i18n._('Clear Cache') + "</b> " + i18n._("button.") + "<br><br>" + "<b>" +
                          i18n._("Caution") + ":  </b>" + i18n._("Clearing the cache requires restarting the caching engine.") + " " +
                          i18n._("This will cause active web sessions to be dropped and may disrupt web traffic for several seconds.")
                }, {
                    xtype: 'container',
                    layout: {type: 'column'},
                    margin: '10 0 0 0',
                    items: [{
                        xtype: 'checkbox',
                        boxLabel: i18n._('I understand the risks.'),
                        name: 'understandRisks',
                        hideLabel: true,
                        checked: false,
                        margin: '0 40 0 0'
                    }, {
                        xtype: 'button',
                        text: i18n._('Clear Cache'),
                        name: 'Clear Cache',
                        iconCls: 'action-icon',
                        handler: Ext.bind(function(callback) {
                            var understandRisks = this.panelStatus.down('checkbox[name="understandRisks"]');
                            if (!understandRisks.getValue()) {
                                Ext.MessageBox.alert(i18n._("Error"), i18n._("You must check:") + " " + i18n._("I understand the risks"));
                            }
                            else {
                                Ext.MessageBox.wait(i18n._("Clearing Cache..."), i18n._("Please wait"));
                                this.getRpcNode().clearSquidCache(Ext.bind(function(result, exception) {
                                    understandRisks.setValue(false);
                                    Ext.MessageBox.hide();
                                }, this));
                            }
                        }, this)
                    }]
                }]
            }]
        });
    },
    buildGridRules: function() {
        this.gridRules = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            name: 'gridRules',
            helpSource: 'web_cache_cache_bypass',
            title: i18n._("Cache Bypass"),
            qtip: i18n._("The Web Cache Bypass List contains host or domain names that should never be cached."),
            dataProperty:'rules',
            recordJavaClass: "com.untangle.node.web_cache.WebCacheRule",
            emptyRow: {
                "hostname": "",
                "live": true
            },
            sortField: "hostname",
            fields: [{
                name: 'id'
            },{
                name: 'hostname'
            },{
                name: 'live'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: "<b>"+i18n._("Enable")+"</b>",
                dataIndex: "live",
                resizable: false,
                width:55
            }, {
                header: i18n._("Hostname"),
                width: 200,
                dataIndex: "hostname",
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter hostname]"),
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                dataIndex: "live",
                fieldLabel: i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "hostname",
                fieldLabel: i18n._("Hostname"),
                emptyText: i18n._("[enter hostname]"),
                allowBlank: false,
                width: 400
            }]
        });
    },
    beforeSave: function(isApply, handler) {
        this.settings.rules.list = this.gridRules.getList();
        handler.call(this, isApply);
    },
    afterSave:function() {
        this.statistics = this.getRpcNode().getStatistics();
    }
});
//# sourceURL=web-cache-settings.js
