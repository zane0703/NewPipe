package org.zane.newpipe.page;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.zane.newpipe.database.Subscribed;
import org.zane.newpipe.ui.ItemListPanel;
import org.zane.newpipe.ui.ItemPanel;
import org.zane.newpipe.util.CommonUtil;

public class HomePage extends JTabbedPane {

    private final MainViewPort mainViewPort;
    private final Subscribed subscribed;

    public HomePage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        this.addMouseMotionListener(CommonUtil.TABBED_CURSOR);
        Thread.startVirtualThread(this::fetchPage);
        subscribed = mainViewPort.getApp().getDatabase().getSubscribed();
    }

    private void fetchPage() {
        try {
            KioskList kioskList = ServiceList.YouTube.getKioskList();
            for (String kioskId : kioskList.getAvailableKiosks()) {
                KioskExtractor ke = kioskList.getExtractorById(kioskId, null);
                ke.fetchPage();
                ItemListPanel itemListPanel = new ItemListPanel<InfoItem>(
                    mainViewPort,
                    ke
                );
                try {
                    String tabName = ke.getName();
                    SwingUtilities.invokeLater(() -> {
                        this.addTab(tabName, itemListPanel);
                    });
                } catch (ParsingException parE) {
                    parE.printStackTrace();
                }
            }
            List<ChannelInfoItem> ChannelInfoItems = subscribed.getAll(
                ServiceList.YouTube.getServiceId()
            );
            JPanel subscriptionPanel = new JPanel();
            subscriptionPanel.setLayout(
                new BoxLayout(subscriptionPanel, BoxLayout.Y_AXIS)
            );
            for (ChannelInfoItem channelInfoItem : ChannelInfoItems) {
                ChannelExtractor channelExtractor =
                    ServiceList.YouTube.getChannelExtractor(
                        channelInfoItem.getUrl()
                    );
                channelExtractor.fetchPage();
                channelInfoItem.setThumbnails(channelExtractor.getAvatars());
                channelInfoItem.setDescription(
                    channelExtractor.getDescription()
                );
                channelInfoItem.setSubscriberCount(
                    channelExtractor.getSubscriberCount()
                );
                ItemPanel.ChannelInfoPanel infoPanel =
                    new ItemPanel.ChannelInfoPanel(
                        mainViewPort,
                        channelInfoItem
                    );
                infoPanel.setAlignmentX(0);
                subscriptionPanel.add(infoPanel);
            }
            SwingUtilities.invokeLater(() ->
                this.addTab("Subscription", subscriptionPanel)
            );
            // setListExtractor(ke);
        } catch (
            ExtractionException
            | IOException
            | NullPointerException
            | URISyntaxException e
        ) {
            e.printStackTrace();
            if (CommonUtil.retryPrompt(mainViewPort, "video")) {
                SwingUtilities.invokeLater(() -> {
                    this.removeAll();
                });
                fetchPage();
            }
        }
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        Component c = getSelectedComponent();
        if (c != null) {
            Dimension d2 = c.getPreferredSize();
            return new Dimension(
                Math.min(d.width, mainViewPort.getWidth()),
                d2.height + 40
            );
        }
        return d;
    }
}
