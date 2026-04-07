package org.zane.newpipe.ui;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.zane.newpipe.page.MainViewPort;

public class CommentPanel extends JPanel {

    private MainViewPort mainViewPort;
    private JPanel mainCommentPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private JLabel pageNumLabel;
    private CommentsExtractor ce;
    private ArrayDeque<Page> pageStack;
    private InfoItemsPage<CommentsInfoItem> itp;
    private Page currentPage;
    private boolean isReplay;
    private HyperlinkListener hyperlinkListener;
    private JViewport viewport;

    public CommentPanel(
        MainViewPort mainViewPort,
        JViewport viewport,
        HyperlinkListener hyperlinkListener,
        CommentsExtractor ce,
        Page page
    ) {
        this(mainViewPort, viewport, hyperlinkListener);
        isReplay = true;
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = page;
        this.ce = ce;

        Thread.startVirtualThread(() -> {
            try {
                itp = ce.getPage(page);
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        });
    }

    public CommentPanel(
        MainViewPort mainViewPort,
        HyperlinkListener hyperlinkListener
    ) {
        this(mainViewPort, mainViewPort, hyperlinkListener);
    }

    public CommentPanel(
        MainViewPort mainViewPort,
        JViewport viewport,
        HyperlinkListener hyperlinkListener
    ) {
        super(new BorderLayout());
        this.mainViewPort = mainViewPort;
        this.viewport = viewport;
        this.hyperlinkListener = hyperlinkListener;
        isReplay = false;
        pageStack = new ArrayDeque<>();
        mainCommentPanel = new JPanel();
        mainCommentPanel.setLayout(
            new BoxLayout(mainCommentPanel, BoxLayout.Y_AXIS)
        );
        this.add(mainCommentPanel, BorderLayout.CENTER);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(IconRes.YOUTUBE_COLOUR);
        preBtn = new JButton(IconRes.ARROW_BACK_ICON);
        preBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        preBtn.addActionListener(e -> {
            mainCommentPanel.removeAll();
            Thread.startVirtualThread(() -> {
                try {
                    if (pageStack.isEmpty()) {
                        itp = ce.getInitialPage();
                        showPage();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText("1");
                            preBtn.setEnabled(false);
                        });
                        currentPage = null;
                    } else {
                        Page prePage = pageStack.pop();
                        itp = ce.getPage(prePage);
                        showPage();
                        int stackSize = pageStack.size();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText(
                                Integer.toString(stackSize + (isReplay ? 1 : 2))
                            );
                        });
                        if (isReplay && stackSize < 1) {
                            preBtn.setEnabled(false);
                        }
                        currentPage = prePage;
                    }
                } catch (ExtractionException | IOException err) {
                    err.printStackTrace();
                }
            });
        });
        pageNevPanel.add(preBtn);
        pageNumLabel = new JLabel("1", SwingConstants.CENTER);
        pageNevPanel.add(pageNumLabel);
        nextBtn = new JButton(IconRes.ARROW_NEXT_ICON);
        nextBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextBtn.addActionListener(e -> {
            mainCommentPanel.removeAll();
            Thread.startVirtualThread(() -> {
                try {
                    Page nextPage = itp.getNextPage();
                    itp = ce.getPage(nextPage);
                    showPage();
                    if (currentPage != null) {
                        pageStack.push(currentPage);
                    }

                    SwingUtilities.invokeLater(() -> {
                        pageNumLabel.setText(
                            Integer.toString(
                                pageStack.size() + (isReplay ? 1 : 2)
                            )
                        );
                        preBtn.setEnabled(true);
                    });
                    currentPage = nextPage;
                } catch (ExtractionException | IOException err) {
                    err.printStackTrace();
                }
            });
        });
        pageNevPanel.add(nextBtn);
        this.add(pageNevPanel, BorderLayout.SOUTH);
    }

    public void fetchComment(String videoURL) {
        mainCommentPanel.removeAll();
        pageStack.clear();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        currentPage = null;
        Thread.startVirtualThread(() -> {
            try {
                ce = ServiceList.YouTube.getCommentsExtractor(videoURL);
                ce.fetchPage();
                itp = ce.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        });
    }

    private void showPage() {
        List<CommentsInfoItem> clist = itp.getItems();
        for (CommentsInfoItem cit : clist) {
            CommentItemPanel commentItemPanel = new CommentItemPanel(
                mainViewPort,
                viewport,
                hyperlinkListener,
                cit,
                ce
            );
            SwingUtilities.invokeLater(() -> {
                mainCommentPanel.add(commentItemPanel);
            });
        }
        SwingUtilities.invokeLater(() -> {
            mainCommentPanel.updateUI();
            this.updateUI();
            nextBtn.setEnabled(itp.hasNextPage());
        });
    }

    public void clear() {
        mainCommentPanel.removeAll();
    }
}
