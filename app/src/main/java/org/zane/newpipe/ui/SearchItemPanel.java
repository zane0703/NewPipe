package org.zane.newpipe.ui;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NevigateOpation;
import org.zane.newpipe.util.CommonUtil;

public class SearchItemPanel extends JPanel {

    private final MainViewPort mainViewPort;

    public SearchItemPanel(MainViewPort mainViewPort, InfoItem item)
        throws IOException, URISyntaxException {
        super(new FlowLayout(FlowLayout.LEFT));
        this.mainViewPort = mainViewPort;

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        BufferedImage image = ImageIO.read(
            new URI(item.getThumbnails().get(0).getUrl()).toURL()
        );
        //JPanel t = new JPanel(new SpringLayout());
        JImage thumbnaillabel = new JImage(image);
        thumbnaillabel.setMaximumSize(new Dimension(200, 200));
        thumbnaillabel.repaint();
        //t.add(thumbnaillabel);
        SwingUtilities.invokeLater(() -> this.add(thumbnaillabel));
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        JLabel itemTitle = new JLabel();
        Font currentFont = itemTitle.getFont();
        itemTitle.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        infoPanel.setBackground(new Color(0, 0, 0, 0));
        infoPanel.setOpaque(false);
        itemTitle.setText(item.getName());
        infoPanel.add(itemTitle);
        if (item instanceof StreamInfoItem streamInfoItem) {
            JLabel uploaderLabel = new JLabel(streamInfoItem.getUploaderName());
            uploaderLabel.setForeground(Color.LIGHT_GRAY);
            infoPanel.add(uploaderLabel);
            String viewLabelString =
                CommonUtil.numberToStringUnit(streamInfoItem.getViewCount()) +
                " views";

            DateWrapper uploadDate = streamInfoItem.getUploadDate();
            if (uploadDate != null) {
                viewLabelString +=
                    "· " +
                    CommonUtil.formatRelativeTime(
                        uploadDate.getLocalDateTime()
                    );
            }
            JLabel viewLabel = new JLabel(viewLabelString);

            viewLabel.setForeground(Color.LIGHT_GRAY);
            infoPanel.add(viewLabel);
        }
        this.addMouseListener(new PanelClickListener(item));

        SwingUtilities.invokeLater(() -> this.add(infoPanel));
    }

    private class PanelClickListener implements MouseListener {

        private final InfoItem infoItem;

        public PanelClickListener(InfoItem infoItem) {
            this.infoItem = infoItem;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            MainViewPort.Page newPage;
            switch (infoItem.getInfoType()) {
                case STREAM:
                    newPage = MainViewPort.Page.VIDEO;
                    break;
                case CHANNEL:
                    newPage = MainViewPort.Page.CHANNEL;
                    break;
                default:
                    return;
            }
            mainViewPort.nevigate(
                new NevigateOpation(newPage, infoItem.getUrl())
            );
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
