package org.zane.newpipe;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.List;
import javax.swing.*;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.timeago.patterns.sl;
import uk.co.caprica.vlcj.media.MediaRef;
import uk.co.caprica.vlcj.media.MediaSlavePriority;
import uk.co.caprica.vlcj.media.MediaSlaveType;
import uk.co.caprica.vlcj.media.SlaveApi;
import uk.co.caprica.vlcj.media.TrackType;
import uk.co.caprica.vlcj.player.base.MediaApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.base.callback.AudioCallbackAdapter;
import uk.co.caprica.vlcj.player.base.events.MediaPlayerEvent;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public class VideoPage extends JPanel {

    private final App app;
    private CallbackMediaPlayerComponent mediaPlayerComponent;
    private JPanel videoContol;
    private boolean isStop = false;
    private JComboBox videoQ;
    private DefaultComboBoxModel<VideoStream> videoQM;
    private JComboBox audioQ;
    private DefaultComboBoxModel<AudioStream> audioQM;
    private AudioStream currentAudioStream;
    private VideoStream currentVideoStream;
    private boolean isEnableComboxEvent;
    private JSlider playbackSlider;
    private boolean isPositionChanged = false;

    public VideoPage(App app) {
        this.app = app;
        super(new java.awt.GridBagLayout());
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
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        mediaPlayerComponent = new CallbackMediaPlayerComponent();
        mediaPlayerComponent.setMaximumSize(new Dimension(1000, 500));
        mediaPlayerComponent
            .mediaPlayer()
            .events()
            .addMediaPlayerEventListener(new MyMediaPlayerEventListener());
        this.add(mediaPlayerComponent, c);
        c.gridy = 1;

        playbackSlider = new JSlider(SwingConstants.HORIZONTAL, 0, 100, 0);
        playbackSlider.addMouseListener(
            new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    //mediaPlayerComponent.mediaPlayer().controls().setPosition(playbackSlider.getValue()/100);
                }

                @Override
                public void mousePressed(MouseEvent e) {}

                @Override
                public void mouseReleased(MouseEvent e) {}

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            }
        );

        playbackSlider.addChangeListener(e -> {
            if (playbackSlider.getValueIsAdjusting() || isPositionChanged) {
                return;
            }
            mediaPlayerComponent
                .mediaPlayer()
                .controls()
                .setPosition(playbackSlider.getValue() / 100.0f);
        });
        this.add(playbackSlider, c);
        c.gridy = 2;
        videoContol = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.add(videoContol, c);
        JButton playButton = new JButton("pause");
        videoContol.add(playButton);
        playButton.addActionListener(e -> {
            if (isStop) {
                mediaPlayerComponent.mediaPlayer().controls().play();
                playButton.setText("pause");
            } else {
                mediaPlayerComponent.mediaPlayer().controls().pause();
                playButton.setText("play");
            }
            isStop = !isStop;
        });
        videoQM = new DefaultComboBoxModel<>();
        videoQ = new JComboBox<VideoStream>(videoQM);
        videoQ.setRenderer(new VideoComboBoxRenderer());
        videoQ.addItemListener(e -> {
            if (
                e.getStateChange() == ItemEvent.SELECTED && isEnableComboxEvent
            ) {
                if (e.getItem() instanceof VideoStream videoStream) {
                    MediaPlayer mediaPlayer =
                        mediaPlayerComponent.mediaPlayer();
                    MediaApi media = mediaPlayer.media();
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
                    if (!isStop) {
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
                    MediaPlayer mediaPlayer =
                        mediaPlayerComponent.mediaPlayer();
                    MediaApi media = mediaPlayer.media();
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
                    if (!isStop) {
                        mediaPlayer.controls().play();
                    }
                    currentAudioStream = audioStream;
                }
            }
        });
        videoContol.add(audioQ);
    }

    public void showVideo(String videoUrl) {
        new Thread(() -> {
            try {
                StreamExtractor streamExtractor =
                    ServiceList.YouTube.getStreamExtractor(videoUrl);
                streamExtractor.fetchPage();
                List<VideoStream> videoStreams =
                    streamExtractor.getVideoOnlyStreams();
                currentVideoStream = videoStreams.get(0);
                List<AudioStream> audioStreams =
                    streamExtractor.getAudioStreams();
                currentAudioStream = audioStreams.get(0);
                videoQM.addAll(videoStreams);
                audioQM.addAll(audioStreams);
                System.out.println("audi size " + audioStreams.size());

                SwingUtilities.invokeLater(() -> {
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
                setText(
                    videoStream.getCodec() +
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
                setText(
                    audioStream.getCodec() + " " + audioStream.getBitrate()
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
        public void playing(MediaPlayer mediaPlayer) {}

        @Override
        public void paused(MediaPlayer mediaPlayer) {}

        @Override
        public void stopped(MediaPlayer mediaPlayer) {}

        @Override
        public void forward(MediaPlayer mediaPlayer) {}

        @Override
        public void backward(MediaPlayer mediaPlayer) {}

        @Override
        public void finished(MediaPlayer mediaPlayer) {}

        @Override
        public void timeChanged(MediaPlayer mediaPlayer, long newTime) {}

        @Override
        public void positionChanged(
            MediaPlayer mediaPlayer,
            float newPosition
        ) {
            if (playbackSlider.getValueIsAdjusting()) {
                return;
            }
            isPositionChanged = true;
            playbackSlider.setValue((int) (newPosition * 100));
            SwingUtilities.invokeLater(() -> {
                isPositionChanged = false;
            });
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
        public void mediaPlayerReady(MediaPlayer mediaPlayer) {}
    }
}
