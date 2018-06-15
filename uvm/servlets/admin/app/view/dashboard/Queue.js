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
        if (!widget || !widget.getEl()) { return; }
        var widgetGeometry = widget.getEl().dom.getBoundingClientRect();
        widget.visible = (widgetGeometry.top + widgetGeometry.height / 2) > 0 && (widgetGeometry.height / 2 + widgetGeometry.top < window.innerHeight);
        if (widget.visible) {
            DashboardQueue.add(widget);
        }
    },

    process: function () {
        var me = this, wg = me.queue[0];

        if (me.queue.length > 0 && !me.processing) {

            /**
             * timeout computes if the widget needs to be refreshed based on refreshIntervalSec
             * for custom widgets refreshIntervalSec is defined in view
             * for report widgets refreshIntervalSec is defined in viewmodel
             * if lastFetchTime = null means the widgets did not fatch data at all, it was just being rendered
             */
            if (wg.lastFetchTime === null) {
                wg.timeout = true;
            } else {
                if (wg.refreshIntervalSec === 0) {
                    wg.timeout = false;
                } else {
                    wg.timeout = ((new Date()).getTime() - wg.lastFetchTime)/1000 - (wg.refreshIntervalSec || wg.getViewModel().get('widget.refreshIntervalSec')) > 0;
                }
            }

            /**
             * if queue is paused (e.g. not in Dashboard view) or
             * widget is not in visible area of the screen or
             * it hasn't passed the timeout to be refreshed
             * just remove it from queue and skip fetching
             */
            if (me.paused || !wg.visible || !wg.timeout || wg.isMasked()) {
                Ext.Array.removeAt(me.queue, 0);
                me.processing = false;
                if (me.queue.length > 0) { me.process(); }
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
                    if (me.queue.length > 0) { me.process(); }
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
                    if (me.queue.length > 0) { me.process(); }
                });
            }
        }
    }
});
