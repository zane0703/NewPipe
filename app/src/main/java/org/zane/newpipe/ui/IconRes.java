package org.zane.newpipe.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.IOException;

public class IconRes {

    public static FlatSVGIcon ARROW_BACK_ICON;
    public static FlatSVGIcon ARROW_NEXT_ICON;
    public static FlatSVGIcon ART_TRACK_ICON;
    public static FlatSVGIcon COMMENT_ICON;
    public static FlatSVGIcon DESCRIPTION_ICON;
    public static FlatSVGIcon HEART_ICON;
    public static FlatSVGIcon PAUSE_ICON;
    public static FlatSVGIcon PLAY_ARROW_ICON;
    public static FlatSVGIcon SEARCH_ICON;
    public static FlatSVGIcon THUMP_UP_ICON;
    public static FlatSVGIcon THUMP_UP_SMALL_ICON;
    public static FlatSVGIcon LANGUAGE_ICON;
    public static FlatSVGIcon VLC_ICON;
    public static FlatSVGIcon COPY_ICON;
    public static FlatSVGIcon LIVE_TV_ICON;
    public static FlatSVGIcon DOWNLOAD_ICON;

    static {
        try {
            ARROW_BACK_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_arrow_back.svg")
            );
            ARROW_NEXT_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_arrow_next.svg")
            );
            COMMENT_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_comment.svg")
            );
            ART_TRACK_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_art_track.svg")
            );
            DESCRIPTION_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_description.svg")
            );
            HEART_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_heart.svg")
            ).derive(15, 15);
            PAUSE_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_pause.svg")
            );
            PLAY_ARROW_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_play_arrow.svg")
            );
            SEARCH_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_search.svg")
            );
            THUMP_UP_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_thumb_up.svg")
            );
            THUMP_UP_SMALL_ICON = THUMP_UP_ICON.derive(15, 15);
            VLC_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream(
                    "/icon/vlc-logo-player-svgrepo-com.svg"
                )
            ).derive(20, 20);
            LANGUAGE_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_language.svg")
            ).derive(20, 20);
            COPY_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/copy-svgrepo-com.svg")
            ).derive(20, 20);
            LIVE_TV_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_live_tv.svg")
            ).derive(20, 20);
            DOWNLOAD_ICON = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_file_download.svg")
            ).derive(20, 20);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
