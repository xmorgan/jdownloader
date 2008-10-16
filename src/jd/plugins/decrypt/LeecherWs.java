//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LeecherWs extends PluginForDecrypt {

    public LeecherWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String outLinks[] = null;
        if (parameter.indexOf("out") != -1) {
            outLinks = new String[1];
            outLinks[0] = parameter.substring(parameter.lastIndexOf("leecher.ws/out/") + 15);
        } else {
            br.getPage(parameter);
            outLinks = br.getRegex("href=\"http://www\\.leecher\\.ws/out/(.*?)\"").getColumn(0);
        }

        progress.setRange(outLinks.length);
        for (String element : outLinks) {
            br.getPage("http://leecher.ws/out/" + element);
            String cryptedLink = br.getRegex("<iframe src=\"(.?)\"").getMatch(0);
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(cryptedLink)));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}