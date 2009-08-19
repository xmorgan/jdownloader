//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//mystream.to by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mystream.to" }, urls = { "http://[\\w\\.]*?mystream\\.to/file-[0-9]+-[0-9|a-z]+.+" }, flags = { 2 })
public class MyStreamTo extends PluginForHost {

    public MyStreamTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.mystream.to/contact";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Datei nicht gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("<h2>Sie schauen: (.*?)</h2>").getMatch(0);
            if (filename == null ) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setName(filename);
                return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("video/divx\" src=\"(.*?)\"").getMatch(0);
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -20);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}
