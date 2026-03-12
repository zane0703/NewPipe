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
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

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
                    JPanel searchResultItem = new JPanel(
                        new FlowLayout(FlowLayout.LEFT)
                    );

                    searchResultItem.setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    );
                    this.add(searchResultItem);
                    BufferedImage image = ImageIO.read(
                        new URI(item.getThumbnails().get(0).getUrl()).toURL()
                    );
                    //JPanel t = new JPanel(new SpringLayout());
                    JImage thumbnaillabel = new JImage(image);
                    thumbnaillabel.setMaximumSize(new Dimension(500, 500));
                    thumbnaillabel.repaint();
                    //t.add(thumbnaillabel);
                    SwingUtilities.invokeLater(() ->
                        searchResultItem.add(thumbnaillabel)
                    );
                    JPanel infoPanel = new JPanel(new GridLayout(2, 1));
                    JLabel itemTitle = new JLabel();
                    infoPanel.setBackground(transparent);
                    infoPanel.setOpaque(false);
                    itemTitle.setText(item.getName());
                    infoPanel.add(itemTitle);
                    if (item instanceof StreamInfoItem streamInfoItem) {
                        JLabel uploaderLabel = new JLabel(
                            streamInfoItem.getUploaderName()
                        );

                        uploaderLabel.setForeground(Color.LIGHT_GRAY);
                        infoPanel.add(uploaderLabel);
                    }
                    searchResultItem.addMouseListener(
                        new PanelClickListener(item)
                    );

                    SwingUtilities.invokeLater(() ->
                        searchResultItem.add(infoPanel)
                    );
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

    private class PanelClickListener implements MouseListener {

        private final InfoItem infoItem;

        public PanelClickListener(InfoItem infoItem) {
            this.infoItem = infoItem;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            switch (infoItem.getInfoType()) {
                case STREAM:
                    app.nevigate(App.Page.VIDEO, infoItem.getUrl());
                    break;
                case CHANNEL:
                    app.nevigate(App.Page.CHANNEL, infoItem.getUrl());
                    break;
            }
        }

        // Other MouseListener methods (must be implemented, even if empty)
        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {
            e.getComponent().setBackground(Color.DARK_GRAY);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            e.getComponent().setBackground(Color.BLACK);
        }
    }
}
