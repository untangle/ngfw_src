Ext.define('Webui.untangle-node-web-cache.settings', {
    extend: 'Ung.NodeWin',
    gridProtocolList: null,
    gridEventLog: null,
    initComponent: function() {
        this.statistics = this.getRpcNode().getStatistics();
        this.buildStatus();
        this.buildGridRules();

        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus, this.gridRules]);
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
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            name: 'Status',
            helpSource: 'web_cache_status',
            title: this.i18n._('Status'),
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            isDirty: function() {
                return false;
            },
            items: [{
                title: this.i18n._('Note'),
                html: this.i18n._("Web Cache provides HTTP content caching.  This status page allows you to monitor overall cache usage and effectiveness.")
            }, {
                title: this.i18n._('Statistics'),
                defaults: {
                    xtype: "displayfield",
                    labelWidth: 230
                },
                items: [{
                    fieldLabel: this.i18n._('Cache Hit Count'),
                    name: 'hitCount',
                    value: this.statFormat(this.statistics.hitCount)
                },{
                    fieldLabel: this.i18n._('Cache Miss Count'),
                    name: 'missCount',
                    value: this.statFormat(this.statistics.missCount)
                },{
                    fieldLabel: this.i18n._('Cache Hit Bytes'),
                    name: 'hitBytes',
                    value: this.statFormat(this.statistics.hitBytes)
                },{
                    fieldLabel: this.i18n._('Cache Miss Bytes'),
                    name: 'missBytes',
                    value: this.statFormat(this.statistics.missBytes)
                },{
                    fieldLabel: this.i18n._('User Bypass Count'),
                    name: 'bypassCount',
                    value: this.statFormat(this.statistics.bypassCount)
                },{
                    fieldLabel: this.i18n._('System Bypass Count'),
                    name: 'systemCount',
                    value: this.statFormat(this.statistics.systemCount)
                }]
            },{
                title: this.i18n._('Clear Cache'),
                items: [{
                    xtype: 'component',
                    html: this.i18n._('If content stored in the cache somehow becomes stale or corrupt, the cache can be cleared with the ') + "<b>" +
                          this.i18n._('Clear Cache') + "</b>" + this.i18n._(" button.") + "<br><br>" + "<b>" +
                          this.i18n._("Caution") + ":  </b>" + this.i18n._("Clearing the cache requires restarting the caching engine. ") +
                          this.i18n._("This will cause active web sessions to be dropped and may disrupt web traffic for several seconds.")
                }, {
                    xtype: 'container',
                    layout: {type: 'column'},
                    margin: '10 0 0 0',
                    items: [{
                        xtype: 'checkbox',
                        boxLabel: this.i18n._('I understand the risks.'),
                        name: 'understandRisks',
                        hideLabel: true,
                        checked: false,
                        margin: '0 40 0 0'
                    }, {
                        xtype: 'button',
                        text: this.i18n._('Clear Cache'),
                        name: 'Clear Cache',
                        iconCls: 'action-icon',
                        handler: Ext.bind(function(callback) {
                            var understandRisks = this.panelStatus.down('checkbox[name="understandRisks"]');
                            if (!understandRisks.getValue()) {
                                Ext.MessageBox.alert(this.i18n._("Error"), this.i18n._("You must check: ") + this.i18n._("I understand the risks"));
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
            title: this.i18n._("Cache Bypass"),
            qtip: this.i18n._("The Web Cache Bypass List contains host or domain names that should never be cached."),
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
                header: "<b>"+this.i18n._("Enable")+"</b>",
                dataIndex: "live",
                resizable: false,
                width:55
            }, {
                header: this.i18n._("Hostname"),
                width: 200,
                dataIndex: "hostname",
                flex:1,
                editor: {
                    xtype:'textfield',
                    emptyText: this.i18n._("[enter hostname]"),
                    allowBlank: false
                }
            }],
            rowEditorInputLines: [{
                xtype:'checkbox',
                dataIndex: "live",
                fieldLabel: this.i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "hostname",
                fieldLabel: this.i18n._("Hostname"),
                emptyText: this.i18n._("[enter hostname]"),
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