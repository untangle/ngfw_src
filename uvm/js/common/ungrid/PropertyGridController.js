Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    /**
     * Listen when mastergrid selection change than update the details view
     * @param {Ext.grid.Panel} propgrid
     */
    init: function (propgrid) {
        var me = this;
        // propgrid.getViewModel().bind('{entry}', function() {
        //     me.table = entry.get('table');
        // });
        propgrid.getViewModel().bind('{masterGrid.selection}', this.masterGridSelect, this);
    },

    /**
     * Display record details in the details panel
     * @param {Ext.data.Model} record
     */
    masterGridSelect: function (record) {
        var me = this, recordData, data = [], category;

        // empty the details view when no record selected
        if (!record) {
            me.getView().getStore().loadData([]);
            return;
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
    },

    /**
     * Used for extra column actions which can be added to the grid but are very specific to that context
     * The grid requires to have defined a parentView tied to the controller on which action method is implemented
     * action - is an extra configuration set on actioncolumn and represents the name of the method to be called
     * see Users/UsersController implementation
     */
    externalAction: function (v, rowIndex, colIndex, item, e, record) {
        var view = this.getView(),
            parentController = null,
            action = item && item.action ? item.action : v.action;

        while( view != null){
            parentController = view.getController();

            if( parentController && parentController[action]){
                break;
            }
            view = view.up();
        }

        if (!parentController) {
            console.log('Unable to get the extra controller');
            return;
        }

        // call the action from the extra controller in extra controller scope, and pass all the actioncolumn arguments
        if (action) {
            parentController[action].apply(parentController, arguments);
        } else {
            console.log('External action not defined!');
        }
    }
});
