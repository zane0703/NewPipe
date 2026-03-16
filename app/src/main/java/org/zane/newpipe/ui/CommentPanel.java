package org.zane.newpipe.ui;

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
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.zane.newpipe.App;
import org.zane.newpipe.page.MainViewPort;
import org.zane.newpipe.page.MainViewPort.NevigateOpation;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;

public class CommentPanel extends JPanel {

    private MainViewPort mainViewPort;
    private JPanel mainCommentPanel;
    private JButton preBtn;
    private JButton nextBtn;
    private JLabel pageNumLabel;
    private CommentsExtractor ce;
    private Stack<Page> pageStack;
    private InfoItemsPage<CommentsInfoItem> itp;
    private Page currentPage;

    public CommentPanel(
        MainViewPort mainViewPort,
        CommentsExtractor ce,
        Page page
    ) {
        this(mainViewPort);
        pageStack.removeAllElements();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        currentPage = page;
        this.ce = ce;
        new Thread(() -> {
            try {
                itp = ce.getPage(page);
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        })
            .start();
    }

    public CommentPanel(MainViewPort mainViewPort) {
        super(new BorderLayout());
        this.mainViewPort = mainViewPort;
        pageStack = new Stack<>();
        mainCommentPanel = new JPanel();
        mainCommentPanel.setLayout(
            new BoxLayout(mainCommentPanel, BoxLayout.Y_AXIS)
        );
        this.add(mainCommentPanel, BorderLayout.CENTER);
        JPanel pageNevPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pageNevPanel.setBackground(new Color(255, 0, 0));
        preBtn = new JButton(IconRes.ARROW_BACK_ICOM);
        preBtn.addActionListener(e -> {
            mainCommentPanel.removeAll();
            new Thread(() -> {
                try {
                    Page prePage = pageStack.pop();
                    if (prePage == null) {
                        itp = ce.getInitialPage();
                        showPage();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText("1");
                            preBtn.setEnabled(false);
                        });
                    } else {
                        itp = ce.getPage(prePage);
                        showPage();
                        int stackSize = pageStack.size();
                        SwingUtilities.invokeLater(() -> {
                            pageNumLabel.setText(
                                Integer.toString(stackSize + 1)
                            );
                        });
                        if (stackSize < 1) {
                            preBtn.setEnabled(false);
                        }
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
            mainCommentPanel.removeAll();
            new Thread(() -> {
                try {
                    Page nextPage = itp.getNextPage();
                    itp = ce.getPage(nextPage);
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

    public void fetchComment(String videoURL) {
        mainCommentPanel.removeAll();
        pageStack.removeAllElements();
        pageNumLabel.setText("1");
        preBtn.setEnabled(false);
        currentPage = null;
        new Thread(() -> {
            try {
                ce = ServiceList.YouTube.getCommentsExtractor(videoURL);
                ce.fetchPage();
                itp = ce.getInitialPage();
                showPage();
            } catch (IOException | ExtractionException err) {
                err.printStackTrace();
            }
        })
            .start();
    }

    private void showPage() {
        List<CommentsInfoItem> clist = itp.getItems();
        for (CommentsInfoItem cit : clist) {
            CommentItemPanel commentItemPanel = new CommentItemPanel(
                cit,
                ce,
                mainViewPort
            );
            SwingUtilities.invokeLater(() ->
                mainCommentPanel.add(commentItemPanel)
            );
        }
        SwingUtilities.invokeLater(() -> {
            mainCommentPanel.updateUI();
            nextBtn.setEnabled(itp.hasNextPage());
        });
    }
}
