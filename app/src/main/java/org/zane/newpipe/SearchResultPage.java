package org.zane.newpipe;

import com.formdev.flatlaf.extras.FlatSVGIcon;
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

public class SearchResultPage extends JPanel {

    private final App app;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private SearchExtractor se;
    private JLabel pageNumLabel;
    private InfoItemsPage<InfoItem> itp;
    private Stack<Page> pageStack = new Stack<>();
    private Page currentPage;

    public SearchResultPage(App app, Icon arrowIocn) {
        this.setLayout(new BorderLayout());
        FlatSVGIcon arrowNexticon = null;
        try {
            arrowNexticon = new FlatSVGIcon(
                getClass().getResourceAsStream("/icon/ic_arrow_next.svg")
            );
        } catch (IOException eio) {
            eio.printStackTrace();
            System.exit(1);
        }
        this.app = app;
        resultListPanel = new JPanel();
        resultListPanel.setLayout(
            new BoxLayout(resultListPanel, BoxLayout.Y_AXIS)
        );
        this.add(resultListPanel, BorderLayout.CENTER);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(new Color(255, 0, 0));
        preBtn = new JButton(arrowIocn);
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
        nextBtn = new JButton(arrowNexticon);
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
        currentPage = null;
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
    }

    private void showPage() {
        new Thread(() -> {
            try {
                List<InfoItem> items = itp.getItems();
                for (int i = 0; i < items.size(); ++i) {
                    InfoItem item = items.get(i);
                    SearchItemPanel searchItemPanel = new SearchItemPanel(
                        app,
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
            } catch (URISyntaxException | IOException err) {}
        })
            .start();
    }
}
