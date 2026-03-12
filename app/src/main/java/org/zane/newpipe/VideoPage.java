package org.zane.newpipe;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.MediaSlavePriority;
import uk.co.caprica.vlcj.media.MediaSlaveType;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPage extends JPanel {

    private final App app;
    private CallbackMediaPlayerComponent mediaPlayerComponent;
    private JPanel videoContol;
    private JComboBox videoQ;
    private DefaultComboBoxModel<VideoStream> videoQM;
    private JComboBox audioQ;
    private DefaultComboBoxModel<AudioStream> audioQM;
    private AudioStream currentAudioStream;
    private VideoStream currentVideoStream;
    private boolean isEnableComboxEvent;
    private JSlider playbackSlider;
    private boolean isPositionChanged = false;
    private JLabel currentTimestampLabel;
    private JLabel videoLenghtLabel;
    private JLabel videoTitle;
    private long currentTimestamp = 0;
    private JLabel uplodoaderName;
    private String channelURL;
    private double videoLengthDiff;
    private JLabel uplodoaderSubCountLabel;
    private FlatSVGIcon playIcom;
    private FlatSVGIcon pauseIcom;
    private JButton playButton;
    private JImage uploaderAvatar;

    public VideoPage(App app) {
        this.app = app;
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.addComponentListener(
            new ComponentListener() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    // Code to execute when the JPanel is hidden
                    //
                    mediaPlayerComponent.mediaPlayer().controls().stop();
                    mediaPlayerComponent.mediaPlayer().release();
                }

                @Override
                public void componentShown(ComponentEvent e) {}

                public void componentMoved(ComponentEvent e) {}

                public void componentResized(ComponentEvent e) {}
            }
        );
        mediaPlayerComponent = new CallbackMediaPlayerComponent();
        final MediaPlayer mediaPlayer = mediaPlayerComponent.mediaPlayer();
        mediaPlayerComponent.setPreferredSize(new Dimension(500, 500));
        mediaPlayer
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());
        this.add(mediaPlayerComponent);
        // c.gridy = 1;

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
        this.add(playbackSlider);
        //c.gridy = 2;
        videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.add(videoContol);
        try {
            playIcom = new FlatSVGIcon(
                getClass().getResourceAsStream("/icon/ic_play_arrow.svgz")
            );
            pauseIcom = new FlatSVGIcon(
                getClass().getResourceAsStream("/icon/ic_pause.svg")
            );
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        playButton = new JButton(playIcom);
        playButton.addActionListener(e -> {
            if (mediaPlayer.status().isPlaying()) {
                mediaPlayer.controls().pause();
            } else {
                mediaPlayer.controls().play();
            }
        });
        videoContol.add(playButton);
        currentTimestampLabel = new JLabel("00:00");
        videoContol.add(currentTimestampLabel);
        videoLenghtLabel = new JLabel("/--:--:--");
        videoContol.add(videoLenghtLabel);
        videoQM = new DefaultComboBoxModel<>();
        videoQ = new JComboBox<VideoStream>(videoQM);
        videoQ.setRenderer(new VideoComboBoxRenderer());
        videoQ.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof VideoStream videoStream) {
                    MediaApi media = mediaPlayer.media();
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    media.prepare(
                        videoStream.getContent(),
                        String.format(":start-time=%.3f", currentTime / 1000.0)
                    );

                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            currentAudioStream.getContent()
                        );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentVideoStream = videoStream;
                }
            }
        });
        videoContol.add(videoQ);
        audioQM = new DefaultComboBoxModel<>();
        audioQ = new JComboBox<AudioStream>(audioQM);
        audioQ.setRenderer(new AudioComboBoxRenderer());
        audioQ.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof AudioStream audioStream) {
                    MediaApi media = mediaPlayer.media();
                    boolean isPlaying = mediaPlayer.status().isPlaying();
                    final long currentTime = mediaPlayer.status().time();
                    media.prepare(
                        currentVideoStream.getContent(),
                        String.format(":start-time=%.3f", currentTime / 1000.0)
                    );

                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            audioStream.getContent()
                        );
                    if (isPlaying) {
                        mediaPlayer.controls().play();
                    }
                    currentAudioStream = audioStream;
                }
            }
        });
        videoContol.add(audioQ);
        JPanel videoTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        videoTitle = new JLabel("", SwingConstants.LEFT);
        Font currentFont = videoTitle.getFont();
        videoTitle.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        videoTitlePanel.add(videoTitle);
        this.add(videoTitlePanel);
        JPanel videoInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel uploaderInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel uploaderSubInfo = new JPanel(new GridLayout(2, 1));
        uploaderAvatar = new JImage();
        uplodoaderName = new JLabel("", SwingConstants.LEFT);
        uplodoaderName.setFont(
            currentFont.deriveFont(Font.BOLD, currentFont.getSize())
        );
        uploaderAvatar.setMaximumSize(new Dimension(100, 100));
        uplodoaderSubCountLabel = new JLabel("", SwingConstants.LEFT);
        uplodoaderSubCountLabel.setForeground(Color.LIGHT_GRAY);
        uploaderInfo.addMouseListener(new PanelClickListener());
        uploaderInfo.add(uploaderAvatar);
        uploaderSubInfo.add(uplodoaderName);
        uploaderSubInfo.add(uplodoaderSubCountLabel);
        uploaderInfo.add(uploaderSubInfo);
        videoInfo.add(uploaderInfo);
        this.add(videoInfo);
    }

    public void showVideo(String videoUrl) {
        new Thread(() -> {
            try {
                StreamExtractor streamExtractor =
                    ServiceList.YouTube.getStreamExtractor(videoUrl);
                streamExtractor.fetchPage();
                channelURL = streamExtractor.getUploaderUrl();
                videoTitle.setText(streamExtractor.getName());
                uplodoaderName.setText(streamExtractor.getUploaderName());
                List<org.schabi.newpipe.extractor.Image> avatars =
                    streamExtractor.getUploaderAvatars();
                if (!avatars.isEmpty()) {
                    try {
                        BufferedImage image = ImageIO.read(
                            new URI(avatars.get(0).getUrl()).toURL()
                        );
                        uploaderAvatar.setImage(image);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                uplodoaderSubCountLabel.setText(
                    CommonUtil.numberToStringUnit(
                            streamExtractor.getUploaderSubscriberCount()
                        ) +
                        " subscribers"
                );
                List<VideoStream> videoStreams =
                    streamExtractor.getVideoOnlyStreams();
                currentVideoStream = videoStreams.get(0);
                List<AudioStream> audioStreams =
                    streamExtractor.getAudioStreams();
                currentAudioStream = audioStreams.get(0);
                videoQM.addAll(videoStreams);
                audioQM.addAll(audioStreams);
                String timeString =
                    "/" + CommonUtil.getTimeString(streamExtractor.getLength());
                //System.out.println("audi size " + audioStreams.size());

                SwingUtilities.invokeLater(() -> {
                    videoLenghtLabel.setText(timeString);
                    isEnableComboxEvent = false;
                    videoQM.setSelectedItem(currentVideoStream);
                    audioQM.setSelectedItem(currentAudioStream);
                    isEnableComboxEvent = true;
                    MediaPlayer MediaPlayer =
                        mediaPlayerComponent.mediaPlayer();
                    MediaApi media = MediaPlayer.media();
                    media.prepare(currentVideoStream.getContent());
                    media
                        .slaves()
                        .add(
                            MediaSlaveType.AUDIO,
                            MediaSlavePriority.HIGH,
                            currentAudioStream.getContent()
                        );
                    MediaPlayer.controls().play();
                });
            } catch (ExtractionException | IOException e) {
                e.printStackTrace();
            }
        })
            .start();
    }

    public void stop() {
        mediaPlayerComponent.mediaPlayer().controls().stop();
    }

    public class VideoComboBoxRenderer extends DefaultListCellRenderer {

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

    public class AudioComboBoxRenderer extends DefaultListCellRenderer {

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

    public class MyMediaPlayerEventListener
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
            playButton.setIcon(pauseIcom);
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
        }

        @Override
        public void forward(MediaPlayer mediaPlayer) {}

        @Override
        public void backward(MediaPlayer mediaPlayer) {}

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            playButton.setIcon(playIcom);
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

    private class PanelClickListener implements MouseListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            // Your "onclick" logic goes here
            app.nevigate(App.Page.CHANNEL, channelURL);
        }

        // Other MouseListener methods (must be implemented, even if empty)
        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {
            e.getComponent().setBackground(Color.DARK_GRAY);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            e.getComponent().setBackground(Color.BLACK);
        }
    }
}
