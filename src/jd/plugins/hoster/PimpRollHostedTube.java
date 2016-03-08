//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 * hostedtube template by pimproll. These guys have various scripts. This is the most common.
 *
 * @see http://www.hostedtube.com/
 * @see http://www.pimproll.com/support.html
 * @linked to wankz.com which still shows "Protranstech BV. DBA NetPass, Postbus 218. ljmudien, 1970AE, Netherlands" in footer message.
 * @linked to porn.com simliar script, but not hosting any image data on hostedtube, and uses old url structure.
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {}, flags = {})
public class PimpRollHostedTube extends PluginForHost {

    public static String[] t = { "2chicks.com", "2hd.com", "3danimesluts.com", "3dvideospornos.com", "4tube.tv", "18xr.com", "20xxxrealitysites.com", "22porn.com", "24x7gays.com", "69hornytube.com", "69xxx.net", "70porn.com", "91porn.us", "110percentamateur.com", "420camgirls.com", "adult-lock.com", "adultanimemovies.com", "adultchoice.com", "adultepass.com", "adulteroticcartoon.net", "adultfilmconnection.com", "adultfilmflux.com", "adultvideoarcade.com", "adultxxx.xxx", "africanfucktour.org", "africansexgirlfriend.com", "agirls.com", "allasiantgp.com", "allhardcoretgp.com", "amateurcat.com", "amatuerteentube.com", "amazingpornsearch.com", "americanporntube.net", "analcomics.com", "angkorporn.com", "animeporntubehd.com", "apetube.eu", "arsqualita.com", "asian-porn.xxx", "asianbdsmtube.com", "asianlesbianlovers.com", "asianporn.me", "asianporn.mobi", "asiansitepass.com",
            "asiantranny.xxx", "assfreeass.com", "australiaporn4all.com", "babes.xxx", "bangsisters.com", "bbcgirls.com", "bedroomgraphics.com", "beeg.be", "beegltd.com", "belasnegras.com", "bellaporn.com", "bestporno.net", "bestporntube.com", "bigfullmovies.com", "bigpenises.com", "bigtitcomics.com", "bigtitsmaid.com", "bigwetcunts.com", "bindysbabes.com", "bisexualpornhdtube.com", "bisexualpov.com", "bisexualstory.com", "bisexualvideotube.com", "bizarreporntube.com", "bizarrevidz.com", "bizarrre.com", "bizzarevideotube.com", "blackafricanass.com", "blackhoesporn.com", "blackknockers.com", "blackpussyporn.net", "blacksextube.net", "blacktube.xxx", "blacktubevideos.net", "blackwhores.xxx", "blowjobpractice.com", "bobogrey.com", "bondagefortress.com", "bondagevideos.xxx", "boobworx.com", "bootyspank.com", "bpbr.net", "brasileiro.xxx", "brazzerscasting.com", "brutalxxxporn.com",
            "bubblebuttboy.com", "bubblebuttboys.com", "buddies.xxx", "buddy.xxx", "bukakebath.com", "bukkakecumshots.com", "bust4.com", "buttbashers.com", "cambodialover.com", "cambodiax.com", "capitalpork.com", "cartoonpornpass.com", "cartoonsmaster.com", "celeb-sex-videos.com", "celebpornpass.com", "cfnmhdtube.com", "cfnmthings.com", "chocolatesistaz.com", "chumleaf.info", "churchofsexology.com", "cloudhentai.com", "cluborgasm.com", "clubsixtynine.com", "cockhut.com", "cocksucker.co.uk", "condomfree.xxx", "contentadult.com", "cougarfuckclub.com", "cougarx.com", "creampiewomen.com", "cuckoldtubevids.com", "cumseed.com", "daddylongtime.com", "dailyhardcorestories.com", "deluxelist.com", "descargarpelisporno.com", "describe.xxx", "dialastud.com", "digimonyaoi.com", "dirtymovie.com", "dirtyweeman.com", "djwhores.com", "dominatrixescorts.com", "download-free-live-sex-cams.com",
            "download-free-sex-chat.com", "drfetish.org", "drtubermovies.com", "dumpporn.com", "ebonylesbianslove.com", "ebonytubehd.com", "ebonytubemovies.com", "ellcanior.com", "enemapov.com", "erectionisland.com", "errvids.com", "evilhub.com", "ex-bfs.com", "exclusivelesbians.com", "explicit.com", "extremedr.com", "fapple.com", "farkingridiculous.com", "farmerotica.com", "feetpornvideos.com", "filmepornogratis.us", "firsttimeanal.org", "fistfullofcum.com", "fraspi.com", "freakofporn.com", "free-porn-videos.co", "free-tube-porn.com", "free-x.xxx", "free6.mobi", "free6clips.com", "freeasianvideos.net", "freeboobtube.com", "freecartoonpornhdtube.com", "freedownloadpornvideo.com", "freefuckvidz.xxx", "freegaypornhdtube.com", "freelesbianpornhdtube.com", "freemobilevidz.com", "freepornfriend.com", "freepornonline.info", "freepornproject.com", "freepornvideos.name", "freepornwhiz.com",
            "freeprontube.net", "freesex.info", "freesex22.com", "freesexint.com", "freesexmembership.net", "freesexonvideo.com", "freesexpeeps.com", "freesexporn.xxx", "freesextube.xxx", "freesexvidio.us", "freesmutclub.com", "freetubes.xxx", "freetubexxl.com", "freexxx.org", "freexxxx.xxx", "freshpornpic.com", "fuck-tube.org", "fuck13.com", "fucks.se", "fukpics.com", "full-porno.com", "funsexteen.com", "funsexworld.com", "galaxysex.com", "galorehub.com", "gay-porn.xxx", "gay-xxx.xxx", "gayasslickingvideos.com", "gaycollegesex.com", "gayflicks.com", "gayinteracial.com", "gaymoz.com", "gayrotica.com", "gaytubehub.com", "germanyfreeporn.com", "getmywomen.com", "gigatube.com", "girlnextwhore.com", "girlsmasterbating.com", "girlstube.net", "gnomeporn.com", "gonzoglamour.com", "grannypornstar.com", "gratisporrfilm.nu", "greatporndownloads.com", "hankyporn.com", "hardcorelezbos.com",
            "hardcoretwinks.com", "hctube.com", "hd-movies.info", "hd-porntv.net", "hd-sextv.net", "hentai-files.com", "hentaianimehd.com", "hentaionline.com", "hentaiporno.us", "hentaiporntube.org", "hentaisexvideos.pw", "hidefporn.org", "highpoweredpussy.com", "hismut.com", "homotube.nl", "hornyslutmoms.com", "hot-porn.us", "hotarabgirl.com", "hotcamgirls.net", "hoteighteen.com", "hothsexd.com", "hotliveaction.com", "hotmalegaytube.com", "hottube.xxx", "hub4.tv", "hubdr.com", "hublube.com", "hugecockpornpass.com", "hugedickpornpass.com", "ifrancefreeporn.com", "ifuck.se", "iizzi.com", "ilesbain.com", "iloveporno.com", "impregnation.net", "impulsive.xxx", "indiafreeporntube.com", "indian-sex.xxx", "internetvideounravelled.com", "interracialcomix.com", "ismytube.biz", "itube.com", "japenxxx.com", "jasonoakley.me", "jenavevejolie.com", "jerk4.com", "jessiam.com", "jizjob.com",
            "jizz-on.com", "jizzdr.com", "jizzglazed.com", "jizzjobs.com", "jizzsoup.com", "joporn.com", "juicypussytube.com", "justaveragesluts.com", "kalyhot.com", "khmer69.com", "khmer111.com", "khmersextube.com", "knepper.nu", "kudtube.com", "ladysnatch.com", "lay.xxx", "lbosexvideos.com", "leahremininude.com", "lesbian-sex.xxx", "lesbianpussy.org", "lesbiansweb.com", "lesbianxxxvideo.com", "lethalmobile.com", "letsbangthebabysitter.com", "likepussytube.com", "littlesexthings.com", "livesexhq.com", "loadedbox.com", "lolylporno.com", "lopussy.com", "loveinjection.com", "lubebucket.com", "makingaporno.com", "maleorgy.xxx", "malepornxxx.com", "mamacitavideos.com", "mangaporno.us", "maturehot.com", "maturesexfreaks.com", "maximumpussy.com", "maximumsexxx.com", "mediastoragemadeeasy.com", "menandmen.com", "milfboard.com", "milfphoneporn.com", "milfsexmovies.net", "milfsporn.net",
            "milftuba.com", "milftube4u.com", "milftubeporn.com", "milfucker.com", "milfvideos.eu", "milfvideotube.com", "misiko.com", "mobile-hentai-tube.com", "mobileanimepass.com", "mobilecartoontube.com", "mobilemilfpass.com", "mofoscasting.com", "mofoshub.com", "momsfuckteens.com", "momspimptheirdaughter.com", "momvids.com", "most-porn.com", "mostporno.com", "motherporno.com", "movies-portal.com", "moviesex.xxx", "moviesexcams.com", "moviesxxx.xxx", "mphoneporn.com", "mr-xxx.com", "mupp.se", "mys.xxx", "myxxxplay.com", "n-u-d-e.com", "nakedmovienews.com", "nashvilleporn.com", "nastyblog.com", "nastyhub.com", "nastyporntube.net", "naughty4.com", "naughty8.com", "nausty.com", "ninjaxxx.com", "nobotv.com", "nudenakedindianwomen.com", "nutten.ru", "nxsnacksvip.com", "ogrishvideos.com", "ogrishxxx.com", "ohsluts.com", "onlypornclips.com", "orientalpornsearch.com", "paris-porn.net",
            "parishilten.com", "pcgn.com", "penisaurus.com", "perfectplatinum.com", "persiankittyvidz.com", "pigyporn.com", "pimptrailers.com", "pimpvod.com", "pinksvisual.com", "pipeporno.com", "playwithmyforeskin.com", "pleasure.net", "pleasureporn.net", "plexiglas.name", "plumppass.com", "porm.com", "porn-18.xxx", "porn-rabbit.com", "porn-tube.com", "porn69tube.com", "pornadult.xxx", "porndada.com", "porndl.com", "porneez.com", "pornfeed.com", "porngayhd.com", "porngoldmine.com", "porninfo.com", "pornmegaplex.com", "pornmilf.xxx", "pornoactual.com", "pornoextra.com", "pornohoes.com", "pornostartryouts.com", "pornotube.xxx", "pornotube21.com", "pornpalazzo.com", "pornpeer.com", "pornporky.com", "pornrss.com", "pornsexlinks.com", "pornsitepros.com", "pornstargalore.net", "pornstarpassion.com", "pornstarsawards.com", "porntoonz.com", "porntube10.com", "pornvideon.com",
            "pornvideos10.com", "pornvie.com", "porrfilmer.biz", "porrfilmer.mobi", "porrfilmgratis.se", "povanalsluts.com", "primalsexvideos.com", "privatepornlinks.com", "pron.co", "pronhubltd.com", "pussyblog.com", "pussyfights.com", "pussyof.com", "qualitypink.com", "r89.com", "ratedpornpremium.com", "ratedsexy.com", "ratedx.eu", "ratedxmoney.com", "ratepenis.com", "realitylist.com", "realitystation.com", "reallywildteens.com", "redgayporntube.com", "redhotdudes.com", "redtube-com.info", "redtubedb.com", "redzonetube.com", "reet.com", "rubguide.com", "rudeboobies.com", "safesextube.com", "sarahpalinsextape.org", "screwmyspouse.com", "screwshack.com", "sergeantstiffy.com", "sessovideo.us", "sex-cake.com", "sex-film.se", "sex-porno-pics.com", "sex-videos.se", "sex.biz", "sexadult.xxx", "sexbyfood.com", "sexclicks.com", "sexdgbbw.com", "sexernet.com", "sexfilmer.nu", "sexfilmer.se",
            "sexfilmsxxx.us", "sexfindout.com", "sexfreevideos.xxx", "sexgratis.se", "sexgratisxxx.us", "sexhamster.co", "sexkate.com", "sexlifetube.com", "sexmalestubes.com", "sexmom.org", "sexmovielibrary.com", "sexmovieporn.com", "sexmovieshd.com", "sexmoviesxxx.com", "sexmum.com", "sexmys.com", "sexostube.com", "sexparty.tv", "sexpc.com", "sexroots.com", "sexstarclub.com", "sextapexxx.com", "sexthings.com", "sextube-6.com", "sextubestv.com", "sextvnews.com", "sexualexploits.com", "sexualnews.com", "sexualurges.net", "sexvidmovs.com", "sexviedos.com", "sexwebs.com", "sexxxx.xxx", "sexyandold.com", "sexyanimevideos.com", "sexyboytube.com", "sexyenema.com", "sexyrhino.com", "sexytubeclip.com", "shafty.com", "shaftytube.com", "sharmota.com", "simplybabes.com", "sinfultoons.com", "skankymoms.com", "skullfucktube.com", "slutloads.us", "smalldickbanger.com", "smalldickfucker.com",
            "smalldickfuckers.com", "smutboss.com", "smuthouse.com", "smutshelf.com", "soyouthinkyoucanscrew.com", "spankingmywife.com", "spanktank.xxx", "strapon.se", "straponlesbians.us", "stream-online-porn.com", "sugarmanga.com", "super-porno.com", "superpene.tv", "superxvideos.net", "swedexxx.com", "sweetchocolatepussy.com", "swesex.se", "swingersex.ca", "swingersmag.com", "swingerstube.net", "swingersx.com", "syntheticheroin.com", "tastyblackpussy.com", "teabaggingpov.com", "teamforbidden.com", "teenbdtube.com", "teenporn2u.info", "teenpornocity.com", "teensteam.com", "teenvideos.com", "teenvirgin.org", "thefreeadult.com", "thefreeasian.com", "thefreepornotube.com", "thefreepornreport.com", "thehotbabes.net", "themeatmen.com", "themostgay.com", "thepornlegacy.com", "thesexlotto.com", "thestagparty.com", "thongtubes.com", "throatpokers.com", "thumblordstube.com", "ticklingpov.com",
            "tnaflix.eu", "toonfucksluts.com", "toonsporno.com", "topfemales.com", "topsecretporn.net", "totalfetish.com", "totallytastelessvideos.com", "traileraddicts.com", "tramplepov.com", "trashymoms.com", "tube4jizz.com", "tube75.com", "tubegalorexxx.com", "tubesmack.com", "tubewetlook.com", "ultimatestripoff.com", "ultporn.com", "ultramoviemadness.com", "ultvid.com", "uncensoredtoons.com", "undercoverpussy.com", "urbansexvideos.com", "usagirlfriends.com", "vid.xxx", "videez.com", "video-porn.us", "video18.com", "videofetish.com", "videolivesex.com", "videosp.com", "videoxxxltd.com", "vids.xxx", "vidsvidsvids.com", "vidxpose.com", "vidz.info", "vidzmobile.com", "vietnamsextube.com", "vip-babes-world.com", "vodporn.eu", "voyeurxxxtube.com", "vudutube.com", "wankbucket.com", "watchbooty.com", "watchpornvideos.com", "watchsex.net", "wetandsticky.com", "whackov.com", "whoores.com",
            "whorebrowser.com", "wltube.com", "worldsbestpornmovies.com", "x-movies.xxx", "x-video.xxx", "xblacktube.com", "xcon3.com", "xfree.xxx", "xhamsterporno.com", "xmovielove.com", "xnxxltd.com", "xnxxx.eu", "xnxxxltd.com", "xpoko.com", "xpornografia.us", "xpornohd.com", "xporntube.us", "xsex.xxx", "xtube.mobi", "xtubegaysex.com", "xxnxltd.com", "xxx-18.xxx", "xxx-con.com", "xxxbfs.com", "xxxcartoonz.com", "xxxcebu.com", "xxxcreampie.org", "xxxdn.com", "xxxfilms.xxx", "xxxgaytube.com", "xxxhardcore.us", "xxxhardcorexxx.com", "xxxhelp.com", "xxxin3d.com", "xxxlesbiansexvideos.org", "xxxlinkshunter.com", "xxxmobiletubes.com", "xxxpornvideos.us", "xxxpornx.xxx", "xxxpromos.com", "xxxslutty.com", "xxxthailand.net", "xxxvidz.org", "xxxxn.com", "yankjobs.com", "youjizz.net", "youjizz66.com", "youjizzltd.com", "youlorn.com", "youngxxxx.com", "youpronltd.com", "yourcocktube.com",
            "yourfreepornsex.com", "yoursmutpass.com", "yozjizz.com", "yufap.com", "zadmo.com", "zippyporn.com", "zmaster.com", "wankz.com" };

    /**
     * Returns the annotations names array
     *
     * @return
     */
    public static String[] getAnnotationNames() {
        return t;
    }

    /**
     * returns the annotation pattern array
     *
     * @return
     */
    public static String[] getAnnotationUrls() {
        String[] s = new String[t.length];
        for (int ssize = 0; ssize != t.length; ssize++) {
            s[ssize] = constructUrl(t[ssize]);
        }
        s[s.length - 1] = "http://(?:www\\.)?wankz\\.com/(?:[\\w\\-]+|embed/)\\d+";
        return s;
    }

    /**
     * Returns the annotations flags array
     *
     * @return
     */
    public static int[] getAnnotationFlags() {
        int[] s = new int[t.length];
        for (int ssize = 0; ssize != t.length; ssize++) {
            s[ssize] = 0;
        }
        return s;
    }

    public static String constructUrl(final String host) {
        return "http://(?:(?:www|m)\\.)?" + Pattern.quote(host) + "/(?![^/]*(?:models|stars|categories|channels|tags)[^/]*)[\\w\\-]+/\\d+";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getDownloadURL().contains("wankz.com/")) {
            link.setUrlDownload(link.getDownloadURL().replace("/embed/", "/"));
        }
    }

    public PimpRollHostedTube(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HTML_PREMIUMONLY = "<h3>Access Denied</h3>";

    private String              DLLINK           = null;

    @Override
    public String getAGBLink() {
        return "";
    }

    private static AtomicReference<String> agent = new AtomicReference<String>();

    /**
     * defines custom browser requirements.
     */
    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setFollowRedirects(true);
        /* http://www.blackwhores.xxx/videos/1255 */
        prepBr.setAllowedResponseCodes(410);
        return prepBr;
    }

    public String extFromLink(String ext) {
        // Trimming query_string
        int extEnd = ext.indexOf("?");
        if (extEnd > 0) {
            ext = ext.substring(0, extEnd);
        }
        // Trimming fragment_id
        extEnd = ext.indexOf("#");
        if (extEnd > 0) {
            ext = ext.substring(0, extEnd);
        }
        // Trimming main part
        ext = ext.substring(ext.lastIndexOf("."));
        // Trimming whitespace
        ext = ext.trim();
        // Checking resulting length
        int resultLength = ext.length();
        if (resultLength < 2 || resultLength > 5) {
            ext = null;
        }
        return ext;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.getPage(downloadLink.getDownloadURL());
        // 404 on desktop page
        if (br.containsHTML("was not found on this server, please try a") || br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // 404 on mobile page
        if (br.containsHTML("<a href=\"#sorting\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1[^>]*>([^<>]*?)</h1>").getMatch(0);
        }
        // DLLINK on desktop page
        final String[] qualities = { "1080p", "720p", "480p", "360p", "240p" };
        for (final String quality : qualities) {
            DLLINK = br.getRegex("\"" + quality + "\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) {
                /* In most cases e.g.: "name":"480p" */
                DLLINK = br.getRegex("\"[a-z]+\":\"" + quality + "\",\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            }
            if (DLLINK != null) {
                break;
            }
        }
        // DLLINK on mobile page
        if (DLLINK == null) {
            DLLINK = br.getRegex("\"(http://[a-z0-9\\-\\.]+movies\\.hostedtube\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null || DLLINK == null) {
            // premium only content.
            if (br.containsHTML(HTML_PREMIUMONLY)) {
                // we can set filename
                if (filename != null) {
                    downloadLink.setName(Encoding.htmlDecode(filename) + ".mp4");
                }
                return AvailableStatus.TRUE;
            } else if (!br.containsHTML("flowplayer\\.pseudostreaming")) {
                /* No video content available */
                /* http://www.110percentamateur.com/horny-amateur-sluts-fucking/49459 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        DLLINK = Encoding.htmlDecode(DLLINK);
        filename = filename.trim();
        String ext = extFromLink(DLLINK);
        if (ext == null) {
            ext = ".mp4";
        }
        while (filename.endsWith(".")) {
            filename = filename.substring(0, filename.length() - 1);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (DLLINK == null && br.containsHTML(HTML_PREMIUMONLY)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
