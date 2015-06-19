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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "spankwire.com" }, urls = { "http://(www\\.)?spankwire\\.com/(.*?/video\\d+|EmbedPlayer\\.aspx/?\\?ArticleId=\\d+)" }, flags = { 0 })
public class SpankWireCom extends PluginForHost {

    public String DLLINK = null;

    public SpankWireCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.spankwire.com/Terms.aspx";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    // main code by external user "hpdub33"
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://spankwire.com/", "performance_timing", "video");
        br.setCookie("http://spankwire.com/", "Tablet_False", "");
        br.setCookie("http://spankwire.com/", "init", "Straight^^false^false^^None");
        br.setCookie("http://spankwire.com/", "adultd", "1");

        br.getPage(downloadLink.getDownloadURL());
        // Invalid link
        if (br.getURL().equals("http://www.spankwire.com/") || br.containsHTML("removedCopyright\\.jpg\"") || br.containsHTML("removedTOS\\.jpg\"") || br.containsHTML("removedDefault\\.jpg\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // Link offline
        if (br.containsHTML(">This (article|video) has been (deleted|disabled)") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
        if (fileID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = null;
        if (!downloadLink.getDownloadURL().contains("EmbedPlayer.aspx")) {
            filename = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- Spankwire\\.com\\s*?</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<meta name=\"Description\" content=\"Watch (.*?) on Spankwire now\\!").getMatch(0);
                }
            }
        } else {
            if (br.containsHTML("This video is temporarily unavailable")) {
                downloadLink.getLinkStatus().setStatusText("This video is temporarily unavailable!");
                downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
                return AvailableStatus.TRUE;
            }
            filename = br.getRegex("playerData.articleTitle\\s+= \'(([^\']*?))\';").getMatch(0);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename.trim());
                filename = filename.replaceAll("\\+", " ");
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (filename.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setName(filename.trim());
        // File not found can have good name
        if (br.containsHTML("playerData\\.isVideoUnavailable\\s*=\\s*true;")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        DLLINK = finddllink();
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        if (DLLINK.contains(".mp4")) {
            downloadLink.setFinalFileName(filename + ".mp4");
        } else {
            downloadLink.setFinalFileName(filename + ".flv");
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = br2.openGetConnection(DLLINK);
        if (!con.getContentType().contains("html")) {
            downloadLink.setDownloadSize(con.getLongContentLength());
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().contains("EmbedPlayer.aspx") && br.containsHTML("This video is temporarily unavailable")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This video is temporarily unavailable!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String finddllink() {
        final String[] qualities = { "720", "480", "240", "180" };
        String dllink = null;
        for (final String quality : qualities) {
            dllink = br.getRegex("cdnPath" + quality + "[^<>\"]*?(\"|\')(http[^<>\"\\']*?\\.mp4[^<>\"\\']*?)(\"|\')").getMatch(1);
            if (dllink != null) {
                break;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}