package org.zane.newpipe;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

public class ChannelPage extends JPanel {

    private final App app;

    public ChannelPage(App app) {
        this.app = app;
    }

    public void ShowChannel(String channelURL) {
        new Thread(() -> {
            try {
                ChannelExtractor channelExtractor =
                    ServiceList.YouTube.getChannelExtractor(channelURL);
                channelExtractor.fetchPage();
            } catch (ExtractionException | IOException e) {
                e.printStackTrace();
            }
        })
            .start();
    }
}
