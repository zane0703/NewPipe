package org.zane.newpipe.page;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;

public class SearchResultPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private SearchExtractor se;
    private JLabel pageNumLabel;
    private InfoItemsPage<InfoItem> itp;
    private ArrayDeque<Page> pageStack = new ArrayDeque<>();
    private Page currentPage;

    public SearchResultPage(MainViewPort mainViewPort) {
        this.setLayout(new BorderLayout());
        this.mainViewPort = mainViewPort;
        resultListPanel = new JPanel();
        resultListPanel.setLayout(
            new BoxLayout(resultListPanel, BoxLayout.Y_AXIS)
        );

        this.add(resultListPanel, BorderLayout.CENTER);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(IconRes.YOUTUBE_COLOUR);
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        preBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
                try {
                    if (pageStack.isEmpty()) {
                        itp = se.getInitialPage();
                        showPage();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText("1");
                            preBtn.setEnabled(false);
                        });
                        currentPage = null;
                    } else {
                        Page prePage = pageStack.pop();
                        itp = se.getPage(prePage);
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
                    itp = se.getPage(nextPage);
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

    public void search(String query, Runnable finaly) {
        resultListPanel.removeAll();
        pageStack.clear();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = null;
        new Thread(() -> {
            try {
                se = ServiceList.YouTube.getSearchExtractor(query);
                se.fetchPage();
                itp = se.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
                if (CommonUtil.retryPrompt(mainViewPort, "search")) {
                    search(query, finaly);
                }
            } finally {
                finaly.run();
            }
        })
            .start();
    }

    private void showPage() {
        try {
            List<InfoItem> items = itp.getItems();
            for (int i = 0; i < items.size(); ++i) {
                InfoItem item = items.get(i);
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
            if (CommonUtil.retryPrompt(mainViewPort, "search")) {
                showPage();
            }
        }
    }
}
