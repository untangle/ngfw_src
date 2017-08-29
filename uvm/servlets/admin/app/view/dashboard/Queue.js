Ext.define('Ung.view.dashboard.Queue', {
    alternateClassName: 'DashboardQueue',
    singleton: true,
    queue: [],
    processing: false,

    paused: false,

    add: function (widget) {
        this.queue.push(widget);
        this.process();
    },

    addFirst: function (widget) {
        this.queue.unshift(widget);
        this.process();
    },

    isVisible: function (widget) {
        if (!widget) { return; }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        widget.visible = (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
        if (widget.visible) {
            DashboardQueue.add(widget);
        }
    },

    process: function () {
        me = this;
        if (this.queue.length > 0 && !me.processing) {
            // console.info('processing', me.processing);
            var wg = this.queue[0], seconds;

            /**
             * if queue is paused (e.g. not in Dashboard view) or
             * widget is not in visible area of the screen or
             * it hasn't passed the timeout to be refreshed
             * just remove it from queue and skip fetching
             */
            if (me.paused || !wg.visible ||
                wg.lastFetchTime && ((new Date()).getTime() - wg.lastFetchTime)/1000 <= wg.getViewModel().get('widget.refreshIntervalSec')
            ) {
                Ext.Array.removeAt(me.queue, 0);
                me.processing = false;
                me.process();
                return;
            }

            me.processing = true;
            if (wg.tout) {clearTimeout(wg.tout); }

            if (wg.down('#report-widget')) {
                wg.down('#report-widget').getController().fetchData(null, function () {
                    var seconds = wg.getViewModel().get('widget.refreshIntervalSec');
                    if (seconds > 0) {
                        // wg.add({ xtype: 'component', itemId: 'timer', cls: 'timer', html: '<div class="inner" style="animation: test ' + seconds + 's linear;"></div>' });
                        wg.tout = setTimeout(function () {
                            DashboardQueue.add(wg);
                        }, seconds * 1000);
                    }
                    wg.lastFetchTime = new Date().getTime();
                    Ext.Array.removeAt(me.queue, 0);
                    me.processing = false;
                    me.process();
                });
            } else {
                wg.fetchData(function () {
                    var seconds = wg.refreshIntervalSec;
                    if (seconds > 0) {
                        wg.tout = setTimeout(function () {
                            DashboardQueue.add(wg);
                        }, seconds * 1000);
                    }
                    wg.lastFetchTime = new Date().getTime();
                    Ext.Array.removeAt(me.queue, 0);
                    me.processing = false;
                    me.process();
                });
            }
        }
    }
});
