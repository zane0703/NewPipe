package org.zane.newpipe;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.swing.*;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;

public class SearchResultPage extends JPanel {

    public SearchResultPage(App app) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.app = app;
    }

    private final App app;

    public void search(String query, Runnable finaly) {
        this.removeAll();
        new Thread(() -> {
            try {
                SearchExtractor se = ServiceList.YouTube.getSearchExtractor(
                    query
                );
                se.fetchPage();
                InfoItemsPage<InfoItem> itp = se.getInitialPage();
                List<InfoItem> items = itp.getItems();
                Color transparent = new Color(0, 0, 0, 0);
                for (int i = 0; i < items.size(); ++i) {
                    InfoItem item = items.get(i);
                    SearchItemPanel searchItemPanel = new SearchItemPanel(
                        app,
                        item
                    );
                    SwingUtilities.invokeLater(() -> this.add(searchItemPanel));
                }

                SwingUtilities.invokeLater(this::updateUI);
            } catch (
                IOException
                | ExtractionException
                | URISyntaxException err
            ) {
                err.printStackTrace();
            } finally {
                finaly.run();
            }
        })
            .start();
    }
}
