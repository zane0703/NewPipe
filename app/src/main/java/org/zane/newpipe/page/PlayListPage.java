package org.zane.newpipe.page;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.ArrayDeque;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.ItemListPanel;
import org.zane.newpipe.ui.SearchItemPanel;

public class PlayListPage extends ItemListPanel<StreamInfoItem> {

    private final MainViewPort mainViewPort;
    private PlaylistExtractor pe;
    private InfoItemsPage<StreamInfoItem> itp;
    private JLabel playListTitleLabel;
    private JLabel playListSubInfo;
    private JLabel channelName;

    public PlayListPage(MainViewPort mainViewPort) {
        super(mainViewPort);
        this.mainViewPort = mainViewPort;
        JPanel playlistHeader = new JPanel(new GridLayout(2, 1));
        playListTitleLabel = new JLabel("", SwingConstants.LEFT);
        channelName = new JLabel("", SwingConstants.LEFT);
        JPanel playListSubInfoPanel = new JPanel(new GridLayout(1, 2));
        playListSubInfo = new JLabel("", SwingConstants.RIGHT);

        //set layout
        Font f = playListTitleLabel.getFont();
        playListTitleLabel.setFont(f.deriveFont(Font.BOLD, f.getSize()));

        playlistHeader.add(playListTitleLabel);
        playListSubInfoPanel.add(channelName);
        playListSubInfoPanel.add(playListSubInfo);
        playlistHeader.add(playListSubInfoPanel);
        this.add(playlistHeader, BorderLayout.NORTH);
    }

    public void fetchPlayList(String playListUrl) {
        Thread.startVirtualThread(() -> {
            try {
                pe = ServiceList.YouTube.getPlaylistExtractor(playListUrl);
                pe.fetchPage();

                playListTitleLabel.setText(pe.getName());
                channelName.setText(pe.getUploaderName());
                List<Image> uploaderAvatars = pe.getUploaderAvatars();
                if (uploaderAvatars != null && !uploaderAvatars.isEmpty()) {
                    Image uploaderAvatar = uploaderAvatars.get(0);
                    BufferedImage bImage = ImageIO.read(
                        URI.create(uploaderAvatar.getUrl()).toURL()
                    );
                    if (bImage != null) {
                        channelName.setIcon(new ImageIcon(bImage));
                    }
                }

                playListSubInfo.setText(
                    NumberFormat.getInstance().format(pe.getStreamCount()) +
                        " Videos"
                );
                setListExtractor(pe);
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        });
    }
}
