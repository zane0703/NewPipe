package org.zane.newpipe.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NavigateOption;
import org.zane.newpipe.util.CommonUtil;

public class CommentItemPanel extends JPanel {

    private final MainViewPort mainViewPort;
    private final JLabel uploaderNameLabel;
    private final JViewport viewport;
    private final HyperlinkListener hyperlinkListener;
    private final CommentsInfoItem cit;
    private final CommentsExtractor commentsExtractor;

    public CommentItemPanel(
        MainViewPort mainViewPort,
        JViewport viewport,
        HyperlinkListener hyperlinkListener,
        CommentsInfoItem cit,
        CommentsExtractor commentsExtractor
    ) {
        super(new FlowLayout(FlowLayout.LEFT));
        this.mainViewPort = mainViewPort;
        this.viewport = viewport;
        this.hyperlinkListener = hyperlinkListener;
        this.cit = cit;
        this.commentsExtractor = commentsExtractor;

        List<Image> avatars = cit.getUploaderAvatars();
        ChannelClickListener ccl = new ChannelClickListener(
            cit.getUploaderUrl()
        );
        JImage jImage = new JImage();
        JPanel commentInfoPanel = new JPanel();
        uploaderNameLabel = new JLabel(cit.getUploaderName());
        JHTMLPane commentText = new JHTMLPane();
        JPanel commentMetaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel likeCunt = new JLabel(
            Integer.toString(cit.getLikeCount()),
            IconRes.THUMP_UP_SMALL_ICON,
            SwingConstants.LEFT
        );

        commentInfoPanel.setLayout(
            new BoxLayout(commentInfoPanel, BoxLayout.Y_AXIS)
        );

        jImage.addMouseListener(ccl);
        uploaderNameLabel.addMouseListener(ccl);
        commentText.addHyperlinkListener(hyperlinkListener);

        jImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        uploaderNameLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );

        uploaderNameLabel.setForeground(Color.lightGray);

        if (cit.isPinned()) {
            uploaderNameLabel.setIcon(IconRes.PIN_ICON);
        }

        jImage.setMaximumSize(new Dimension(50, 50));
        commentText.setMaximumSize(
            new Dimension(
                getPreferredSize().width - jImage.getWidth() - 100,
                Integer.MAX_VALUE
            )
        );

        if (!avatars.isEmpty()) {
            try {
                BufferedImage bImage = ImageIO.read(
                    new URI(avatars.get(0).getUrl()).toURL()
                );
                jImage.setImage(bImage);
            } catch (IOException | URISyntaxException err) {
                err.printStackTrace();
            }
        }

        commentInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        uploaderNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentText.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentMetaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        commentText.setText(cit.getCommentText().getContent());

        this.add(jImage);
        commentInfoPanel.add(uploaderNameLabel);
        commentInfoPanel.add(commentText);
        commentMetaPanel.add(likeCunt);
        if (cit.isHeartedByUploader()) {
            commentMetaPanel.add(new JLabel(IconRes.HEART_ICON));
        }
        commentMetaPanel.add(
            new JLabel(
                " " +
                    CommonUtil.formatRelativeTime(
                        cit.getUploadDate().getLocalDateTime()
                    )
            )
        );
        commentInfoPanel.add(commentMetaPanel);
        this.add(commentInfoPanel);
        int replyCount = cit.getReplyCount();
        if (replyCount > 0) {
            JButton replayBtn = new JButton(replyCount + " replies");
            replayBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

            replayBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            replayBtn.addActionListener(this::onReplayBtnClicked);
            commentInfoPanel.add(replayBtn);
        }

        this.addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    commentText.setMaximumSize(
                        new Dimension(
                            getWidth() - jImage.getWidth() - 100,
                            Integer.MAX_VALUE
                        )
                    );
                    commentInfoPanel.updateUI();
                }
            }
        );
    }

    private void onReplayBtnClicked(ActionEvent e) {
        JPanel replayListPanel = new JPanel();
        replayListPanel.setLayout(
            new BoxLayout(replayListPanel, BoxLayout.Y_AXIS)
        );
        JScrollPane scrollReplay = new JScrollPane(
            replayListPanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        JViewport scrollReplayViewPort = scrollReplay.getViewport();
        scrollReplayViewPort.setPreferredSize(new Dimension(500, 500));
        Thread.startVirtualThread(() -> {
            try {
                CommentPanel commentPanel = new CommentPanel(
                    mainViewPort,
                    scrollReplayViewPort,
                    hyperlinkListener,
                    commentsExtractor,
                    cit.getReplies()
                );

                scrollReplayViewPort.setView(commentPanel);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        this,
                        scrollReplay,
                        "Replies",
                        JOptionPane.PLAIN_MESSAGE
                    );
                });
            } catch (Exception err) {
                err.printStackTrace();
            }
        });
    }

    private class ChannelClickListener implements MouseListener {

        private final String channelURL;

        public ChannelClickListener(String channelURL) {
            this.channelURL = channelURL;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            mainViewPort.navigate(
                new NavigateOption(MainViewPort.Page.CHANNEL, channelURL)
            );
        }

        // Other MouseListener methods (must be implemented, even if empty)
        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {
            Font f = uploaderNameLabel.getFont();
            Map<TextAttribute, Object> attr = (Map<
                TextAttribute,
                Object
            >) f.getAttributes();
            attr.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            uploaderNameLabel.setFont(f.deriveFont(attr));
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Font f = uploaderNameLabel.getFont();
            Map<TextAttribute, Object> attr = (Map<
                TextAttribute,
                Object
            >) f.getAttributes();
            attr.put(TextAttribute.UNDERLINE, -1);
            uploaderNameLabel.setFont(f.deriveFont(attr));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        //int maxSize = Math.min(size.width, mainViewPort.getWidth());
        return new Dimension(viewport.getWidth(), size.height);
    }
}
