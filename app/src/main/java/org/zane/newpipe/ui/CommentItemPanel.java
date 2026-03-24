package org.zane.newpipe.ui;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
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

    private MainViewPort mainViewPort;
    private JLabel uploaderNameLabel;

    public CommentItemPanel(
        MainViewPort mainViewPort,
        HyperlinkListener hyperlinkListener,
        CommentsInfoItem cit,
        CommentsExtractor commentsExtractor
    ) {
        this.mainViewPort = mainViewPort;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        List<Image> avatars = cit.getUploaderAvatars();
        ChannelClickListener ccl = new ChannelClickListener(
            cit.getUploaderUrl()
        );
        JImage jImage = new JImage();
        jImage.addMouseListener(ccl);
        jImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jImage.setMaximumSize(new Dimension(50, 50));
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
        SwingUtilities.invokeLater(() -> {
            this.add(jImage);
        });
        JPanel commentInfoPanel = new JPanel();
        commentInfoPanel.setLayout(
            new BoxLayout(commentInfoPanel, BoxLayout.Y_AXIS)
        );

        commentInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel uploaderNamepPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        uploaderNameLabel = new JLabel(cit.getUploaderName());
        uploaderNameLabel.addMouseListener(ccl);
        uploaderNameLabel.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        );
        uploaderNamepPanel.add(uploaderNameLabel);
        commentInfoPanel.add(uploaderNamepPanel);
        JHTMLPane commentText = new JHTMLPane();
        commentText.setText(cit.getCommentText().getContent());
        commentText.setMaximumSize(
            new Dimension(
                getPreferredSize().width - jImage.getWidth() - 100,
                Integer.MAX_VALUE
            )
        );
        commentText.addHyperlinkListener(hyperlinkListener);
        commentInfoPanel.add(commentText);

        JPanel commentMetaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel likeCunt = new JLabel(
            Integer.toString(cit.getLikeCount()),
            IconRes.THUMP_UP_SMALL_ICON,
            SwingConstants.LEFT
        );
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
        SwingUtilities.invokeLater(() -> {
            this.add(commentInfoPanel);
        });

        int replyCount = cit.getReplyCount();
        if (replyCount > 0) {
            JPanel replayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton replayBtm = new JButton(replyCount + " replies");
            replayBtm.addActionListener(e -> {
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
                scrollReplayViewPort.setPreferredSize(new Dimension(700, 500));
                new Thread(() -> {
                    try {
                        CommentPanel commentPanel = new CommentPanel(
                            mainViewPort,
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
                })
                    .start();
            });
            replayPanel.add(replayBtm);
            commentInfoPanel.add(replayPanel);
        }

        this.addComponentListener(
            new ComponentListener() {
                @Override
                public void componentHidden(ComponentEvent e) {}

                @Override
                public void componentShown(ComponentEvent e) {}

                public void componentMoved(ComponentEvent e) {}

                public void componentResized(ComponentEvent e) {
                    commentText.setMaximumSize(
                        new Dimension(
                            getPreferredSize().width - jImage.getWidth() - 100,
                            Integer.MAX_VALUE
                        )
                    );
                }
            }
        );
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
        //int maxSize = Math.min(size.width, scrollViewPort.getWidth());
        return new Dimension(mainViewPort.getWidth(), size.height);
    }
}
