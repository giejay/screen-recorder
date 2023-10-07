import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestDemo {
    public static void main(String[] args) throws InterruptedException, MalformedURLException {
        System.out.println("Monitor with 1920x1080 resolution + 17 mins + 200ms + 0.8f = .mov file with 796 MB");
        VideoRecorderConfiguration.wantToUseFullScreen(true);
        VideoRecorderConfiguration.setVideoDirectory(new File(Paths.get("video").toAbsolutePath().toString()));
        VideoRecorderConfiguration.setKeepFrames(false);
        VideoRecorderConfiguration.setCaptureInterval(200);
        VideoRecorderConfiguration.setImageCompressionQuality(0.8f);

        long startTime = System.currentTimeMillis();

        VideoRecorder.start("17min.200ms.8f");
        TimeUnit.MINUTES.sleep(17);
        VideoRecorder.stop();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        long minutes = (executionTime / (1000 * 60)) % 60;
        long seconds = (executionTime / 1000) % 60;
        long milliseconds = executionTime % 1000;

        System.out.println("Execution time: " + minutes + " minutes, " + seconds + " seconds, " + milliseconds + " milliseconds");
    }
}
