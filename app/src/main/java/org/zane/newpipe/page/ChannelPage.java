package org.zane.newpipe.page;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.zane.newpipe.ui.ChannelInfoPanel;
import org.zane.newpipe.ui.JHTMLPane;
import org.zane.newpipe.ui.JImage;
import org.zane.newpipe.util.WrapLayout;

public class ChannelPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JImage banner;
    private ChannelInfoPanel channelInfoPanel;
    private JPanel videoFeedListPanel;
    private JHTMLPane channelInfoText;
    private JPanel tagListPanel;
    private JTabbedPane channelNevView;
    private JPanel channelDetiledInfoPanel;

    public ChannelPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        banner = new JImage();
        this.add(banner);
        channelInfoPanel = new ChannelInfoPanel();
        this.add(channelInfoPanel);
        channelNevView = new JTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                Component c = getSelectedComponent();
                if (c != null) {
                    Dimension d2 = c.getPreferredSize();
                    return new Dimension(d.width, d2.height + 40);
                }
                return d;
            }
        };
        JPanel videoFeedPanel = new JPanel(new BorderLayout());
        Dimension maxSize = new Dimension(
            getPreferredSize().width,
            Integer.MAX_VALUE
        );
        channelDetiledInfoPanel = new JPanel();
        channelDetiledInfoPanel.setLayout(
            new BoxLayout(channelDetiledInfoPanel, BoxLayout.Y_AXIS)
        );
        channelInfoText = new JHTMLPane("text/plain");
        channelInfoText.setMaximumSize(maxSize);
        channelDetiledInfoPanel.add(channelInfoText);

        JLabel tagLabel = new JLabel("Tag", SwingConstants.CENTER);
        Font f = tagLabel.getFont();
        tagLabel.setFont(f.deriveFont(Font.BOLD));
        channelDetiledInfoPanel.add(tagLabel);
        tagListPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
        channelDetiledInfoPanel.add(tagListPanel);
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
        channelNevView.addChangeListener(e -> {
            channelNevView.revalidate();
        });
        this.add(channelNevView);
    }

    public void fetchChannel(String channelURL) {
        videoFeedListPanel.removeAll();
        new Thread(() -> {
            try {
                ChannelExtractor channelExtractor =
                    ServiceList.YouTube.getChannelExtractor(channelURL);
                channelExtractor.fetchPage();

                System.out.println(channelExtractor.getTabs());
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
                channelNevView.removeAll();
                for (ListLinkHandler tabLink : channelExtractor.getTabs()) {
                    ChannelTabExtractor cte =
                        ServiceList.YouTube.getChannelTabExtractor(tabLink);
                    cte.fetchPage();
                    SearchResultPage tabViewPanel = new SearchResultPage(
                        mainViewPort,
                        cte
                    );
                    channelNevView.addTab(cte.getName(), tabViewPanel);
                }
                channelNevView.addTab("Info", channelDetiledInfoPanel);
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
}
