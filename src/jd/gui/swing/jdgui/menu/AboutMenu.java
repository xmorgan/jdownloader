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

package jd.gui.swing.jdgui.menu;

import javax.swing.JMenu;

import jd.gui.swing.jdgui.menu.actions.AboutAction;
import jd.gui.swing.jdgui.menu.actions.KnowledgeAction;
import jd.gui.swing.jdgui.menu.actions.LatestChangesAction;
import jd.gui.swing.jdgui.menu.actions.LogAction;

import org.jdownloader.gui.translate.T;

public class AboutMenu extends JMenu {

    private static final long serialVersionUID = 1899581616146592295L;

    public AboutMenu() {
        super(T._.gui_menu_about());

        this.add(new LogAction());
        this.addSeparator();
        this.add(new LatestChangesAction());
        this.add(new KnowledgeAction());
        this.addSeparator();
        this.add(new AboutAction());

    }
}