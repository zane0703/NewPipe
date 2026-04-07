package org.zane.newpipe.page;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.List;
import javax.swing.*;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;

public class SearchResultPage extends JPanel {

    private final MainViewPort mainViewPort;
    private JPanel resultListPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private ListExtractor<InfoItem> se;
    private JLabel pageNumLabel;
    private InfoItemsPage<InfoItem> itp;
    private ArrayDeque<Page> pageStack = new ArrayDeque<>();
    private Page currentPage;

    public SearchResultPage(
        MainViewPort mainViewPort,
        ListExtractor<InfoItem> se
    ) {
        this(mainViewPort);
        this.se = se;
        nextBtn.setEnabled(false);
        preBtn.setEnabled(false);
        Thread.startVirtualThread(() -> {
            try {
                itp = se.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        });
    }

    public SearchResultPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;

        resultListPanel = new JPanel();
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        pageNumLabel = new JLabel("1", SwingConstants.CENTER);
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICON);

        this.setLayout(new BorderLayout());
        resultListPanel.setLayout(
            new BoxLayout(resultListPanel, BoxLayout.Y_AXIS)
        );
        pageNevPanel.setBackground(IconRes.YOUTUBE_COLOUR);
        preBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        preBtn.addActionListener(this::onPreBtnPressed);
        nextBtn.addActionListener(this::onNextBtnPressed);

        pageNumLabel.setEnabled(false);
        nextBtn.setEnabled(false);
        this.add(resultListPanel, BorderLayout.CENTER);
        pageNevPanel.add(preBtn);
        pageNevPanel.add(pageNumLabel);
        pageNevPanel.add(nextBtn);
        this.add(pageNevPanel, BorderLayout.SOUTH);
    }

    private void onPreBtnPressed(ActionEvent e) {
        resultListPanel.removeAll();
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        Thread.startVirtualThread(this::onPreBtnPressed);
    }

    public void onPreBtnPressed() {
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
    }

    private void onNextBtnPressed(ActionEvent e) {
        resultListPanel.removeAll();
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        Thread.startVirtualThread(this::onNextBtnPressed);
    }

    private void onNextBtnPressed() {
        try {
            Page nextPage = itp.getNextPage();
            itp = se.getPage(nextPage);
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

    public void search(String query, Runnable finaly) {
        resultListPanel.removeAll();
        pageStack.clear();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = null;
        Thread.startVirtualThread(() -> {
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
        });
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

    public void clearResult() {
        resultListPanel.removeAll();
        System.gc();
    }
}
