package ch.sbb.matsim.umlego.log;

import ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@Plugin(name = "Hdfs", category = "Core", elementType = "appender", printObject = true)
public class HdfsAppender extends AbstractAppender {

    private final FileSystem fileSystem;
    private final Path logFilePath;
    private FSDataOutputStream outputStream;

    protected HdfsAppender(String name, Layout<? extends Serializable> layout, String path) throws IOException {
        super(name, null, layout, true);
        this.fileSystem = FileSystemUtil.getFileSystem();
        this.logFilePath = new Path(path);
        this.outputStream = fileSystem.create(logFilePath);
    }

    @Override
    public void append(LogEvent event) {
        try {
            String message = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8);
            outputStream.write(message.getBytes(StandardCharsets.UTF_8), 0, message.length());
            outputStream.hflush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PluginFactory
    public static HdfsAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginAttribute("path") String path) throws IOException {
        return new HdfsAppender(name, layout, path);
    }

    @Override
    public void stop() {
        super.stop();
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
