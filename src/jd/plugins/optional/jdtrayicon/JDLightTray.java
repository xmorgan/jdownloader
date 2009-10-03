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

package jd.plugins.optional.jdtrayicon;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import jd.Main;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.event.ControlEvent;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Formatter;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "trayicon", interfaceversion = 5, minJVM = 1.6)
public class JDLightTray extends PluginOptional implements MouseListener, MouseMotionListener, WindowListener, LinkGrabberControllerListener, WindowStateListener {

    private SubConfiguration subConfig = null;

    private static final String PROPERTY_START_MINIMIZED = "PROPERTY_START_MINIMIZED";

    private static final String PROPERTY_MINIMIZE_TO_TRAY = "PROPERTY_MINIMIZE_TO_TRAY";

    private static final String PROPERTY_SINGLE_CLICK = "PROPERTY_SINGLE_CLICK";

    private static final String PROPERTY_TOOLTIP = "PROPERTY_TOOLTIP";

    private static final String PROPERTY_SHOW_ON_LINKGRAB = "PROPERTY_SHOW_ON_LINKGRAB";

    private static final String PROPERTY_SHOW_INFO_IN_TITLE = "PROPERTY_SHOW_INFO_IN_TITLE";

    private TrayIconPopup trayIconPopup;

    private TrayIcon trayIcon;

    private JFrame guiFrame;

    private TrayIconTooltip trayIconTooltip;

    private TrayMouseAdapter ma;

    private Thread updateThread;

    private boolean shutdown = false;

    private boolean iconified = false;

    public JDLightTray(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = SubConfiguration.getConfig("ADDONS_JDLIGHTTRAY");
        initConfig();
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public boolean initAddon() {
        return new GuiRunnable<Boolean>() {

            @Override
            public Boolean runSave() {
                if (JDUtilities.getJavaVersion() < 1.6) {
                    logger.severe("Error initializing SystemTray: Tray is supported since Java 1.6. your Version: " + JDUtilities.getJavaVersion());
                    return false;
                }
                if (!SystemTray.isSupported()) {
                    logger.severe("Error initializing SystemTray: Tray isn't supported jet");
                    return false;
                }
                try {
                    JDUtilities.getController().addControlListener(JDLightTray.this);
                    if (SwingGui.getInstance() != null) {
                        guiFrame = SwingGui.getInstance().getMainFrame();
                        if (guiFrame != null) {
                            guiFrame.removeWindowListener(JDLightTray.this);
                            guiFrame.addWindowListener(JDLightTray.this);
                            guiFrame.removeWindowStateListener(JDLightTray.this);
                            guiFrame.addWindowStateListener(JDLightTray.this);
                        }
                    }
                    logger.info("Systemtray OK");
                    initGUI();
                } catch (Exception e) {
                    return false;
                }
                LinkGrabberController.getInstance().addListener(JDLightTray.this);
                return true;
            }

        }.getReturnValue();

    }

    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_MINIMIZE_TO_TRAY, JDL.L("plugins.optional.JDLightTray.minimizetotray", "Minimize to tray")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_START_MINIMIZED, JDL.L("plugins.optional.JDLightTray.startMinimized", "Start minimized")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SINGLE_CLICK, JDL.L("plugins.optional.JDLightTray.singleClick", "Toggle window status with single click")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_TOOLTIP, JDL.L("plugins.optional.JDLightTray.tooltip", "Show Tooltip")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SHOW_ON_LINKGRAB, JDL.L("plugins.optional.JDLightTray.linkgrabber", "Show on Linkgrabbing")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PROPERTY_SHOW_INFO_IN_TITLE, JDL.L("plugins.optional.JDLightTray.titleinfo", "Show info in TaskBar when minimized")).setDefaultValue(true));
    }

    @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            logger.info("JDLightTrayIcon Init complete");
            guiFrame = SwingGui.getInstance().getMainFrame();
            if (guiFrame != null) {
                guiFrame.removeWindowListener(JDLightTray.this);
                guiFrame.addWindowListener(JDLightTray.this);
                guiFrame.removeWindowStateListener(JDLightTray.this);
                guiFrame.addWindowStateListener(JDLightTray.this);
            }
            if (subConfig.getBooleanProperty(PROPERTY_START_MINIMIZED, false)) {
                miniIt(true);
            }
        } else if (event.getID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            shutdown = true;
        } else if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_START) {
            updateThread = new Thread() {
                @Override
                public void run() {
                    boolean needupdate = false;
                    while (true) {
                        if (DownloadWatchDog.getInstance().getDownloadStatus() != DownloadWatchDog.STATE.RUNNING) break;
                        if (iconified && subConfig.getBooleanProperty(PROPERTY_SHOW_INFO_IN_TITLE, true)) {
                            needupdate = true;
                            JDGui.getInstance().setWindowTitle("JD AC: " + DownloadWatchDog.getInstance().getActiveDownloads() + " DL: " + Formatter.formatReadable(DownloadWatchDog.getInstance().getTotalSpeed()));
                        } else {
                            if (needupdate) {
                                JDGui.getInstance().setWindowTitle(JDUtilities.getJDTitle());
                                needupdate = false;
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                    }
                    JDGui.getInstance().setWindowTitle(JDUtilities.getJDTitle());
                }
            };
            updateThread.start();
        } else if (event.getID() == ControlEvent.CONTROL_DOWNLOAD_STOP) {
            if (updateThread != null) updateThread.interrupt();
            JDGui.getInstance().setWindowTitle(JDUtilities.getJDTitle());
        }
        super.controlEvent(event);
    }

    private void initGUI() {
        SystemTray systemTray = SystemTray.getSystemTray();
        Image img = JDImage.getImage("logo/jd_logo_128_128").getScaledInstance((int) systemTray.getTrayIconSize().getWidth(), (int) systemTray.getTrayIconSize().getHeight(), Image.SCALE_SMOOTH);
        /*
         * trayicon message must be set, else windows cannot handle icon right
         * (eg autohide feature)
         */
        trayIcon = new TrayIcon(img, "JDownloader");
        trayIcon.addActionListener(this);

        ma = new TrayMouseAdapter(this, trayIcon);
        trayIcon.addMouseListener(ma);
        trayIcon.addMouseMotionListener(ma);

        trayIconTooltip = new TrayIconTooltip();

        try {
            systemTray.add(trayIcon);
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        trayIconTooltip.hideWindow();
    }

    public void mousePressed(MouseEvent e) {
        trayIconTooltip.hideWindow();
        if (e.getSource() instanceof TrayIcon) {
            if (!OSDetector.isMac()) {
                if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isRightMouseButton(e)) {
                    miniIt(guiFrame.isVisible());
                } else {
                    if (trayIconPopup != null && trayIconPopup.isShowing()) {
                        trayIconPopup.dispose();
                        trayIconPopup = null;
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        trayIconPopup = new TrayIconPopup();
                        calcLocation(trayIconPopup, e.getPoint());
                        trayIconPopup.setVisible(true);
                        trayIconPopup.startAutoHide();
                    }
                }
            } else if (e.getSource() instanceof JWindow) {

            } else {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() >= (subConfig.getBooleanProperty(PROPERTY_SINGLE_CLICK, false) ? 1 : 2) && !SwingUtilities.isLeftMouseButton(e)) {
                        miniIt(guiFrame.isVisible());
                    } else {
                        if (trayIconPopup != null && trayIconPopup.isShowing()) {
                            trayIconPopup.dispose();
                            trayIconPopup = null;
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            trayIconPopup = new TrayIconPopup();
                            Point pointOnScreen = e.getLocationOnScreen();
                            if (e.getX() > 0) pointOnScreen.x -= e.getPoint().x;
                            calcLocation(trayIconPopup, pointOnScreen);
                            trayIconPopup.setVisible(true);
                            trayIconPopup.startAutoHide();
                        }
                    }
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void onExit() {
        removeTrayIcon();
        LinkGrabberController.getInstance().removeListener(this);
        if (guiFrame != null) {
            guiFrame.removeWindowListener(this);
            guiFrame.removeWindowStateListener(this);
            if (!shutdown) miniIt(false);
            guiFrame.setAlwaysOnTop(false);
        }
    }

    private void calcLocation(final JWindow window, final Point p) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                int limitX = (int) screenSize.getWidth() / 2;
                int limitY = (int) screenSize.getHeight() / 2;
                if (!OSDetector.isMac()) {
                    if (p.x <= limitX) {
                        if (p.y <= limitY) {
                            // top left
                            window.setLocation(p.x, p.y);
                        } else {
                            // bottom left
                            window.setLocation(p.x, p.y - window.getHeight());
                        }
                    } else {
                        if (p.y <= limitY) {
                            // top right
                            window.setLocation(p.x - window.getWidth(), p.y);
                        } else {
                            // bottom right
                            window.setLocation(p.x - window.getWidth(), p.y - window.getHeight());
                        }
                    }
                } else {
                    if (p.getX() <= (screenSize.getWidth() - window.getWidth())) {
                        window.setLocation((int) p.getX(), 22);
                    } else {
                        window.setLocation(p.x - window.getWidth(), 22);
                    }
                }

                return null;
            }
        }.waitForEDT();
    }

    private void miniIt(final boolean minimize) {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                /* set visible state */
                guiFrame.setVisible(!minimize);
                return null;
            }
        }.start();
        if (minimize == false) {
            /* workaround for : toFront() */
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    guiFrame.setAlwaysOnTop(true);
                    guiFrame.toFront();
                    return null;
                }
            }.start();
        }
    }

    /**
     * gets called if mouse stays over the tray. Edit delay in
     * TrayJDMouseAdapter
     * 
     * @param me
     */
    public void mouseStay(MouseEvent e) {
        if (!subConfig.getBooleanProperty(PROPERTY_TOOLTIP, true)) return;
        if (trayIconPopup != null && trayIconPopup.isVisible()) return;
        trayIconTooltip.show(((TrayMouseAdapter) e.getSource()).getEstimatedTopLeft(), this.trayIcon);
    }

    private void removeTrayIcon() {
        try {
            if (trayIcon != null) {
                trayIcon.removeActionListener(this);
                SystemTray.getSystemTray().remove(trayIcon);
                if (ma != null) {
                    trayIcon.removeMouseListener(ma);
                    trayIcon.removeMouseMotionListener(ma);
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public Object interact(String command, Object parameter) {
        if (command == null) return null;
        if (command.equalsIgnoreCase("enabled")) return subConfig.getBooleanProperty(PROPERTY_MINIMIZE_TO_TRAY, true);
        if (command.equalsIgnoreCase("refresh")) {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    removeTrayIcon();
                    initGUI();
                    return null;
                }
            }.start();
        }
        return null;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        if (subConfig.getBooleanProperty(PROPERTY_MINIMIZE_TO_TRAY, true)) {
            miniIt(true);
        }
    }

    public void windowDeactivated(WindowEvent e) {
        /* workaround for : toFront() */
        if (guiFrame != null) guiFrame.setAlwaysOnTop(false);
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        if (event.getID() == LinkGrabberControllerEvent.NEW_LINKS && subConfig.getBooleanProperty(PROPERTY_SHOW_ON_LINKGRAB, true)) {
            if (!guiFrame.isVisible()) {
                /* set visible */
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        guiFrame.setVisible(true);
                        return null;
                    }
                }.start();
                /* workaround for : toFront() */
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        guiFrame.setAlwaysOnTop(true);
                        guiFrame.toFront();
                        return null;
                    }
                }.start();
            }
            if (iconified) {
                /* restore normale state,if windows was iconified */
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        /* after this normal, its back to iconified */
                        guiFrame.setState(JFrame.NORMAL);
                        return null;
                    }
                }.start();
            }
        }
    }

    public void windowStateChanged(WindowEvent evt) {
        int oldState = evt.getOldState();
        int newState = evt.getNewState();

        if ((oldState & Frame.ICONIFIED) == 0 && (newState & Frame.ICONIFIED) != 0) {
            iconified = true;
            // Frame was not iconified
        } else if ((oldState & Frame.ICONIFIED) != 0 && (newState & Frame.ICONIFIED) == 0) {
            iconified = false;
            // Frame was iconified
        }
        if ((oldState & Frame.MAXIMIZED_BOTH) == 0 && (newState & Frame.MAXIMIZED_BOTH) != 0) {
            // Frame was maximized
        } else if ((oldState & Frame.MAXIMIZED_BOTH) != 0 && (newState & Frame.MAXIMIZED_BOTH) == 0) {
            // Frame was minimized
        }

    }
}