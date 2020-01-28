/**
 * Note!
 * Changed property grid to normal grid/store
 * 
 * PropertyGrid uses `source` and `sourceConfig` as props to set and populate data
 * `source` can be only a { key: value } object
 * - advantage: can be specified a renderer for each value from a row
 * - disadvantage: inability to have more data than the { key: value } (e.g. category)
 * 
 * Normal store:
 * - advantage: we can define multiple columns and extra properties allowing data grouping
 * - disadvantage: the renderer is specified on entire column
 *   (which cannot be applied in this case as each row represents a different column/value in master grid)
 */

Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    getBindRecordName: function(){
        var me = this,
            v = me.getView();

        var bindRecordName = 'propertyRecord';
        if( v.initialConfig.bind &&
            ( typeof( v.initialConfig.bind ) == 'object' ) &&
            v.initialConfig.bind.source ){
            bindRecordName = v.initialConfig.bind.source.substring( 1, v.initialConfig.bind.source.length - 1);
        }

        return bindRecordName;
    },

    /*
     * If property grid started collapsed it will have no data.
     * On expansion, force population based on first row in master grid
     */
    onBeforeExpand: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get(me.getBindRecordName()) == null ){
            v.up().down('grid').getView().getSelectionModel().select(0);
        }
    },

    onBeforeRender: function(){
        var me = this,
            propGrid = me.getView();

        /*
         * Build source list from accompanying grid's column definitions
         */
        var masterGrid = propGrid.up().down('grid');
        if (masterGrid != propGrid){
            masterGrid.getView().on('select', 'masterGridSelect', me );
        }
    },

    /*
     * When row selected by master grid, have the property grid properly massage
     * data suitable for property grid.
     *
     * So keep in mind that this is all in the contet of the "grid master" we're attached to,
     * not this property grid.
     */
    masterGridSelect: function (grid, record) {
        var me = this,
            vm = me.getViewModel(),
            propertyRecord = record.getData();

        // hide these attributes always
        delete propertyRecord._id;
        delete propertyRecord.javaClass;
        delete propertyRecord.state;
        delete propertyRecord.attachments;
        delete propertyRecord.tags;

        var data = [], category;

        Ext.Object.each( propertyRecord, function(key, value){
            category = ' Event'.t();
            if(value != null) {
                // create grouping
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
                if (key.startsWith('vweb_filter')) { category = 'Web Filter'; }
                if (key.startsWith('threat_prevention')) { category = 'Threat Prevention'; }

                data.push({
                    name: Map.fields[key] ? Map.fields[key].col.text : key,
                    value: value,
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
