package org.zane.newpipe;

import com.formdev.flatlaf.IntelliJTheme;
import com.sun.jna.NativeLibrary;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.schabi.newpipe.extractor.NewPipe;
import org.zane.newpipe.util.Downloader;
import org.zane.newpipe.util.VideoUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.MissingParameterException;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;

@Command(
    name = "NewPipe Desktop",
    mixinStandardHelpOptions = true,
    version = "NewPipe Desktop v0.3"
)
public class Main implements Runnable {

    public static enum LaunchMode {
        DEFAULT,
        DOWNLOAD,
        VLC,
    }

    @Option(
        names = { "-m", "--mode" },
        description = "Valid values: ${COMPLETION-CANDIDATES}",
        defaultValue = "DEFAULT",
        paramLabel = "Launch Mode"
    )
    LaunchMode launchMode;

    @Option(
        names = { "-v", "--vlc" },
        description = "custom VLC media player install location",
        paramLabel = "VLC Path"
    )
    String vlcPath;

    @Parameters(
        arity = "0..1",
        index = "0",
        paramLabel = "Query",
        description = "Search query or video URL"
    )
    String query;

    @Spec
    CommandSpec spec;

    public void run() {
        NewPipe.init(new Downloader());
        IntelliJTheme.setup(
            App.class.getResourceAsStream("/Darcula_Pitch_Black.theme.json")
        );
        NativeDiscovery nd = new NativeDiscovery();
        if (nd.discover()) {
            VideoUtil.setVlcPath(nd.discoveredPath());
        }
        if (vlcPath != null && !vlcPath.isBlank()) {
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcCoreLibraryName(),
                vlcPath
            );
        }
        switch (launchMode) {
            case DEFAULT:
                SwingUtilities.invokeLater(() -> {
                    App app;
                    if (query == null || query.isBlank()) {
                        app = new App();
                    } else {
                        app = new App(query);
                    }
                    app.setVisible(true);
                });
                break;
            case DOWNLOAD:
                if (query == null || query.isBlank()) {
                    ArgSpec querySpec = null;
                    for (ArgSpec argSpec : spec.args()) {
                        if (argSpec.paramLabel() == "Query") {
                            querySpec = argSpec;
                        }
                    }
                    throw new MissingParameterException(
                        spec.commandLine(),
                        querySpec,
                        "Video URL are required for download mode."
                    );
                }
                VideoUtil.downloadVideo(query, true);
                break;
            case VLC:
                if (query == null || query.isBlank()) {
                    ArgSpec querySpec = null;
                    for (ArgSpec argSpec : spec.args()) {
                        if (argSpec.paramLabel() == "Query") {
                            querySpec = argSpec;
                        }
                    }
                    throw new MissingParameterException(
                        spec.commandLine(),
                        querySpec,
                        "Video URL are required for open with VLC mode."
                    );
                }
                VideoUtil.openVLC(query, null);
                break;
        }
    }

    public static void main(String[] args) {
        CommandLine cl = new CommandLine(new Main());
        cl.setCaseInsensitiveEnumValuesAllowed(true);
        int exitCode = cl.execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
