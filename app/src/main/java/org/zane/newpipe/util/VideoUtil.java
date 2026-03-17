package org.zane.newpipe.util;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

public class VideoUtil {

    private static String vlcPath;

    public static void setVlcPath(String vlcPath) {
        VideoUtil.vlcPath = vlcPath;
    }

    public static void openVLC(String videoURL, Component component) {
        new Thread(() -> {
            try {
                if (VideoUtil.vlcPath == null) {
                    return;
                }
                StreamExtractor se = ServiceList.YouTube.getStreamExtractor(
                    videoURL
                );
                se.fetchPage();
                se.getVideoOnlyStreams();
                List<VideoStream> videoStreams = se.getVideoOnlyStreams();
                List<AudioStream> audioStreams = se.getAudioStreams();
                List<SubtitlesStream> subtitlesStreams =
                    se.getSubtitlesDefault();
                JPanel videoContol = new JPanel(
                    new FlowLayout(FlowLayout.LEFT)
                );
                DefaultComboBoxModel<VideoStream> videoModel =
                    new DefaultComboBoxModel<>();
                videoModel.addAll(videoStreams);
                videoModel.setSelectedItem(videoStreams.get(0));
                JComboBox<VideoStream> videoComboBox = new JComboBox<
                    VideoStream
                >(videoModel);
                videoComboBox.setRenderer(new VideoComboBoxRenderer());
                videoContol.add(new JLabel("Video:"));
                videoContol.add(videoComboBox);
                DefaultComboBoxModel<AudioStream> audioModel =
                    new DefaultComboBoxModel<>();
                audioModel.addAll(audioStreams);
                audioModel.setSelectedItem(audioStreams.get(0));
                JComboBox<AudioStream> audioComboBox = new JComboBox<
                    AudioStream
                >(audioModel);
                audioComboBox.setRenderer(new AudioComboBoxRenderer());
                videoContol.add(new JLabel("Audio:"));
                videoContol.add(audioComboBox);
                JComboBox<SubtitlesStream> subtitleComboBox = null;
                if (!subtitlesStreams.isEmpty()) {
                    DefaultComboBoxModel<SubtitlesStream> subtitleModel =
                        new DefaultComboBoxModel<SubtitlesStream>();
                    subtitleModel.addElement(null);
                    subtitleModel.addAll(subtitlesStreams);
                    subtitleComboBox = new JComboBox<SubtitlesStream>(
                        subtitleModel
                    );
                    subtitleComboBox.setRenderer(
                        new SubTitleComboBoxRenderer()
                    );
                    videoContol.add(new JLabel("Subtitle:"));
                    videoContol.add(subtitleComboBox);
                }
                JComboBox<SubtitlesStream> subtitleComboBox2 = subtitleComboBox;
                String videoTitle = se.getName();
                String uploaderName = se.getUploaderName();
                SwingUtilities.invokeLater(() -> {
                    if (
                        JOptionPane.showConfirmDialog(
                            component,
                            videoContol,
                            "Oepn in VLC Media Player",
                            JOptionPane.OK_CANCEL_OPTION
                        ) ==
                        JOptionPane.OK_OPTION
                    ) {
                        new Thread(() -> {
                            VideoStream vs =
                                (VideoStream) videoComboBox.getSelectedItem();
                            AudioStream as =
                                (AudioStream) audioComboBox.getSelectedItem();
                            SubtitlesStream ss = null;
                            if (subtitleComboBox2 != null) {
                                ss =
                                    (SubtitlesStream) subtitleComboBox2.getSelectedItem();
                            }

                            ArrayList<String> commands = new ArrayList<>(4);
                            commands.add(VideoUtil.vlcPath + "/vlc");
                            commands.add("--meta-title=" + videoTitle);
                            commands.add("--meta-artist=" + uploaderName);
                            // commands.add(videoTitle);
                            if (ss != null) {
                                commands.add(
                                    "--input-slave=" +
                                        as.getContent() +
                                        "#" +
                                        ss.getContent()
                                );
                            } else {
                                commands.add(
                                    "--input-slave=" + as.getContent()
                                );
                                commands.add("--sub-track=1");
                            }
                            commands.add(vs.getContent());

                            ProcessBuilder processBuilder = new ProcessBuilder(
                                commands
                            );
                            try {
                                processBuilder.start();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        })
                            .start();
                    }
                });
            } catch (ExtractionException | IOException err) {
                err.printStackTrace();
            }
        })
            .start();
    }

    public static class VideoComboBoxRenderer extends DefaultListCellRenderer {

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

    public static class AudioComboBoxRenderer extends DefaultListCellRenderer {

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

    public static class SubTitleComboBoxRenderer
        extends DefaultListCellRenderer
    {

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
}
