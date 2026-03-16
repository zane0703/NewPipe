package org.zane.newpipe.page;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.zane.newpipe.page.MainViewPort.NevigateOpation;
import org.zane.newpipe.ui.ChannelInfoPanel;
import org.zane.newpipe.ui.CommentItemPanel;
import org.zane.newpipe.ui.IconRes;
import org.zane.newpipe.ui.JHTMLPane;
import org.zane.newpipe.ui.JImage;
import org.zane.newpipe.ui.SearchItemPanel;
import org.zane.newpipe.util.CommonUtil;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.MediaSlavePriority;
import uk.co.caprica.vlcj.media.MediaSlaveType;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPage extends JPanel {

    private JPanel videoContol;
    private JComboBox videoComboBox;
    private DefaultComboBoxModel<VideoStream> videoModel;
    private JComboBox audioComboBox;
    private DefaultComboBoxModel<AudioStream> audioModel;
    private JComboBox subtitleComboBox;
    private DefaultComboBoxModel<SubtitlesStream> subtitleModel;
    private AudioStream currentAudioStream;
    private VideoStream currentVideoStream;
    private SubtitlesStream currentSubtitlesStream;
    private boolean isEnableComboxEvent;
    private JSlider playbackSlider;
    private boolean isPositionChanged = false;
    private JLabel currentTimestampLabel;
    private JLabel videoLenghtLabel;
    private JLabel videoTitle;
    private long currentTimestamp = 0;
    private double videoLengthDiff;
    private JButton playButton;
    private MediaPlayer mediaPlayer;
    private JLabel publishDateLabel;
    private JEditorPane videoDescriptionText;
    private JPanel videoCommentPanel;
    private JLabel viewCountLabel;
    private JLabel likeCountLabel;
    private NumberFormat numberFormat;
    private int currentSpeed = 100;
    private DecimalFormat df = new DecimalFormat("0.##");
    private BigDecimal div = new BigDecimal(100);
    private JPanel relatedStreamsPanel;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern(
        "dd MMMM yyyy HH:mm:ss"
    );
    private ChannelInfoPanel uploaderInfo;
    private String videoId;
    private MainViewPort mainViewPort;

    public VideoPage(MainViewPort mainViewPort) {
        this.mainViewPort = mainViewPort;
        Class pageClass = getClass();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        numberFormat = NumberFormat.getInstance();
        CallbackMediaPlayerComponent mediaPlayerComponent =
            new CallbackMediaPlayerComponent();
        mediaPlayer = mediaPlayerComponent.mediaPlayer();
        this.addComponentListener(
            new ComponentListener() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    // Code to execute when the JPanel is hidden
                    //
                    mediaPlayer.controls().stop();
                    mediaPlayer.release();
                }

                @Override
                public void componentShown(ComponentEvent e) {}

                public void componentMoved(ComponentEvent e) {}

                public void componentResized(ComponentEvent e) {
                    videoDescriptionText.setMaximumSize(
                        new Dimension(
                            getPreferredSize().width,
                            Integer.MAX_VALUE
                        )
                    );
                }
            }
        );

        mediaPlayerComponent.setPreferredSize(new Dimension(500, 500));
        mediaPlayer
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());
        this.add(mediaPlayerComponent);
        // c.gridy = 1;
        JPanel playbackSliderRow = new JPanel(new BorderLayout());

        currentTimestampLabel = new JLabel("00:00");
        playbackSliderRow.add(currentTimestampLabel, BorderLayout.LINE_START);
        playbackSlider = new JSlider(
            SwingConstants.HORIZONTAL,
            0,
            Integer.MAX_VALUE,
            0
        );
        playbackSlider.addChangeListener(e -> {
            if (playbackSlider.getValueIsAdjusting() || isPositionChanged) {
                return;
            }
            System.out.println("change timestamp");
            mediaPlayer
                .controls()
                .setTime((long) (playbackSlider.getValue() * videoLengthDiff));
        });
        playbackSliderRow.add(playbackSlider, BorderLayout.CENTER);

        videoLenghtLabel = new JLabel("-:--:--");
        playbackSliderRow.add(videoLenghtLabel, BorderLayout.LINE_END);
        this.add(playbackSliderRow);

        // video Contol
        videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.add(videoContol);
        playButton = new JButton(IconRes.PLAY_ARROW_ICOM);
        playButton.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            } else {
                mediaPlayer.controls().play();
            }
        });
        videoContol.add(playButton);

        videoModel = new DefaultComboBoxModel<>();

        //ComboBox
        videoComboBox = new JComboBox<VideoStream>(videoModel);
        videoComboBox.setRenderer(new VideoComboBoxRenderer());
        videoComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof VideoStream videoStream) {
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    playVideo(
                        videoStream,
                        currentAudioStream,
                        currentSubtitlesStream,
                        currentTime
                    );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentVideoStream = videoStream;
                }
            }
        });
        videoContol.add(new JLabel("Video:"));
        videoContol.add(videoComboBox);
        audioModel = new DefaultComboBoxModel<>();
        audioComboBox = new JComboBox<AudioStream>(audioModel);
        audioComboBox.setRenderer(new AudioComboBoxRenderer());
        audioComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof AudioStream audioStream) {
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    playVideo(
                        currentVideoStream,
                        audioStream,
                        currentSubtitlesStream,
                        currentTime
                    );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentAudioStream = audioStream;
                }
            }
        });
        videoContol.add(new JLabel("Audio:"));
        videoContol.add(audioComboBox);
        subtitleModel = new DefaultComboBoxModel<SubtitlesStream>();
        subtitleComboBox = new JComboBox<SubtitlesStream>(subtitleModel);
        subtitleComboBox.setRenderer(new SubTitleComboBoxRenderer());
        subtitleComboBox.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                boolean isPlaying = mediaPlayer.status().isPlaying();
                final long currentTime = mediaPlayer.status().time();
                if (e.getItem() instanceof SubtitlesStream subtitlesStream) {
                    playVideo(
                        currentVideoStream,
                        currentAudioStream,
                        subtitlesStream,
                        currentTime
                    );

                    currentSubtitlesStream = subtitlesStream;
                } else {
                    playVideo(
                        currentVideoStream,
                        currentAudioStream,
                        null,
                        currentTime
                    );
                    currentSubtitlesStream = null;
                }
                if (isPlaying) {
                    mediaPlayer.controls().play();
                }
            }
        });
        videoContol.add(new JLabel("Subtitle:"));
        videoContol.add(subtitleComboBox);
        JButton speedBtm = new JButton("1x");
        speedBtm.addActionListener(e -> {
            SpeedSelectorPanel s = new SpeedSelectorPanel();
            JButton resetBtn = new JButton("Reset");
            resetBtn.addActionListener(e2 -> s.reset());
            Object[] options = new Object[] { resetBtn, "Ok", "Cancel" };
            if (
                JOptionPane.showOptionDialog(
                    this,
                    s,
                    "Playback speed option",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[2]
                ) ==
                1
            ) {
                currentSpeed = s.getSpeed();
                BigDecimal speedDec = new BigDecimal(currentSpeed).divide(div);
                speedBtm.setText(df.format(speedDec) + "x");
                mediaPlayer.controls().setRate(speedDec.floatValue());
            }
        });
        videoContol.add(speedBtm);

        JPanel videoTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        videoTitle = new JLabel("", SwingConstants.LEFT);
        Font currentFont = videoTitle.getFont();
        videoTitle.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        videoTitlePanel.add(videoTitle);
        this.add(videoTitlePanel);
        JPanel videoInfo = new JPanel(new GridLayout(1, 2));
        uploaderInfo = new ChannelInfoPanel(mainViewPort);
        videoInfo.add(uploaderInfo);
        JPanel likeAndViewPanelPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT)
        );
        JPanel likeAndViewPanel = new JPanel();

        likeAndViewPanel.setLayout(
            new BoxLayout(likeAndViewPanel, BoxLayout.Y_AXIS)
        );
        viewCountLabel = new JLabel("", SwingConstants.RIGHT);
        likeAndViewPanel.add(viewCountLabel);
        likeCountLabel = new JLabel(
            "",
            IconRes.THUMP_UP_ICOM,
            SwingConstants.RIGHT
        );
        likeAndViewPanel.add(likeCountLabel);
        likeAndViewPanelPanel.add(likeAndViewPanel);
        videoInfo.add(likeAndViewPanelPanel);
        this.add(videoInfo);
        relatedStreamsPanel = new JPanel();
        relatedStreamsPanel.setLayout(
            new BoxLayout(relatedStreamsPanel, BoxLayout.Y_AXIS)
        );
        videoCommentPanel = new JPanel();
        videoCommentPanel.setLayout(
            new BoxLayout(videoCommentPanel, BoxLayout.Y_AXIS)
        );
        videoCommentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel videoDescriptionPanel = new JPanel();
        videoDescriptionPanel.setLayout(
            new BoxLayout(videoDescriptionPanel, BoxLayout.Y_AXIS)
        );
        JPanel publishDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        publishDateLabel = new JLabel();
        publishDatePanel.add(publishDateLabel);
        videoDescriptionPanel.add(publishDatePanel);
        JPanel videoDescriptionTextPanel = new JPanel(new BorderLayout());
        videoDescriptionText = new JHTMLPane();
        videoDescriptionText.addHyperlinkListener(e -> {
            try {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    URL url = e.getURL();
                    if (url.getHost().toLowerCase().equals("www.youtube.com")) {
                        System.out.println(url.getPath());
                        String[] paths = url.getPath().split("/");
                        switch (paths.length) {
                            case 2:
                                if (paths[1].equals("watch")) {
                                    Map<String, String> query =
                                        CommonUtil.getQueryMap(url.getQuery());
                                    String v = query.get("v");
                                    if (v != null) {
                                        if (v.equals(videoId)) {
                                            String t = query.get("t");
                                            long newTime = Long.parseLong(t);
                                            mediaPlayer
                                                .controls()
                                                .setTime(newTime * 1000);
                                        } else {
                                            mainViewPort.nevigate(
                                                new NevigateOpation(
                                                    MainViewPort.Page.VIDEO,
                                                    url.toString()
                                                )
                                            );
                                        }
                                    }
                                }
                                break;
                            case 3:
                                if (paths[1].equals("hashtag")) {
                                    mainViewPort.nevigate(
                                        new NevigateOpation(
                                            MainViewPort.Page.SEARCH,
                                            "#" + paths[2]
                                        )
                                    );
                                }
                                break;
                        }
                    } else if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(url.toURI());
                    }
                }
            } catch (ParseException | IOException | URISyntaxException pe) {
                pe.printStackTrace();
            }
        });
        videoDescriptionText.setMaximumSize(
            new Dimension(getPreferredSize().width, Integer.MAX_VALUE)
        );
        videoDescriptionTextPanel.add(videoDescriptionText, BorderLayout.NORTH);
        videoDescriptionPanel.add(videoDescriptionTextPanel);
        JPanel navigationBar = new JPanel(new GridLayout(1, 3));
        JViewport viewport = new JViewport();
        JButton commentBtn = new JButton(IconRes.COMMENT_ICOM);
        commentBtn.addActionListener(e -> viewport.setView(videoCommentPanel));
        navigationBar.add(commentBtn);
        JButton relatedStreamsBtn = new JButton(IconRes.ART_TRACK_ICOM);
        relatedStreamsBtn.addActionListener(e ->
            viewport.setView(relatedStreamsPanel)
        );
        navigationBar.add(relatedStreamsBtn);
        JButton descriptionBtn = new JButton(IconRes.DESCRIPTION_ICOM);
        descriptionBtn.addActionListener(e ->
            viewport.setView(videoDescriptionPanel)
        );
        navigationBar.add(descriptionBtn);
        this.add(navigationBar);

        viewport.setView(relatedStreamsPanel);
        this.add(viewport);
    }

    public void showVideo(String videoUrl) {
        new Thread(() -> {
            try {
                StreamExtractor streamExtractor =
                    ServiceList.YouTube.getStreamExtractor(videoUrl);
                streamExtractor.fetchPage();
                videoId = streamExtractor.getId();
                videoTitle.setText(streamExtractor.getName());
                List<org.schabi.newpipe.extractor.Image> avatars =
                    streamExtractor.getUploaderAvatars();
                if (!avatars.isEmpty()) {
                    uploaderInfo.setChanelAvatar(avatars.get(0).getUrl());
                }
                uploaderInfo.setInfo(
                    streamExtractor.getUploaderName(),
                    streamExtractor.getUploaderSubscriberCount(),
                    streamExtractor.getUploaderUrl()
                );
                List<VideoStream> videoStreams =
                    streamExtractor.getVideoOnlyStreams();
                List<AudioStream> audioStreams =
                    streamExtractor.getAudioStreams();
                List<SubtitlesStream> subtitlesStreams =
                    streamExtractor.getSubtitlesDefault();
                subtitlesStreams.addFirst(null);
                currentVideoStream = videoStreams.get(0);
                currentAudioStream = audioStreams.get(0);
                currentSubtitlesStream = null;
                videoModel.removeAllElements();
                audioModel.removeAllElements();
                videoModel.addAll(videoStreams);
                audioModel.addAll(audioStreams);
                subtitleModel.addAll(subtitlesStreams);

                String timeString = CommonUtil.getTimeString(
                    streamExtractor.getLength()
                );
                String viewCountString =
                    numberFormat.format(streamExtractor.getViewCount()) +
                    " views";
                String likeCountString = CommonUtil.numberToStringUnit(
                    streamExtractor.getLikeCount()
                );
                //System.out.println("audi size " + audioStreams.size());
                String UploadDateString =
                    "Published on " +
                    dtf.format(
                        streamExtractor.getUploadDate().getLocalDateTime()
                    );

                String videoDescriptionString = streamExtractor
                    .getDescription()
                    .getContent();
                SwingUtilities.invokeLater(() -> {
                    videoLenghtLabel.setText(timeString);
                    publishDateLabel.setText(UploadDateString);
                    try {
                        videoDescriptionText.setText(videoDescriptionString);
                        relatedStreamsPanel.removeAll();
                        for (InfoItem item : streamExtractor
                            .getRelatedItems()
                            .getItems()) {
                            relatedStreamsPanel.add(
                                new SearchItemPanel(mainViewPort, item)
                            );
                        }
                    } catch (
                        ExtractionException
                        | IOException
                        | URISyntaxException e
                    ) {
                        e.printStackTrace();
                    }
                    isEnableComboxEvent = false;
                    videoModel.setSelectedItem(currentVideoStream);
                    audioModel.setSelectedItem(currentAudioStream);
                    subtitleModel.setSelectedItem(null);
                    isEnableComboxEvent = true;
                    viewCountLabel.setText(viewCountString);
                    likeCountLabel.setText(likeCountString);
                    playVideo(currentVideoStream, currentAudioStream, null, 0);
                    mediaPlayer.controls().play();
                });
                CommentsExtractor commentsExtractor =
                    ServiceList.YouTube.getCommentsExtractor(videoUrl);
                commentsExtractor.fetchPage();
                InfoItemsPage<CommentsInfoItem> cInfoItemsPage =
                    commentsExtractor.getInitialPage();
                List<CommentsInfoItem> clist = cInfoItemsPage.getItems();
                videoCommentPanel.removeAll();
                for (CommentsInfoItem cit : clist) {
                    videoCommentPanel.add(
                        new CommentItemPanel(
                            cit,
                            commentsExtractor,
                            mainViewPort
                        )
                    );
                }
            } catch (ExtractionException | IOException e) {
                e.printStackTrace();
            }
        })
            .start();
    }

    public void stop() {
        mediaPlayer.controls().stop();
    }

    public void playVideo(
        VideoStream videoStream,
        AudioStream audioStream,
        SubtitlesStream subtitlesStream,
        long currentTime
    ) {
        MediaApi mediaApi = mediaPlayer.media();
        mediaApi.prepare(
            videoStream.getContent(),
            String.format(":start-time=%.3f", currentTime / 1000.0)
        );

        mediaApi
            .slaves()
            .add(
                MediaSlaveType.AUDIO,
                MediaSlavePriority.HIGH,
                currentAudioStream.getContent()
            );
        if (subtitlesStream != null) {
            //System.out.println(subtitlesStream.getContent());
            mediaPlayer
                .subpictures()
                .setSubTitleUri(subtitlesStream.getContent());
        }
    }

    private class VideoComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );
            if (value instanceof VideoStream videoStream) {
                String codec = videoStream.getCodec();
                int dotIndex = codec.indexOf('.');
                if (dotIndex > -1) {
                    codec = codec.subSequence(0, dotIndex).toString();
                }
                setText(
                    codec +
                        " " +
                        videoStream.getResolution() +
                        " " +
                        videoStream.getFps() +
                        "FPS"
                );
            }
            return this;
        }
    }

    private class AudioComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );
            if (value instanceof AudioStream audioStream) {
                String codec = audioStream.getCodec();
                int dotIndex = codec.indexOf('.');
                if (dotIndex > -1) {
                    codec = codec.subSequence(0, dotIndex).toString();
                }
                setText(
                    codec +
                        " " +
                        CommonUtil.numberToStringUnit(
                            audioStream.getBitrate()
                        ) +
                        "bps"
                );
            }
            return this;
        }
    }

    private class SubTitleComboBoxRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );
            if (value instanceof SubtitlesStream subtitlesStream) {
                setText(subtitlesStream.getDisplayLanguageName());
            } else if (value == null) {
                setText("None");
            }
            return this;
        }
    }

    private class MyMediaPlayerEventListener
        implements MediaPlayerEventListener
    {

        @Override
        public void mediaChanged(MediaPlayer mediaPlayer, MediaRef media) {}

        @Override
        public void opening(MediaPlayer mediaPlayer) {}

        @Override
        public void buffering(MediaPlayer mediaPlayer, float newCache) {}

        @Override
        public void playing(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PAUSE_ICOM);
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICOM);
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICOM);
        }

        @Override
        public void forward(MediaPlayer mediaPlayer) {}

        @Override
        public void backward(MediaPlayer mediaPlayer) {}

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            playButton.setIcon(IconRes.PLAY_ARROW_ICOM);
        }

        @Override
        public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
            long newTimeSec = newTime / 1000;
            if (newTimeSec != currentTimestamp) {
                String newTimeString = CommonUtil.getTimeString(newTime / 1000);
                SwingUtilities.invokeLater(() -> {
                    currentTimestampLabel.setText(newTimeString);
                });
            }

            int newTimeSlid = (int) (newTime / videoLengthDiff);
            SwingUtilities.invokeLater(() -> {
                if (playbackSlider.getValueIsAdjusting()) {
                    return;
                }
                isPositionChanged = true;
                playbackSlider.setValue(newTimeSlid);
                isPositionChanged = false;
            });
        }

        @Override
        public void positionChanged(
            MediaPlayer mediaPlayer,
            float newPosition
        ) {
            // if (playbackSlider.getValueIsAdjusting()) {
            //     return;
            // }
            // isPositionChanged = true;
            // playbackSlider.setValue((int) (newPosition * 100));
            // SwingUtilities.invokeLater(() -> {
            //     isPositionChanged = false;
            // });
        }

        @Override
        public void seekableChanged(MediaPlayer mediaPlayer, int newSeekable) {}

        @Override
        public void pausableChanged(MediaPlayer mediaPlayer, int newPausable) {}

        @Override
        public void titleChanged(MediaPlayer mediaPlayer, int newTitle) {}

        @Override
        public void snapshotTaken(MediaPlayer mediaPlayer, String filename) {}

        @Override
        public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {}

        @Override
        public void videoOutput(MediaPlayer mediaPlayer, int newCount) {}

        @Override
        public void scrambledChanged(
            MediaPlayer mediaPlayer,
            int newScrambled
        ) {}

        @Override
        public void elementaryStreamAdded(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void elementaryStreamDeleted(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void elementaryStreamSelected(
            MediaPlayer mediaPlayer,
            TrackType type,
            int id
        ) {}

        @Override
        public void corked(MediaPlayer mediaPlayer, boolean corked) {}

        @Override
        public void muted(MediaPlayer mediaPlayer, boolean muted) {}

        @Override
        public void volumeChanged(MediaPlayer mediaPlayer, float volume) {}

        @Override
        public void audioDeviceChanged(
            MediaPlayer mediaPlayer,
            String audioDevice
        ) {}

        @Override
        public void chapterChanged(MediaPlayer mediaPlayer, int newChapter) {}

        @Override
        public void error(MediaPlayer mediaPlayer) {}

        @Override
        public void mediaPlayerReady(MediaPlayer mediaPlayer) {
            videoLengthDiff =
                ((double) mediaPlayer.status().length()) /
                ((double) Integer.MAX_VALUE);
        }
    }

    private class SpeedSelectorPanel extends JPanel {

        private int speed;
        private JSlider slider;

        public SpeedSelectorPanel() {
            setLayout(new BorderLayout());
            speed = currentSpeed;
            JLabel speedLanel = new JLabel(
                df.format(new BigDecimal(currentSpeed).divide(div)) + "x",
                SwingConstants.CENTER
            );
            JPanel speedLabelPanel = new JPanel(new BorderLayout());
            speedLabelPanel.add(new JLabel("0.1x"), BorderLayout.LINE_START);
            speedLabelPanel.add(speedLanel, BorderLayout.CENTER);
            speedLabelPanel.add(new JLabel("10x"), BorderLayout.LINE_END);
            JPanel subPanel = new JPanel();
            subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
            JButton subBtn = new JButton("-25%");
            slider = new JSlider(10, 1000, currentSpeed);
            JButton addBtn = new JButton("+25%");
            this.add(subBtn, BorderLayout.LINE_START);
            subPanel.add(speedLabelPanel);
            subPanel.add(slider);
            this.add(addBtn, BorderLayout.LINE_END);
            subBtn.addActionListener(e -> slider.setValue(speed - 25));
            addBtn.addActionListener(e -> slider.setValue(speed + 25));
            slider.addChangeListener(e -> {
                speed = slider.getValue();
                speedLanel.setText(
                    df.format(new BigDecimal(speed).divide(div)) + "x"
                );
            });

            this.add(subPanel);
        }

        public void reset() {
            slider.setValue(100);
        }

        public int getSpeed() {
            return speed;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int maxSize = Math.min(size.width, mainViewPort.getWidth());
        return new Dimension(maxSize, size.height);
    }
}
