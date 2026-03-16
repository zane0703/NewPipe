package org.zane.newpipe.page;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Stack;
import javax.swing.*;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.SearchItemPanel;

public class SearchResultPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private SearchExtractor se;
    private JLabel pageNumLabel;
    private InfoItemsPage<InfoItem> itp;
    private Stack<Page> pageStack = new Stack<>();
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
        pageNevPanel.setBackground(new Color(255, 0, 0));
        preBtn = new JButton(IconRes.ARROW_BACK_ICOM);
        preBtn.addActionListener(e -> {
            resultListPanel.removeAll();
            new Thread(() -> {
                try {
                    Page prePage = pageStack.pop();
                    if (prePage == null) {
                        itp = se.getInitialPage();
                        showPage();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText("1");
                            preBtn.setEnabled(false);
                        });
                    } else {
                        itp = se.getPage(prePage);
                        showPage();
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
                    itp = se.getPage(nextPage);
                    showPage();
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

        this.add(pageNevPanel, BorderLayout.SOUTH);
    }

    public void search(String query, Runnable finaly) {
        resultListPanel.removeAll();
        pageStack.removeAllElements();
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
        }
    }
}
