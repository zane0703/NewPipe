package org.zane.newpipe.page;

import java.io.IOException;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.zane.newpipe.ui.ItemListPanel;
import org.zane.newpipe.util.CommonUtil;

public class SearchResultPage extends ItemListPanel<InfoItem> {

    private final MainViewPort mainViewPort;

    public SearchResultPage(MainViewPort mainViewPort) {
        super(mainViewPort);
        this.mainViewPort = mainViewPort;
    }

    public void search(String query, Runnable done) {
        Thread.startVirtualThread(() -> {
            try {
                SearchExtractor se = ServiceList.YouTube.getSearchExtractor(
                    query
                );
                se.fetchPage();
                setListExtractor(se);
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
                if (CommonUtil.retryPrompt(mainViewPort, "search")) {
                    search(query, done);
                }
            } finally {
                done.run();
            }
        });
    }
}
