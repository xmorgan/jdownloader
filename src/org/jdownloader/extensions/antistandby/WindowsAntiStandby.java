//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.antistandby;

import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class WindowsAntiStandby extends Thread implements Runnable {

    private final AtomicBoolean        lastState = new AtomicBoolean(false);
    private static final int           sleep     = 5000;
    private final AntiStandbyExtension jdAntiStandby;
    private final Kernel32             kernel32;
    private final LogSource            logger;

    public WindowsAntiStandby(final AntiStandbyExtension jdAntiStandby) {
        super();
        this.jdAntiStandby = jdAntiStandby;
        this.setDaemon(true);
        setName("WindowsAntiStandby");
        kernel32 = (Kernel32) com.sun.jna.Native.loadLibrary("kernel32", Kernel32.class);
        logger = LogController.CL(AntiStandbyExtension.class);
    }

    @Override
    public void run() {
        try {
            while (jdAntiStandby.isAntiStandbyThread()) {
                switch (jdAntiStandby.getMode()) {
                case DOWNLOADING:
                    if (DownloadWatchDog.getInstance().getStateMachine().isState(DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                        enableAntiStandby(true);
                    } else {
                        enableAntiStandby(false);
                    }
                    break;
                case RUNNING:
                    enableAntiStandby(true);
                    break;
                default:
                    logger.finest("JDAntiStandby: Config error (unknown mode: " + jdAntiStandby.getMode() + ")");
                    break;
                }
                sleep(sleep);
            }
        } catch (Throwable e) {
            logger.log(e);
        } finally {
            try {
                enableAntiStandby(false);
            } catch (final Throwable e) {
            } finally {
                logger.fine("JDAntiStandby: Terminated");
                logger.close();
            }
        }
    }

    private void enableAntiStandby(final boolean enabled) {
        if (lastState.compareAndSet(!enabled, enabled)) {
            if (enabled) {
                if (jdAntiStandby.getSettings().isDisplayRequired()) {
                    kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_DISPLAY_REQUIRED);
                } else {
                    kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS | Kernel32.ES_SYSTEM_REQUIRED);
                }
                logger.fine("JDAntiStandby: Start");
            } else {
                kernel32.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
                logger.fine("JDAntiStandby: Stop");
            }
        }
    }

}
