Ext.define('Ung.view.dashboard.Queue', {
    alternateClassName: 'DashboardQueue',
    mixins: ['Ext.mixin.Observable'],

    singleton: true,
    // processing: false,
    // paused: false,
    queue: [],
    processing: false,
    // queueMap: {},
    // tout: null,

    add: function (widget) {
        this.queue.push(widget);
        // console.log(this.queue);
        this.process();
    },

    addFirst: function (widget) {
        this.queue.unshift(widget);
        this.process();
    },

    remove: function () {

    },

    process: function () {
        me = this;
        if (this.queue.length > 0 && !me.processing) {
            var wg = this.queue[0]; // this is the controller of the report
            // console.log(wg);
            // console.log(wg.getView().down('#report-widget'));
            me.processing = true;
            if (wg.tout) {clearTimeout(wg.tout); }

            if (wg.down('#report-widget')) {
                if (wg.down('#timer')) { wg.remove('timer'); }
                wg.down('#report-widget').getController().fetchData(null, function () {
                    Ext.Array.removeAt(me.queue, 0);
                    me.processing = false;
                    me.process();

                    var seconds = wg.getViewModel().get('widget.refreshIntervalSec');
                    if (seconds > 0) {
                        wg.add({ xtype: 'component', itemId: 'timer', cls: 'timer', html: '<div class="inner" style="animation: test ' + seconds + 's linear;"></div>' });
                        wg.tout = setTimeout(function () {
                            DashboardQueue.add(wg);
                        }, seconds * 1000);
                    }
                });
            } else {
                wg.fetchData(function () {
                    Ext.Array.removeAt(me.queue, 0);
                    me.processing = false;
                    me.process();
                    var seconds = wg.refreshIntervalSec;
                    if (seconds > 0) {
                        wg.tout = setTimeout(function () {
                            DashboardQueue.add(wg);
                        }, seconds * 1000);
                    }
                });
            }
        }
    }
});
