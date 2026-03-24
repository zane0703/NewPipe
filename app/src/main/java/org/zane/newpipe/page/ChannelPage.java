package org.zane.newpipe.page;

import java.awt.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
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
import org.zane.newpipe.ui.JHTMLPane;
import org.zane.newpipe.ui.JImage;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;
import org.zane.newpipe.util.WrapLayout;

public class ChannelPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JImage banner;
    private ChannelInfoPanel channelInfoPanel;
    private JPanel videoFeedListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private Stack<Page> pageStack = new Stack<>();
    private JLabel pageNumLabel;
    private FeedExtractor fe;
    private InfoItemsPage<StreamInfoItem> itp;
    private Page currentPage;
    private JHTMLPane channelInfoText;
    private JPanel tagListPanel;

    public ChannelPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        banner = new JImage();
        this.add(banner);
        channelInfoPanel = new ChannelInfoPanel();
        this.add(channelInfoPanel);
        JViewport channelNevView = new JViewport();
        JPanel videoFeedPanel = new JPanel(new BorderLayout());
        Dimension maxSize = new Dimension(
            getPreferredSize().width,
            Integer.MAX_VALUE
        );
        JPanel channelInfoPanel = new JPanel();
        channelInfoPanel.setLayout(
            new BoxLayout(channelInfoPanel, BoxLayout.Y_AXIS)
        );
        channelInfoText = new JHTMLPane("text/plain");
        channelInfoText.setMaximumSize(maxSize);
        channelInfoPanel.add(channelInfoText);

        JLabel tagLabel = new JLabel("Tag", SwingConstants.CENTER);
        Font f = tagLabel.getFont();
        tagLabel.setFont(f.deriveFont(Font.BOLD));
        channelInfoPanel.add(tagLabel);
        tagListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
        channelInfoPanel.add(tagListPanel);
        tagListPanel.setMaximumSize(maxSize);
        this.addComponentListener(
            new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    Dimension maxSize = new Dimension(
                        getPreferredSize().width,
                        Integer.MAX_VALUE
                    );
                    channelInfoText.setMaximumSize(maxSize);
                    tagListPanel.setMaximumSize(maxSize);
                }
            }
        );
        videoFeedListPanel = new JPanel();
        videoFeedListPanel.setLayout(
            new BoxLayout(videoFeedListPanel, BoxLayout.Y_AXIS)
        );
        videoFeedPanel.add(videoFeedListPanel, BorderLayout.CENTER);
        JPanel channelNevBar = new JPanel(new GridLayout(1, 2));
        JButton videoFeedBtn = new JButton("video");
        videoFeedBtn.addActionListener(e ->
            channelNevView.setView(videoFeedPanel)
        );
        channelNevBar.add(videoFeedBtn);
        JButton infoBtn = new JButton("Info");
        infoBtn.addActionListener(e ->
            channelNevView.setView(channelInfoPanel)
        );
        channelNevBar.add(infoBtn);
        this.add(channelNevBar);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(IconRes.YOUTUBE_COLOUR);
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        preBtn.addActionListener(e -> {
            videoFeedListPanel.removeAll();
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
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICON);
        nextBtn.addActionListener(e -> {
            videoFeedListPanel.removeAll();
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

        videoFeedPanel.add(pageNevPanel, BorderLayout.SOUTH);
        channelNevView.setView(videoFeedPanel);
        this.add(channelNevView);
    }

    public void fetchChannel(String channelURL) {
        videoFeedListPanel.removeAll();
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
                String channelDist = channelExtractor.getDescription();
                List<String> tagList = channelExtractor.getTags();
                SwingUtilities.invokeLater(() -> {
                    channelInfoText.setText(channelDist);
                    tagListPanel.removeAll();
                    for (String tag : tagList) {
                        JButton tagBtn = new JButton(tag);
                        tagBtn.addActionListener(e ->
                            mainViewPort.navigate(
                                new MainViewPort.NavigateOption(
                                    MainViewPort.Page.SEARCH,
                                    "#" + tag
                                )
                            )
                        );
                        tagListPanel.add(tagBtn);
                    }
                });
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
                    videoFeedListPanel.add(searchItemPanel)
                );
            }
            SwingUtilities.invokeLater(() -> {
                videoFeedListPanel.updateUI();
                nextBtn.setEnabled(itp.hasNextPage());
            });
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
