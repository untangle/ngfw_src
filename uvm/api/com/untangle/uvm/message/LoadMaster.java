/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm/api/com/untangle/uvm/logging/LoggingSettings.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.message;

import java.util.HashSet;
import java.util.Set;

import com.untangle.uvm.util.Pulse;

/**
 * Maintains load statistics by periodically strobing a source for its
 * current value.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class LoadMaster implements LoadStats
{
    private static Pulse pulse = null;
    private static final Set<LoadMaster> loadMasters
        = new HashSet<LoadMaster>();

    private final LoadStrober loadStrober;
    private final StatDesc statDesc;

    // constructors -----------------------------------------------------------

    public LoadMaster(LoadStrober loadStrober, String name,
                      String displayName)
    {
        this.loadStrober = loadStrober;
        this.statDesc = new StatDesc(name, displayName, null, null, false);
    }

    // public methods ---------------------------------------------------------

    public StatDesc getStatDescs()
    {
        return statDesc;
    }

    /**
     * Start strobing the source.
     */
    public void start()
    {
        synchronized (LoadMaster.class) {
            if (pulse == null) {
                pulse = new Pulse("stats updater", true,
                                  new Runnable() {
                                      public void run()
                                      {
                                          for (LoadMaster lm : loadMasters) {
                                              lm.updateStats();
                                          }
                                      }
                                  });
            }
            loadMasters.add(this);
        }
    }

    /**
     * Stop strobing the source.
     */
    public void stop()
    {
        synchronized (LoadMaster.class) {
            loadMasters.remove(this);
            if (loadMasters.isEmpty()) {
                pulse.stop();
                pulse = null;
            }
        }
    }

    public LoadStrober getLoadStrober()
    {
        return loadStrober;
    }

    // StateKeeper methods ----------------------------------------------------

    public float get1MinuteAverage()
    {
        return 0;
    }

    public float get5MinuteAverage()
    {
        return 0;
    }

    public float get15MinuteAverage()
    {
        return 0;
    }

    // private methods ---------------------------------------------------------

    private void updateStats()
    {
        // XXX DO IT!
        // call loadStrober.updateValue();
    }
}