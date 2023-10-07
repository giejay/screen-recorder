import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestDemo {
    public static void main(String[] args) throws InterruptedException, MalformedURLException {
        System.out.println("Hello World!");
        VideoRecorderConfiguration.wantToUseFullScreen(true);
        VideoRecorderConfiguration.setVideoDirectory(new File(Paths.get("video").toAbsolutePath().toString()));
        VideoRecorderConfiguration.setKeepFrames(false);
        VideoRecorderConfiguration.setCaptureInterval(100);

        long startTime = System.currentTimeMillis();

        VideoRecorder.start("3min");
        TimeUnit.MINUTES.sleep(3);
        VideoRecorder.stop();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        long minutes = (executionTime / (1000 * 60)) % 60;
        long seconds = (executionTime / 1000) % 60;
        long milliseconds = executionTime % 1000;

        System.out.println("Execution time: " + minutes + " minutes, " + seconds + " seconds, " + milliseconds + " milliseconds");
    }
}
