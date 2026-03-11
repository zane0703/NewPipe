package org.zane.newpipe;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.SwingUtilities;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InfoItem.InfoType;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.youtube.ItagItem.ItagType;

public class SearchResultPage extends JPanel {

    public SearchResultPage(App app) {
        super(new java.awt.GridBagLayout());
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
                SwingUtilities.invokeLater(() -> {});
                GridBagConstraints c = new GridBagConstraints();
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 0;
                c.weighty = 1;
                c.gridy = 0;
                for (int i = 0; i < items.size(); ++i) {
                    InfoItem item = items.get(i);
                    JPanel searchResultItem = new JPanel(
                        new FlowLayout(FlowLayout.LEFT)
                    );

                    c.gridy = i;
                    this.add(searchResultItem, c);
                    BufferedImage image = ImageIO.read(
                        new URI(item.getThumbnails().get(0).getUrl()).toURL()
                    );
                    if (image != null) {
                        ImageIcon icon = new ImageIcon(image);
                        // Create a JLabel to hold the icon
                        JLabel thumbnaillabel = new JLabel(icon);
                        thumbnaillabel.setSize(new Dimension(10, 10));
                        SwingUtilities.invokeLater(() ->
                            searchResultItem.add(thumbnaillabel)
                        );
                    }
                    JPanel infoPanel = new JPanel(new GridLayout(2, 1));
                    JLabel itemTitle = new JLabel();
                    itemTitle.setText(item.getName());
                    searchResultItem.addMouseListener(
                        new PanelClickListener(item)
                    );
                    infoPanel.add(itemTitle);
                    SwingUtilities.invokeLater(() ->
                        searchResultItem.add(infoPanel)
                    );
                }
                app.pack();
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

    private class PanelClickListener implements MouseListener {

        private final InfoItem infoItem;

        public PanelClickListener(InfoItem infoItem) {
            this.infoItem = infoItem;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            InfoType.STREAM;
            switch (infoItem.getInfoType()) {
                case STREAM:
                    app.nevigate(App.Page.VIDEO, infoItem.getUrl());
                    break;
            }
        }

        // Other MouseListener methods (must be implemented, even if empty)
        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
    }
}
