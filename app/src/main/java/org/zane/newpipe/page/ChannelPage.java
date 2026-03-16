package org.zane.newpipe.page;

import java.awt.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import java.util.List;
import java.util.Stack;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.*;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.feed.FeedExtractor;
import org.schabi.newpipe.extractor.feed.FeedInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.zane.newpipe.ui.ChannelInfoPanel;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.JImage;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;

public class ChannelPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JImage banner;
    private ChannelInfoPanel channelInfoPanel;
    private JPanel videoFeedPanel;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private Stack<Page> pageStack = new Stack<>();
    private JLabel pageNumLabel;
    private FeedExtractor fe;
    private InfoItemsPage<StreamInfoItem> itp;
    private Page currentPage;

    public ChannelPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        banner = new JImage();
        this.add(banner);
        channelInfoPanel = new ChannelInfoPanel();
        this.add(channelInfoPanel);
        videoFeedPanel = new JPanel();
        videoFeedPanel.setLayout(
            new BoxLayout(videoFeedPanel, BoxLayout.Y_AXIS)
        );
        this.add(videoFeedPanel);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(new Color(255, 0, 0));
        preBtn = new JButton(IconRes.ARROW_BACK_ICOM);
        preBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
                try {
                    Page prePage = pageStack.pop();
                    if (prePage == null) {
                        itp = fe.getInitialPage();
                        showFeed();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText("1");
                            preBtn.setEnabled(false);
                        });
                    } else {
                        itp = fe.getPage(prePage);
                        showFeed();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText(
                                Integer.toString(pageStack.size() + 1)
                            );
                        });
                    }
                    currentPage = prePage;
                } catch (ExtractionException | IOException err) {
                    err.printStackTrace();
                }
            })
                .start();
        });
        pageNevPanel.add(preBtn);
        pageNumLabel = new JLabel("1", SwingConstants.CENTER);
        pageNevPanel.add(pageNumLabel);
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICOM);
        nextBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
                try {
                    Page nextPage = itp.getNextPage();
                    itp = fe.getPage(nextPage);
                    showFeed();
                    pageStack.add(currentPage);
                    SwingUtilities.invokeLater(() -> {
                        pageNumLabel.setText(
                            Integer.toString(pageStack.size() + 1)
                        );
                        preBtn.setEnabled(true);
                    });
                    currentPage = nextPage;
                } catch (ExtractionException | IOException err) {
                    err.printStackTrace();
                }
            })
                .start();
        });
        pageNevPanel.add(nextBtn);

        this.add(pageNevPanel);
    }

    public void fetchChannel(String channelURL) {
        videoFeedPanel.removeAll();
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        new Thread(() -> {
            try {
                ChannelExtractor channelExtractor =
                    ServiceList.YouTube.getChannelExtractor(channelURL);
                channelExtractor.fetchPage();
                List<Image> banners = channelExtractor.getBanners();
                BufferedImage imageb;
                if (banners.isEmpty()) {
                    imageb = ImageIO.read(
                        getClass().getResourceAsStream(
                            "/placeholder_channel_banner.png"
                        )
                    );
                } else {
                    imageb = ImageIO.read(
                        new URI(banners.get(0).getUrl()).toURL()
                    );
                }
                banner.setImage(imageb);
                List<Image> avatars = channelExtractor.getAvatars();
                if (!avatars.isEmpty()) {
                    channelInfoPanel.setChanelAvatar(avatars.get(0).getUrl());
                }
                channelInfoPanel.setInfo(
                    channelExtractor.getName(),
                    channelExtractor.getSubscriberCount()
                );

                fe = ServiceList.YouTube.getFeedExtractor(channelURL);
                fe.fetchPage();
                itp = fe.getInitialPage();
                showFeed();
            } catch (ExtractionException | IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        })
            .start();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (
            SwingUtilities.getAncestorOfClass(JViewport.class, this) instanceof
                JViewport viewport
        ) {
            return new Dimension(
                Math.min(size.width, viewport.getWidth()),
                size.height
            );
        }
        return size;
    }

    private void showFeed() {
        try {
            List<StreamInfoItem> fList = itp.getItems();
            for (StreamInfoItem fItem : fList) {
                SearchItemPanel searchItemPanel = new SearchItemPanel(
                    mainViewPort,
                    fItem
                );
                SwingUtilities.invokeLater(() ->
                    videoFeedPanel.add(searchItemPanel)
                );
            }
            SwingUtilities.invokeLater(() -> {
                videoFeedPanel.updateUI();
                nextBtn.setEnabled(itp.hasNextPage());
            });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
