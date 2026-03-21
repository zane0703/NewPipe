package org.zane.newpipe.ui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.concurrent.Flow;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NevigateOpation;
import org.zane.newpipe.util.CommonUtil;
import org.zane.newpipe.util.VideoUtil;

public class SearchItemPanel extends JPanel {

    private final MainViewPort mainViewPort;
    private JPopupMenu popupMenu;

    public SearchItemPanel(MainViewPort mainViewPort, InfoItem item)
        throws IOException, URISyntaxException {
        super(new FlowLayout(FlowLayout.LEFT));
        this.mainViewPort = mainViewPort;
        popupMenu = new JPopupMenu();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        BufferedImage image = ImageIO.read(
            new URI(item.getThumbnails().get(0).getUrl()).toURL()
        );
        //JPanel t = new JPanel(new SpringLayout());
        JPanel layeredPane = new JPanel();
        layeredPane.setLayout(new OverlayLayout(layeredPane));

        //layeredPane.setPreferredSize(new Dimension(200, 100));
        JImage thumbnaillabel = new JImage(image, this);
        thumbnaillabel.setMaximumSize(new Dimension(200, 200));
        if (item instanceof StreamInfoItem streamInfoItem) {
            JLabel videoTypeLabel;
            switch (streamInfoItem.getStreamType()) {
                case LIVE_STREAM:
                case AUDIO_LIVE_STREAM:
                    videoTypeLabel = new JLabel("LIVE", SwingConstants.RIGHT);
                    videoTypeLabel.setBackground(new Color(255, 0, 0, 200));
                    break;
                default:
                    videoTypeLabel = new JLabel(
                        CommonUtil.getTimeString(streamInfoItem.getDuration()),
                        SwingConstants.RIGHT
                    );
                    videoTypeLabel.setBackground(new Color(0, 0, 0, 200));
                    break;
            }
            videoTypeLabel.setBorder(new EmptyBorder(0, 0, 5, 5));
            videoTypeLabel.setOpaque(true);
            videoTypeLabel.setAlignmentX(1.0f); // Anchor to Right
            videoTypeLabel.setAlignmentY(1.0f); // Anchor to Bottom
            layeredPane.add(videoTypeLabel);
            thumbnaillabel.setAlignmentX(1.0f);
            thumbnaillabel.setAlignmentY(1.0f);
        } else if (item instanceof PlaylistInfoItem playlistInfoItem) {
            JPanel videoCountLabelPanel = new JPanel(new BorderLayout());

            videoCountLabelPanel.setOpaque(false);
            JLabel videoCountLabel = new JLabel(
                CommonUtil.numberToStringUnit(
                    playlistInfoItem.getStreamCount()
                ),
                IconRes.PLAYLIST_PLAY_ICON,
                SwingConstants.CENTER
            );
            videoCountLabel.setBackground(new Color(0, 0, 0, 200));
            videoCountLabel.setOpaque(true);
            videoCountLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
            videoCountLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
            videoCountLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            videoCountLabelPanel.setAlignmentX(1.0f);
            videoCountLabelPanel.setAlignmentY(0.0f);
            videoCountLabelPanel.add(videoCountLabel, BorderLayout.EAST);
            layeredPane.add(videoCountLabelPanel);
            thumbnaillabel.setAlignmentX(1.0f);
            thumbnaillabel.setAlignmentY(0.0f);
        }

        thumbnaillabel.repaint();

        layeredPane.add(thumbnaillabel);
        this.addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    layeredPane.updateUI();
                }
            }
        );
        //t.add(thumbnaillabel);
        SwingUtilities.invokeLater(() -> {
            this.add(layeredPane);
        });
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
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        JMenuItem openInBrowser = new JMenuItem(
            "Open in Browser",
            IconRes.LANGUAGE_ICON
        );
        openInBrowser.addActionListener(e -> {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(URI.create(item.getUrl()));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        popupMenu.add(openInBrowser);
        JMenuItem copyURL = new JMenuItem("Copy URL", IconRes.COPY_ICON);
        copyURL.addActionListener(e ->
            clipboard.setContents(new StringSelection(item.getUrl()), null)
        );
        popupMenu.add(copyURL);
        switch (item) {
            case StreamInfoItem streamInfoItem:
                JLabel uploaderLabel = new JLabel(
                    streamInfoItem.getUploaderName()
                );
                uploaderLabel.setForeground(Color.LIGHT_GRAY);
                infoPanel.add(uploaderLabel);
                String viewLabelString =
                    CommonUtil.numberToStringUnit(
                        streamInfoItem.getViewCount()
                    ) +
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

                JMenuItem showChannelDetile = new JMenuItem(
                    "Show channel Details",
                    IconRes.LIVE_TV_ICON
                );
                showChannelDetile.addActionListener(e ->
                    mainViewPort.nevigate(
                        new NevigateOpation(
                            MainViewPort.Page.CHANNEL,
                            streamInfoItem.getUploaderUrl()
                        )
                    )
                );
                popupMenu.add(showChannelDetile);
                JMenuItem openInVlc = new JMenuItem(
                    "Open in VLC media player",
                    IconRes.VLC_ICON
                );
                openInVlc.addActionListener(e ->
                    VideoUtil.openVLC(item.getUrl(), mainViewPort)
                );
                popupMenu.add(openInVlc);
                if (streamInfoItem.getStreamType() == StreamType.VIDEO_STREAM) {
                    JMenuItem downloadMenu = new JMenuItem(
                        "Download video",
                        IconRes.DOWNLOAD_ICON
                    );
                    downloadMenu.addActionListener(e ->
                        VideoUtil.downloadVideo(item.getUrl())
                    );
                    popupMenu.add(downloadMenu);
                }
                break;
            case PlaylistInfoItem playlistInfo:
                JLabel uploaderLabel2 = new JLabel(
                    playlistInfo.getUploaderName()
                );
                uploaderLabel2.setForeground(Color.LIGHT_GRAY);
                infoPanel.add(uploaderLabel2);
                JMenuItem showChannelDetile2 = new JMenuItem(
                    "Show channel Details",
                    IconRes.LIVE_TV_ICON
                );
                showChannelDetile2.setForeground(Color.LIGHT_GRAY);
                showChannelDetile2.addActionListener(e ->
                    mainViewPort.nevigate(
                        new NevigateOpation(
                            MainViewPort.Page.CHANNEL,
                            playlistInfo.getUploaderUrl()
                        )
                    )
                );
                popupMenu.add(showChannelDetile2);
                break;
            default:
                break;
        }

        this.setComponentPopupMenu(popupMenu);
        this.addMouseListener(new PanelClickListener(item));

        SwingUtilities.invokeLater(() -> {
            this.add(infoPanel);
            thumbnaillabel.updateUI();
            layeredPane.updateUI();
        });
    }

    private class PanelClickListener implements MouseListener {

        private final InfoItem infoItem;

        public PanelClickListener(InfoItem infoItem) {
            this.infoItem = infoItem;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            if (SwingUtilities.isRightMouseButton(e)) {
                return;
            }
            MainViewPort.Page newPage;
            switch (infoItem.getInfoType()) {
                case STREAM:
                    newPage = MainViewPort.Page.VIDEO;
                    break;
                case CHANNEL:
                    newPage = MainViewPort.Page.CHANNEL;
                    break;
                case PLAYLIST:
                    newPage = MainViewPort.Page.PLAYLIST;
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
