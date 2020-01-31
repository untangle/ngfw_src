Ext.define('Ung.cmp.RecordDetailsController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.recorddetails',

    onBeforeRender: function (view) {
        var me = this, masterGrid = view.up().down('grid'), sourceConfig = {};

        masterGrid.getView().on('select', me.masterGridSelect, me);

        Ext.Array.each(masterGrid.getColumns(), function (column) {
            if (!column.dataIndex) { return; }
            var displayName = column.text;
            if (column.ownerCt.text) {
                displayName = column.ownerCt.text + ' ' + displayName;
            }

            sourceConfig[column.dataIndex] = {
                displayName: displayName,
                renderer: column.renderer || null
            };
        });

        me.sourceConfig = sourceConfig;
    },

    /**
     * Display record details in the details panel
     * @param {Ext.data.Model} record
     */
    masterGridSelect: function (grid, record) {
        var me = this, recordData, data = {}, category;

        if (!record) { return; }

        recordData = record.getData();

        // delete extra non relevant attributes
        delete recordData._id;
        delete recordData.javaClass;
        delete recordData.state;
        delete recordData.attachments;
        delete recordData.tags;

        Ext.Object.each(recordData, function(key, value) {
            data[key] = value;
        });
        me.getView().setSource(data, me.sourceConfig);
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
