Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    /**
     * Listen when mastergrid selection change than update the details view
     * @param {Ext.grid.Panel} propgrid
     */
    init: function (propgrid) {
        var me = this;
        propgrid.getViewModel().bind('{entry}', function(entry) {
            if (!entry) { return; }
            me.tableName = entry.get('table');
        });
        propgrid.getViewModel().bind('{masterGrid.selection}', this.masterGridSelect, this);
    },

    /**
     * Display record details in the details panel
     * @param {Ext.data.Model} record
     */
    masterGridSelect: function (record) {
        var me = this, recordData, data = [], category, listeners;

        if (!record) {
            // empty the details view when no record selected
            me.getView().getStore().loadData([]);
            return;
        }

        // for thread prevention is fetching extra data
        listeners = TableConfig.tableConfig[me.tableName].listeners;
        if (listeners) {
            if (Ext.isFunction(listeners.select));
            listeners.select(record, function(response) {
                me.getView().getStore().add(response);
            });
        }

        recordData = record.getData();

        // delete extra non relevant attributes
        delete recordData._id;
        delete recordData.javaClass;
        delete recordData.state;
        delete recordData.attachments;
        delete recordData.tags;

        Ext.Object.each(recordData, function(key, value) {
            category = ' Event'.t();
            if(value != null) {
                // set grouping category
                if (key.startsWith('ad_blocker')) { category = 'Ad Blocker'; }
                if (key.startsWith('application_control')) { category = 'Application Control'; }
                if (key.startsWith('application_control_lite')) { category = 'Application Control Lite'; }
                if (key.startsWith('bandwidth_control')) { category = 'Bandwidth Control'; }
                if (key.startsWith('captive_portal')) { category = 'Captive Portal'; }
                if (key.startsWith('firewall')) { category = 'Firewall'; }
                if (key.startsWith('phish_blocker')) { category = 'Phish Blocker'; }
                if (key.startsWith('spam_blocker')) { category = 'Spam Blocker'; }
                if (key.startsWith('spam_blocker_lite')) { category = 'Spam Blocker Lite'; }
                if (key.startsWith('ssl_inspector')) { category = 'SSL Inspector'; }
                if (key.startsWith('virus_blocker')) { category = 'Virus Blocker'; }
                if (key.startsWith('virus_blocker_lite')) { category = 'Virus Blocker Lite'; }
                if (key.startsWith('web_filter')) { category = 'Web Filter'; }
                if (key.startsWith('threat_prevention')) { category = 'Threat Prevention'; }

                data.push({
                    // the human readable field name
                    name: Map.fields[key] ? Map.fields[key].col.text : key,
                    // in case the value is rendered use the renderer in details view too
                    value: (Map.fields[key] && Map.fields[key].col.renderer) ? Map.fields[key].col.renderer(value) : value,
                    // use categories for grouping purposes
                    category: category
                });
            }
        });
        me.getView().getStore().loadData(data);
    }
});
