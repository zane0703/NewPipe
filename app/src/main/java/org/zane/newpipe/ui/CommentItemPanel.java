package org.zane.newpipe.ui;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.zane.newpipe.util.CommonUtil;

public class CommentItemPanel extends JPanel {

    private JViewport scrollViewPort;

    public CommentItemPanel(
        CommentsInfoItem cit,
        CommentsExtractor commentsExtractor,
        JViewport scrollViewPort
    ) {
        this.scrollViewPort = scrollViewPort;
        setLayout(new FlowLayout(FlowLayout.LEFT));
        List<Image> avatars = cit.getUploaderAvatars();
        JImage jImage = new JImage();
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
        uploaderNamepPanel.add(new JLabel(cit.getUploaderName()));
        commentInfoPanel.add(uploaderNamepPanel);
        JHTMLPane commentText = new JHTMLPane();
        commentText.setText(cit.getCommentText().getContent());
        commentText.setMaximumSize(
            new Dimension(
                getPreferredSize().width - jImage.getWidth() - 100,
                Integer.MAX_VALUE
            )
        );
        commentInfoPanel.add(commentText);

        JPanel commentMetaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel likeCunt = new JLabel(
            Integer.toString(cit.getLikeCount()),
            IconRes.THUMP_UP_SMALL_ICOM,
            SwingConstants.LEFT
        );
        commentMetaPanel.add(likeCunt);
        if (cit.isHeartedByUploader()) {
            commentMetaPanel.add(new JLabel(IconRes.HEART_ICOM));
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
                        InfoItemsPage<CommentsInfoItem> repliestemsPage =
                            commentsExtractor.getPage(cit.getReplies());
                        List<CommentsInfoItem> replieyItems =
                            repliestemsPage.getItems();
                        for (CommentsInfoItem rItem : replieyItems) {
                            CommentItemPanel replayItemPanel =
                                new CommentItemPanel(
                                    rItem,
                                    commentsExtractor,
                                    scrollViewPort
                                );
                            replayListPanel.add(replayItemPanel);
                        }
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                this,
                                scrollReplay,
                                "Replies",
                                JOptionPane.PLAIN_MESSAGE
                            );
                        });
                    } catch (IOException | ExtractionException err) {
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

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        //int maxSize = Math.min(size.width, scrollViewPort.getWidth());
        return new Dimension(scrollViewPort.getWidth(), size.height);
    }
}
