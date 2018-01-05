Ext.define('Ung.cmp.GridStatus', {
    extend: 'Ext.toolbar.TextItem',
    alias: 'widget.ungridstatus',

    listeners: {
        update: function () {
            var view = this,
                store = view.up('panel').down('grid').getStore(),
                count = store.getCount();
            view.update(Ext.String.format('Showing {0} of {1}'.t(), count, store.getData().getSource() ? store.getData().getSource().items.length : count));
        }
    }
});
