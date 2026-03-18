package org.zane.newpipe.page;

import java.awt.*;
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
import org.zane.newpipe.ui.ChannelInfoPanel;
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
        this.setLayout(new BorderLayout());
        this.mainViewPort = mainViewPort;
        resultListPanel = new JPanel();
        resultListPanel.setLayout(
            new BoxLayout(resultListPanel, BoxLayout.Y_AXIS)
        );
        JPanel playlistHeader = new JPanel(new GridLayout(2, 1));
        playListTitleLabel = new JLabel("", SwingConstants.LEFT);
        Font f = playListTitleLabel.getFont();
        playListTitleLabel.setFont(f.deriveFont(Font.BOLD));
        playlistHeader.add(playListTitleLabel);
        JPanel playListSubInfoPanel = new JPanel(new GridLayout(1, 2));
        channelName = new JLabel("", SwingConstants.LEFT);
        playListSubInfoPanel.add(channelName);
        playListSubInfo = new JLabel("", SwingConstants.RIGHT);
        playListSubInfoPanel.add(playListSubInfo);
        playlistHeader.add(playListSubInfoPanel);
        this.add(playlistHeader, BorderLayout.NORTH);
        this.add(resultListPanel, BorderLayout.CENTER);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(new Color(255, 0, 0));
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        preBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
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
            })
                .start();
        });
        pageNevPanel.add(preBtn);
        pageNumLabel = new JLabel("1", SwingConstants.CENTER);
        pageNevPanel.add(pageNumLabel);
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICON);
        nextBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
                try {
                    Page nextPage = itp.getNextPage();
                    itp = pe.getPage(nextPage);
                    mainViewPort.setViewPosition(new Point(0, 0));
                    showPage();
                    if (currentPage != null) {
                        pageStack.push(currentPage);
                    }
                    SwingUtilities.invokeLater(() -> {
                        pageNumLabel.setText(
                            Integer.toString(pageStack.size() + 2)
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

        this.add(pageNevPanel, BorderLayout.SOUTH);
    }

    public void fatchPlayList(String playListUrl) {
        resultListPanel.removeAll();
        pageStack.clear();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = null;
        new Thread(() -> {
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
                System.out.println(pe.getTimeAgoParser());
                itp = pe.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        })
            .start();
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
}
