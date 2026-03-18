package org.zane.newpipe.page;

import java.awt.BorderLayout;
import java.util.ArrayDeque;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

public class MainViewPort extends JViewport {

    private ChannelPage channelPage;
    private SearchResultPage searchResultPage;
    private VideoPage videoPage;
    private NevigateOpation currentPage;
    private ArrayDeque<NevigateOpation> nevigateHistory;
    private SetEnabledBtn setBackEnable;
    private SetEnabledBtn setSearchEnable;
    private SetText setSearchText;

    public MainViewPort(
        SetEnabledBtn setBackEnable,
        SetEnabledBtn setSearchEnable,
        SetText setSearchText,
        boolean showDefault
    ) {
        this.setBackEnable = setBackEnable;
        this.setSearchEnable = setSearchEnable;
        this.setSearchText = setSearchText;
        channelPage = new ChannelPage(this);
        searchResultPage = new SearchResultPage(this);
        videoPage = new VideoPage(this);
        nevigateHistory = new ArrayDeque<>();
        if (showDefault) {
            JPanel defultPage = new JPanel(new BorderLayout());
            defultPage.add(
                new JLabel(
                    "Try searching to get started",
                    SwingConstants.CENTER
                ),
                BorderLayout.CENTER
            );
            this.setView(defultPage);
        }
    }

    public static class NevigateOpation {

        public final Page PAGE;
        public final String QUERY;

        public NevigateOpation(Page page, String query) {
            this.PAGE = page;
            this.QUERY = query;
        }
    }

    public void nevigate(NevigateOpation nevigateOpation) {
        boolean isNotFirst = currentPage != null;
        setBackEnable.setEnabled(false);
        if (isNotFirst) {
            switch (currentPage.PAGE) {
                case VIDEO:
                    videoPage.stop();
            }
        }
        SwitchView(nevigateOpation);
        if (isNotFirst) {
            nevigateHistory.push(currentPage);
        }
        currentPage = nevigateOpation;
        setBackEnable.setEnabled(isNotFirst);
    }

    public void back() {
        switch (currentPage.PAGE) {
            case VIDEO:
                videoPage.stop();
        }
        NevigateOpation prePage = nevigateHistory.pop();
        setBackEnable.setEnabled(!nevigateHistory.isEmpty());
        SwitchView(prePage);
        currentPage = prePage;
    }

    private void SwitchView(NevigateOpation nevigateOpation) {
        switch (nevigateOpation.PAGE) {
            case SEARCH:
                setSearchEnable.setEnabled(false);
                searchResultPage.search(nevigateOpation.QUERY, () -> {
                    setSearchEnable.setEnabled(true);
                });
                this.setView(searchResultPage);
                setSearchText.setText(nevigateOpation.QUERY);
                break;
            case VIDEO:
                this.setView(videoPage);
                videoPage.showVideo(nevigateOpation.QUERY);
                //this.pack();
                break;
            case CHANNEL:
                this.setView(channelPage);
                channelPage.fetchChannel(nevigateOpation.QUERY);
                break;
            case PLAYLIST:
                PlayListPage playListPage = new PlayListPage(this);
                this.setView(playListPage);
                playListPage.fatchPlayList(nevigateOpation.QUERY);
        }
    }

    public static enum Page {
        SEARCH,
        VIDEO,
        CHANNEL,
        PLAYLIST,
    }

    public static interface SetEnabledBtn {
        public void setEnabled(boolean enable);
    }

    public static interface SetText {
        public void setText(String text);
    }
}
