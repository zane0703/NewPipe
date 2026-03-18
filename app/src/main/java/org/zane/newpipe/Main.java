package org.zane.newpipe;

import com.formdev.flatlaf.IntelliJTheme;
import com.sun.jna.NativeLibrary;
import javax.swing.SwingUtilities;
import org.schabi.newpipe.extractor.NewPipe;
import org.zane.newpipe.util.Downloader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;

@Command(
    name = "NewPipe Desktop",
    mixinStandardHelpOptions = true,
    version = "NewPipe Desktop v0.2"
)
public class Main implements Runnable {

    @Option(
        names = { "-v", "--vlc" },
        description = "custom VLC media player install location"
    )
    String vlcPath;

    @Parameters(
        arity = "0..1",
        index = "0",
        paramLabel = "Search query",
        description = "Search query or video URL"
    )
    String searchQuery;

    public void run() {
        NewPipe.init(new Downloader());
        IntelliJTheme.setup(
            App.class.getResourceAsStream("/Darcula_Pitch_Black.theme.json")
        );
        if (vlcPath != null) {
            NativeLibrary.addSearchPath(
                RuntimeUtil.getLibVlcCoreLibraryName(),
                vlcPath
            );
        }
        SwingUtilities.invokeLater(() -> {
            App app;
            if (searchQuery == null) {
                app = new App();
            } else {
                app = new App(searchQuery);
            }
            app.setVisible(true);
        });
    }

    public static void main(String[] args) {
        new CommandLine(new Main()).execute(args);
    }
}
