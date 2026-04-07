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
import org.zane.newpipe.ui.SearchItemPanel;

public class PlayListPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private PlaylistExtractor pe;
    private JLabel pageNumLabel;
    private InfoItemsPage<StreamInfoItem> itp;
    private ArrayDeque<Page> pageStack = new ArrayDeque<>();
    private Page currentPage;
    private JLabel playListTitleLabel;
    private JLabel playListSubInfo;
    private JLabel channelName;

    public PlayListPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        resultListPanel = new JPanel();
        JPanel playlistHeader = new JPanel(new GridLayout(2, 1));
        playListTitleLabel = new JLabel("", SwingConstants.LEFT);
        channelName = new JLabel("", SwingConstants.LEFT);
        JPanel playListSubInfoPanel = new JPanel(new GridLayout(1, 2));
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        pageNumLabel = new JLabel("1", SwingConstants.CENTER);
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICON);
        playListSubInfo = new JLabel("", SwingConstants.RIGHT);

        //set layout
        this.setLayout(new BorderLayout());
        resultListPanel.setLayout(
            new BoxLayout(resultListPanel, BoxLayout.Y_AXIS)
        );

        Font f = playListTitleLabel.getFont();
        playListTitleLabel.setFont(f.deriveFont(Font.BOLD));

        pageNevPanel.setBackground(IconRes.YOUTUBE_COLOUR);

        preBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        preBtn.addActionListener(this::onPreBtnClicked);
        nextBtn.addActionListener(this::onNextBtnClicked);

        playlistHeader.add(playListTitleLabel);
        playListSubInfoPanel.add(channelName);
        playListSubInfoPanel.add(playListSubInfo);
        playlistHeader.add(playListSubInfoPanel);
        this.add(playlistHeader, BorderLayout.NORTH);

        this.add(resultListPanel, BorderLayout.CENTER);

        pageNevPanel.add(preBtn);
        pageNevPanel.add(pageNumLabel);
        pageNevPanel.add(nextBtn);
        this.add(pageNevPanel, BorderLayout.SOUTH);
    }

    private void onPreBtnClicked(ActionEvent e) {
        resultListPanel.removeAll();
        Thread.startVirtualThread(this::onPreBtnClicked);
    }

    private void onPreBtnClicked() {
        try {
            if (pageStack.isEmpty()) {
                itp = pe.getInitialPage();
                showPage();
                SwingUtilities.invokeLater(() -> {
                    pageNumLabel.setText("1");
                    preBtn.setEnabled(false);
                });
                currentPage = null;
            } else {
                Page prePage = pageStack.pop();
                itp = pe.getPage(prePage);
                showPage();
                SwingUtilities.invokeLater(() -> {
                    pageNumLabel.setText(
                        Integer.toString(pageStack.size() + 2)
                    );
                });
                currentPage = prePage;
            }

            mainViewPort.setViewPosition(new Point(0, 0));
        } catch (ExtractionException | IOException err) {
            err.printStackTrace();
        }
    }

    private void onNextBtnClicked(ActionEvent e) {
        resultListPanel.removeAll();
        Thread.startVirtualThread(this::onNextBtnClicked);
    }

    private void onNextBtnClicked() {
        try {
            Page nextPage = itp.getNextPage();
            itp = pe.getPage(nextPage);
            mainViewPort.setViewPosition(new Point(0, 0));
            showPage();
            if (currentPage != null) {
                pageStack.push(currentPage);
            }
            SwingUtilities.invokeLater(() -> {
                pageNumLabel.setText(Integer.toString(pageStack.size() + 2));
                preBtn.setEnabled(true);
            });
            currentPage = nextPage;
        } catch (ExtractionException | IOException err) {
            err.printStackTrace();
        }
    }

    public void fetchPlayList(String playListUrl) {
        resultListPanel.removeAll();
        pageStack.clear();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = null;
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
                itp = pe.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        });
    }

    private void showPage() {
        try {
            List<StreamInfoItem> items = itp.getItems();
            for (int i = 0; i < items.size(); ++i) {
                StreamInfoItem item = items.get(i);
                SearchItemPanel searchItemPanel = new SearchItemPanel(
                    mainViewPort,
                    item
                );
                SwingUtilities.invokeLater(() ->
                    resultListPanel.add(searchItemPanel)
                );
            }

            SwingUtilities.invokeLater(() -> {
                resultListPanel.updateUI();
                nextBtn.setEnabled(itp.hasNextPage());
            });
        } catch (URISyntaxException | IOException err) {
            err.printStackTrace();
        }
    }

    public void clear() {
        resultListPanel.removeAll();
        System.gc();
    }
}
