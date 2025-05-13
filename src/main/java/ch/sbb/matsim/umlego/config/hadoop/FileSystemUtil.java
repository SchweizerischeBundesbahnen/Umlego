package ch.sbb.matsim.umlego.config.hadoop;

import static ch.sbb.matsim.umlego.config.UmlegoConfig.isRunningLocally;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public final class FileSystemUtil {

    private FileSystemUtil() {}

    public static FileSystem getFileSystem() throws IOException {
        if (isRunningLocally()) {
            return FileSystem.getLocal(LocalFileSystemConfiguration.create());
        } else {
            return FileSystem.get(AzureFileSystemConfiguration.get());
        }
    }

    public static String getRootDir() {
        if (isRunningLocally()) {
            return "";
        } else {
            return "/simba_tagesprognose/";
        }
    }

    public static BufferedReader getBufferedReader(String filename) {
        try {
            FileSystem fs = getFileSystem();
            FSDataInputStream inputStream = fs.open(new Path(FileSystemUtil.getRootDir() + filename));
            return new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static BufferedWriter getBufferedWriter(String filename) {
        try {
            FileSystem fs = getFileSystem();
            FSDataOutputStream outputStream = fs.create(new Path(FileSystemUtil.getRootDir() + filename));
            return new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean exists(String fileName) {
        try {
            FileSystem fs = getFileSystem();
            return fs.exists(new Path(fileName));
        } catch (IOException e) {
            return false;
        }
    }

}
