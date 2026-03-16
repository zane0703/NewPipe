package org.zane.newpipe.page;

import java.awt.BorderLayout;
import java.util.Stack;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

public class MainViewPort extends JViewport {

    private ChannelPage channelPage;
    private SearchResultPage searchResultPage;
    private VideoPage videoPage;
    private NevigateOpation currentPage;
    private Stack<NevigateOpation> nevigateHistory;
    private SetEnabledBtn setBackEnable;
    private SetEnabledBtn setSearchEnable;

    public MainViewPort(
        SetEnabledBtn setBackEnable,
        SetEnabledBtn setSearchEnable
    ) {
        this.setBackEnable = setBackEnable;
        this.setSearchEnable = setSearchEnable;
        channelPage = new ChannelPage(this);
        searchResultPage = new SearchResultPage(this);
        videoPage = new VideoPage(this);
        nevigateHistory = new Stack<>();
        JPanel defultPage = new JPanel(new BorderLayout());
        defultPage.add(
            new JLabel("Try searching to get started", SwingConstants.CENTER),
            BorderLayout.CENTER
        );
        this.setView(defultPage);
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
        if (isNotFirst) {
            switch (currentPage.PAGE) {
                case VIDEO:
                    videoPage.stop();
            }
        }
        SwitchView(nevigateOpation);
        if (isNotFirst) {
            nevigateHistory.add(currentPage);
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
                break;
            case VIDEO:
                this.setView(videoPage);
                videoPage.showVideo(nevigateOpation.QUERY);
                //this.pack();
                break;
            case CHANNEL:
                this.setView(channelPage);
                channelPage.fetchChannel(nevigateOpation.QUERY);
        }
    }

    public static enum Page {
        SEARCH,
        VIDEO,
        CHANNEL,
    }

    public static interface SetEnabledBtn {
        public void setEnabled(boolean enable);
    }
}
