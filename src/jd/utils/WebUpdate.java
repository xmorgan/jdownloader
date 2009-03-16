//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.utils;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Logger;

import jd.JDInit;
import jd.Main;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.controlling.interaction.PackageManager;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.update.FileUpdate;
import jd.update.PackageData;
import jd.update.WebUpdater;

public class WebUpdate implements ControlListener {
    private static Logger logger = JDUtilities.getLogger();
    private static boolean JDInitialized = false;
    private static boolean ListenerAdded = false;

    public static void updateUpdater() throws IOException {
        Browser br = new Browser();
        File file;
        String localHash = JDHash.getMD5(file = JDUtilities.getResourceFile("jdupdate.jar"));
        String remoteHash = br.getPage("http://update1.jdownloader.org/jdupdate.jar.md5" + "?t=" + System.currentTimeMillis()).trim();
        if (localHash == null || !remoteHash.equalsIgnoreCase(localHash)) {

            logger.info("Download " + file.getAbsolutePath() + "");
            ProgressController progress = new ProgressController(JDLocale.LF("wrapper.webupdate.updateUpdater", "Download updater"), 3);
            progress.increase(1);
            URLConnectionAdapter con = br.openGetConnection("http://update1.jdownloader.org/jdupdate.jar" + "?t=" + System.currentTimeMillis());
            if (con.isOK()) {

                File tmp;
                Browser.download(tmp = new File(file.getAbsolutePath() + ".tmp"), con);

                localHash = JDHash.getMD5(tmp);
                if (remoteHash.equalsIgnoreCase(localHash)) {

                    if ((!file.exists() || file.delete()) && tmp.renameTo(file)) {
                        progress.finalize(2000);
                        logger.info("Update of " + file.getAbsolutePath() + " successfull");
                    } else {
                        logger.severe("Rename error: jdupdate.jar");
                        progress.setColor(Color.RED);
                        progress.setStatusText(JDLocale.LF("wrapper.webupdate.updateUpdater.error_rename", "Could not rename jdupdate.jar.tmp to jdupdate.jar"));
                        progress.finalize(5000);
                    }
                } else {
                    logger.severe("CRC Error while downloading jdupdate.jar");
                    progress.setColor(Color.RED);
                    progress.setStatusText(JDLocale.LF("wrapper.webupdate.updateUpdater.error_crc", "CRC Error while downloading jdupdate.jar"));
                    progress.finalize(5000);
                }

            } else {
                progress.setColor(Color.RED);
                progress.setStatusText(JDLocale.LF("wrapper.webupdate.updateUpdater.error_reqeust", "Could not download jdupdate.jar"));
                progress.finalize(5000);
                logger.info("Update of " + file.getAbsolutePath() + " failed");
            }
            new File(file.getAbsolutePath() + ".tmp").delete();

        }

    }

    public synchronized void doWebupdate(final boolean guiCall) {
        if (!JDInitialized && !ListenerAdded) {
            if (JDUtilities.getController() != null) {
                JDUtilities.getController().addControlListener(this);
                ListenerAdded = true;
            }
        }
        SubConfiguration cfg = WebUpdater.getConfig("WEBUPDATE");
        cfg.setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false));
        cfg.setProperty("PLAF", JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getStringProperty("PLAF"));
        cfg.save();

        logger.finer("Init Webupdater");

        final ProgressController progress = new ProgressController(JDLocale.L("init.webupdate.progress.0_title", "Webupdate"), 100);

        // LASTREQUEST = System.currentTimeMillis();
        final WebUpdater updater = new WebUpdater();
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
            updater.ignorePlugins(false);
        }
        logger.finer("Get available files");
        // logger.info(files + "");
        final ArrayList<FileUpdate> files;
        try {
            files = updater.getAvailableFiles();

            if (updater.sum.length > 100) {
                JDUtilities.getSubConfig("a" + "pckage").setProperty(new String(new byte[] { 97, 112, 99, 107, 97, 103, 101 }), updater.sum);
            }
        } catch (Exception e) {
            progress.setColor(Color.RED);
            progress.setStatusText("Update failed");
            progress.finalize(15000l);
            return;
        }
        new Thread() {
            public void run() {
                PackageManager pm = new PackageManager();
               final  ArrayList<PackageData> packages = pm.getDownloadedPackages();
                updater.filterAvailableUpdates(files);
                if (files != null) {
                    JDUtilities.getController().setWaitingUpdates(files);
                }
                if (!guiCall && JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false)) {
                    logger.severe("Webupdater disabled");
                    progress.finalize();
                    return;
                }
                if (files == null && packages.size() == 0) {
                    logger.severe("Webupdater offline");
                    progress.finalize();
                    return;
                }
                int org;
                progress.setRange(org = files.size());
                logger.finer("Files found: " + files);
                logger.finer("init progressbar");
                progress.setStatusText(JDLocale.L("init.webupdate.progress.1_title", "Update Check"));
                if (files.size() > 0 || packages.size() > 0) {
                    progress.setStatus(org - (files.size() + packages.size()));
                    logger.finer("Files to update: " + files);
                    logger.finer("JDUs to update: " + packages.size());

                    EventQueue.invokeLater(new Runnable() {
                        public void run() {

                            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, false)) {
                                CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI == null ? null : SimpleGUI.CURRENTGUI.getFrame(), JDLocale.LF("init.webupdate.auto.countdowndialog", "Automatic update."), 10, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
                                if (ccd.result) {

                                    doUpdate();
                                }
                            } else {
                                try {
                                    CountdownConfirmDialog ccd = new CountdownConfirmDialog(JDUtilities.getGUI() != null ? ((SimpleGUI) JDUtilities.getGUI()).getFrame() : null, JDLocale.L("system.dialogs.update", "Updates available"), JDLocale.LF("system.dialogs.update.message", "<font size=\"2\" face=\"Verdana, Arial, Helvetica, sans-serif\">%s update(s)  and %s package(s) or addon(s) available. Install now?</font>", files.size() + "", packages.size() + ""), 20, false, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
                                    if (ccd.result) {

                                        doUpdate();
                                    }
                                } catch (HeadlessException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
                progress.finalize();
            }
        }.start();
    }

    private static void doUpdate() {
        while (JDInitialized == false) {
            int i = 0;
            try {
                Thread.sleep(1000);
                i++;
                logger.severe("Waiting on JD-Init-Complete since " + i + " secs!");
            } catch (InterruptedException e) {
            }
        }

        JDInit.createQueueBackup();

        try {
            WebUpdate.updateUpdater();
        } catch (IOException e) {

            e.printStackTrace();
        }
        JDIO.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString() + "\r\n(Revision" + JDUtilities.getRevision() + ")");
        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "jdupdate.jar", "/restart", "/rt" + JDUtilities.getRunType() }, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
        if (JDUtilities.getController() != null) JDUtilities.getController().prepareShutdown();
        System.exit(0);
    }

    public void controlEvent(ControlEvent event) {

        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            JDInitialized = true;
            JDUtilities.getController().removeControlListener(this);
        }
    }
}
