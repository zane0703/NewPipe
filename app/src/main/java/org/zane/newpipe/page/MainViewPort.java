package org.zane.newpipe.page;

import java.awt.BorderLayout;
import java.util.ArrayDeque;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import org.zane.newpipe.App;

public class MainViewPort extends JViewport {

    private ChannelPage channelPage;
    private SearchResultPage searchResultPage;
    private VideoPage videoPage;
    private NavigateOption currentPage;
    private ArrayDeque<NavigateOption> nevigateHistory;
    private SetEnabledBtn setBackEnable;
    private SetEnabledBtn setSearchEnable;
    private SetText setSearchText;
    private App app;

    public App getApp() {
        return app;
    }

    public MainViewPort(
        SetEnabledBtn setBackEnable,
        SetEnabledBtn setSearchEnable,
        SetText setSearchText,
        boolean showDefault,
        boolean isAutoPlay,
        App app
    ) {
        this.app = app;
        this.setBackEnable = setBackEnable;
        this.setSearchEnable = setSearchEnable;
        this.setSearchText = setSearchText;
        channelPage = new ChannelPage(this);
        searchResultPage = new SearchResultPage(this);
        videoPage = new VideoPage(this, isAutoPlay);
        nevigateHistory = new ArrayDeque<>();
        if (showDefault) {
            currentPage = new NavigateOption(Page.HOME, null);
            this.setView(new HomePage(this));
        }
    }

    public static class NavigateOption {

        public final Page PAGE;
        public final String QUERY;

        public NavigateOption(Page page, String query) {
            this.PAGE = page;
            this.QUERY = query;
        }
    }

    public void navigate(NavigateOption navigateOption) {
        boolean isNotFirst = currentPage != null;
        setBackEnable.setEnabled(false);
        if (isNotFirst) {
            switch (currentPage.PAGE) {
                case VIDEO:
                    videoPage.stop();
                    break;
                case SEARCH:
                    searchResultPage.clearResult();
                    break;
                case CHANNEL:
                    channelPage.clear();
                default:
                    break;
            }
        }
        SwitchView(navigateOption);
        if (isNotFirst) {
            nevigateHistory.push(currentPage);
        }
        currentPage = navigateOption;
        setBackEnable.setEnabled(isNotFirst);
    }

    public void back() {
        switch (currentPage.PAGE) {
            case VIDEO:
                videoPage.stop();
                break;
            case SEARCH:
                searchResultPage.clearResult();
                break;
            case CHANNEL:
                channelPage.clear();
                break;
            default:
                break;
        }
        NavigateOption prePage = nevigateHistory.pop();
        setBackEnable.setEnabled(!nevigateHistory.isEmpty());
        SwitchView(prePage);
        currentPage = prePage;
    }

    private void SwitchView(NavigateOption navigateOption) {
        switch (navigateOption.PAGE) {
            case HOME:
                this.setView(new HomePage(this));
                break;
            case SEARCH:
                setSearchEnable.setEnabled(false);
                searchResultPage.search(navigateOption.QUERY, () -> {
                    setSearchEnable.setEnabled(true);
                });
                this.setView(searchResultPage);
                setSearchText.setText(navigateOption.QUERY);
                break;
            case VIDEO:
                this.setView(videoPage);
                videoPage.showVideo(navigateOption.QUERY);
                //this.pack();
                break;
            case CHANNEL:
                this.setView(channelPage);
                channelPage.fetchChannel(navigateOption.QUERY);
                break;
            case PLAYLIST:
                PlayListPage playListPage = new PlayListPage(this);
                this.setView(playListPage);
                playListPage.fetchPlayList(navigateOption.QUERY);
        }
    }

    public static enum Page {
        HOME,
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
