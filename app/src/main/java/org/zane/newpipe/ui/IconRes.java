package org.zane.newpipe.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.IOException;

public class IconRes {

    public static FlatSVGIcon ARROW_BACK_ICOM;
    public static FlatSVGIcon ARROW_NEXT_ICOM;
    public static FlatSVGIcon ART_TRACK_ICOM;
    public static FlatSVGIcon COMMENT_ICOM;
    public static FlatSVGIcon DESCRIPTION_ICOM;
    public static FlatSVGIcon HEART_ICOM;
    public static FlatSVGIcon PAUSE_ICOM;
    public static FlatSVGIcon PLAY_ARROW_ICOM;
    public static FlatSVGIcon SEARCH_ICOM;
    public static FlatSVGIcon THUMP_UP_ICOM;
    public static FlatSVGIcon THUMP_UP_SMALL_ICOM;

    static {
        try {
            ARROW_BACK_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_arrow_back.svg")
            );
            ARROW_NEXT_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_arrow_next.svg")
            );
            COMMENT_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_comment.svg")
            );
            ART_TRACK_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_art_track.svg")
            );
            DESCRIPTION_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_description.svg")
            );
            HEART_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_heart.svg")
            ).derive(15, 15);
            PAUSE_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_pause.svg")
            );
            PLAY_ARROW_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_play_arrow.svg")
            );
            SEARCH_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_search.svg")
            );
            THUMP_UP_ICOM = new FlatSVGIcon(
                IconRes.class.getResourceAsStream("/icon/ic_thumb_up.svg")
            );
            THUMP_UP_SMALL_ICOM = THUMP_UP_ICOM.derive(15, 15);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
