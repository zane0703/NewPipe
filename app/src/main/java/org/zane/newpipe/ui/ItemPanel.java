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
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NavigateOption;
import org.zane.newpipe.util.CommonUtil;
import org.zane.newpipe.util.VideoUtil;

public class ItemPanel extends JPanel {

    private final MainViewPort mainViewPort;
    JPopupMenu popupMenu;
    JPanel layeredPane;
    JImage thumbnailLabel;
    JPanel infoPanel;

    ItemPanel(MainViewPort mainViewPort, InfoItem item)
        throws IOException, URISyntaxException {
        super(new FlowLayout(FlowLayout.LEFT));
        this.mainViewPort = mainViewPort;
        popupMenu = new JPopupMenu("video");

        BufferedImage image = ImageIO.read(
            new URI(item.getThumbnails().get(0).getUrl()).toURL()
        ); // JPanel t = new JPanel(new SpringLayout());
        layeredPane = new JPanel();
        thumbnailLabel = new JImage(image, this);
        thumbnailLabel.setMaximumSize(new Dimension(200, 200));
        String itemName = item.getName();
        JLabel popUpLabel = new JLabel(
            itemName.length() > 30
                ? itemName.substring(0, 30) + "..."
                : itemName
        );

        Font currentFont = popUpLabel.getFont();
        infoPanel = new JPanel();
        JHTMLPane itemTitle = new JHTMLPane();

        layeredPane.setLayout(new OverlayLayout(layeredPane));
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));

        popUpLabel.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        // MarqueePanel marquee = new MarqueePanel(10, 5);
        // marquee.add(popUpLabel);
        popupMenu.add(popUpLabel);
        popupMenu.addSeparator();
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        SwingUtilities.invokeLater(() -> {
            this.add(layeredPane);
        });

        currentFont = itemTitle.getFont();
        itemTitle.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        itemTitle.setAlignmentX(0);
        infoPanel.setBackground(new Color(0, 0, 0, 0));
        infoPanel.setOpaque(false);
        itemTitle.setText(itemName);
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

        this.setComponentPopupMenu(popupMenu);
        this.addMouseListener(new PanelClickListener(item));

        SwingUtilities.invokeLater(() -> {
            this.add(infoPanel);
            thumbnailLabel.updateUI();
            layeredPane.updateUI();
        });
        this.addComponentListener(
            new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    itemTitle.setMaximumSize(
                        new Dimension(
                            getWidth() - thumbnailLabel.getWidth() - 20,
                            Integer.MAX_VALUE
                        )
                    );
                    infoPanel.updateUI();
                    layeredPane.updateUI();
                }
            }
        );
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
            mainViewPort.navigate(
                new NavigateOption(newPage, infoItem.getUrl())
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

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int maxSize = Math.min(size.width, mainViewPort.getWidth());
        return new Dimension(maxSize, size.height);
    }

    public static class StreamInfoPanel extends ItemPanel {

        public StreamInfoPanel(
            MainViewPort mainViewPort,
            StreamInfoItem streamInfoItem
        ) throws IOException, URISyntaxException {
            super(mainViewPort, streamInfoItem);
            JLabel uploaderLabel = new JLabel(streamInfoItem.getUploaderName());
            JMenuItem showChannelDetails = new JMenuItem(
                "Show channel Details",
                IconRes.LIVE_TV_ICON
            );
            JMenuItem openInVlc = new JMenuItem(
                "Open in VLC media player",
                IconRes.VLC_ICON
            );

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

            JLabel videoTypeLabel;
            viaItemType: {
                switch (streamInfoItem.getStreamType()) {
                    case LIVE_STREAM:
                    case AUDIO_LIVE_STREAM:
                        videoTypeLabel = new JLabel(
                            "LIVE",
                            SwingConstants.RIGHT
                        );
                        videoTypeLabel.setBackground(new Color(255, 0, 0, 200));
                        break;
                    default:
                        long videoDuration = streamInfoItem.getDuration();
                        if (videoDuration < 0) {
                            break viaItemType;
                        }
                        videoTypeLabel = new JLabel(
                            CommonUtil.getTimeString(
                                streamInfoItem.getDuration()
                            ),
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
                thumbnailLabel.setAlignmentX(1.0f);
                thumbnailLabel.setAlignmentY(1.0f);
            }
            thumbnailLabel.repaint();
            layeredPane.add(thumbnailLabel);

            uploaderLabel.setAlignmentX(0);
            viewLabel.setAlignmentX(0);

            uploaderLabel.setForeground(Color.LIGHT_GRAY);
            viewLabel.setForeground(Color.LIGHT_GRAY);

            showChannelDetails.addActionListener(e ->
                mainViewPort.navigate(
                    new NavigateOption(
                        MainViewPort.Page.CHANNEL,
                        streamInfoItem.getUploaderUrl()
                    )
                )
            );
            openInVlc.addActionListener(e ->
                VideoUtil.openVLC(streamInfoItem.getUrl(), mainViewPort)
            );
            infoPanel.add(uploaderLabel);
            infoPanel.add(viewLabel);
            popupMenu.add(showChannelDetails);
            popupMenu.add(openInVlc);
            if (streamInfoItem.getStreamType() == StreamType.VIDEO_STREAM) {
                JMenuItem downloadMenu = new JMenuItem(
                    "Download video",
                    IconRes.DOWNLOAD_ICON
                );
                downloadMenu.addActionListener(e ->
                    VideoUtil.downloadVideo(
                        streamInfoItem.getUrl(),
                        false,
                        mainViewPort.getApp().getTrayIcon()
                    )
                );
                popupMenu.add(downloadMenu);
            }
        }
    }

    public static class ChannelInfoPanel extends ItemPanel {

        public ChannelInfoPanel(
            MainViewPort mainViewPort,
            ChannelInfoItem channelInfoItem
        ) throws IOException, URISyntaxException {
            super(mainViewPort, channelInfoItem);
            JLabel channelSubCountLabel = new JLabel(
                CommonUtil.numberToStringUnit(
                        channelInfoItem.getSubscriberCount()
                    ) +
                    " Subscribers"
            );
            String description = channelInfoItem.getDescription();
            JHTMLPane descriptionLabel = new JHTMLPane("text/plain");
            descriptionLabel.setText(
                description.length() > 200
                    ? description.substring(0, 200) + "..."
                    : description
            );

            this.addComponentListener(
                new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        descriptionLabel.setMaximumSize(
                            new Dimension(
                                getWidth() - thumbnailLabel.getWidth() - 20,
                                Integer.MAX_VALUE
                            )
                        );
                    }
                }
            );
            channelSubCountLabel.setAlignmentX(0);
            descriptionLabel.setAlignmentX(0);

            channelSubCountLabel.setForeground(Color.LIGHT_GRAY);
            descriptionLabel.setForeground(Color.LIGHT_GRAY);

            thumbnailLabel.repaint();
            layeredPane.add(thumbnailLabel);

            infoPanel.add(descriptionLabel);
            infoPanel.add(channelSubCountLabel);
            descriptionLabel.updateUI();
        }
    }

    public static class PlayListInfoPanel extends ItemPanel {

        public PlayListInfoPanel(
            MainViewPort mainViewPort,
            PlaylistInfoItem playlistInfoItem
        ) throws IOException, URISyntaxException {
            super(mainViewPort, playlistInfoItem);
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
            thumbnailLabel.setAlignmentX(1.0f);
            thumbnailLabel.setAlignmentY(0.0f);
            thumbnailLabel.repaint();
            layeredPane.add(thumbnailLabel);
            JLabel uploaderLabel = new JLabel(
                playlistInfoItem.getUploaderName()
            );
            uploaderLabel.setAlignmentX(0);

            uploaderLabel.setForeground(Color.LIGHT_GRAY);
            infoPanel.add(uploaderLabel);
            JMenuItem showChannelDetails = new JMenuItem(
                "Show channel Details",
                IconRes.LIVE_TV_ICON
            );
            showChannelDetails.setForeground(Color.LIGHT_GRAY);
            showChannelDetails.addActionListener(e ->
                mainViewPort.navigate(
                    new NavigateOption(
                        MainViewPort.Page.CHANNEL,
                        playlistInfoItem.getUploaderUrl()
                    )
                )
            );
            popupMenu.add(showChannelDetails);
        }
    }
}
