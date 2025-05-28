import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestDemo {
    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Monitor with 1920x1080 resolution + 17 mins + 200ms + 0.8f = .mov file with 796 MB");
        VideoRecorderConfiguration.wantToUseFullScreen(true);
        Path video1 = Paths.get("video");
        File video = new File(video1.toAbsolutePath().toString());
        FileUtils.deleteDirectory(video);
        VideoRecorderConfiguration.setVideoDirectory(video);
        VideoRecorderConfiguration.setTempDirectory(video);
        VideoRecorderConfiguration.setKeepFrames(false);
        VideoRecorderConfiguration.setCaptureInterval(100);
        VideoRecorderConfiguration.setImageCompressionQuality(0.8f);

        long startTime = System.currentTimeMillis();

        VideoRecorder.start("10s.200ms.8f");
        TimeUnit.SECONDS.sleep(10);
        VideoRecorder.stop();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        long minutes = (executionTime / (1000 * 60)) % 60;
        long seconds = (executionTime / 1000) % 60;
        long milliseconds = executionTime % 1000;

        System.out.println("Execution time: " + minutes + " minutes, " + seconds + " seconds, " + milliseconds + " milliseconds");
    }
}
