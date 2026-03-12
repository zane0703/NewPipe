package org.zane.newpipe;

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
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.*;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

public class ChannelPage extends JPanel {

    private final App app;
    private JImage banner;
    private JLabel channelName;
    private JImage channelAvatar;

    public ChannelPage(App app) {
        this.app = app;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        banner = new JImage();
        this.add(banner);
        JPanel ChannelInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        channelAvatar = new JImage();
        channelAvatar.setMaximumSize(new Dimension(100, 100));
        ChannelInfoPanel.add(channelAvatar);
        channelName = new JLabel();
        ChannelInfoPanel.add(channelName);
        this.add(ChannelInfoPanel);
    }

    public void ShowChannel(String channelURL) {
        new Thread(() -> {
            try {
                ChannelExtractor channelExtractor =
                    ServiceList.YouTube.getChannelExtractor(channelURL);
                channelExtractor.fetchPage();
                List<Image> banners = channelExtractor.getBanners();
                if (!banners.isEmpty()) {
                    BufferedImage image = ImageIO.read(
                        new URI(banners.get(0).getUrl()).toURL()
                    );
                    banner.setImage(image);
                }
                List<Image> avatars = channelExtractor.getAvatars();
                if (!avatars.isEmpty()) {
                    BufferedImage image = ImageIO.read(
                        new URI(avatars.get(0).getUrl()).toURL()
                    );
                    channelAvatar.setImage(image);
                }
                channelName.setText(channelExtractor.getName());
            } catch (ExtractionException | IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        })
            .start();
    }
}
