/**
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Alejandro Gómez Morón
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.agomezmoron.multimedia.recorder;

import com.github.agomezmoron.multimedia.capture.ScreenCapture;
import com.github.agomezmoron.multimedia.external.JpegImagesToMovie;
import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;
import com.github.agomezmoron.multimedia.recorder.listener.VideoRecorderEventListener;
import com.github.agomezmoron.multimedia.recorder.listener.VideoRecorderEventObject;
import com.sun.jna.Platform;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.media.MediaLocator;

/**
 * It models the video recorder.
 *
 * @author Alejandro Gomez <agommor@gmail.com>
 */
public class VideoRecorder {

    private static final List<VideoRecorderEventListener> listeners = new ArrayList<>();
    private static final Robot rt;
    /**
     * Executor to deal with the queue of capture instances
     */
    private static final ExecutorService queueExecutor = Executors.newSingleThreadExecutor();
    /**
     * Executor to schedule the capture of frames
     */
    private static ScheduledExecutorService executor;
    /**
     * Status of the recorder.
     */
    private static boolean recording = false;
    /**
     * Flag to label that does it need continue to deal with the queue of captures
     */
    private static AtomicBoolean processing;
    /**
     * Latch to wait for the queue of captures to be processed completely
     */
    private static CountDownLatch latch;
    /**
     * Associated frames.
     */
    private static List<String> frames;
    /**
     * Video name.
     */
    private static String videoName = "output.mov";
    private static final File videoFile = new File(
        VideoRecorderConfiguration.getTempDirectory().getAbsolutePath()
            + File.separatorChar
            + videoName.replace(".mov", ""));
    private static Thread currentThread;


    private static BlockingQueue<ScreenCapture> queue;
    private static final Runnable task = new Runnable() {
        @Override
        public void run() {
            try {
                ScreenCapture capture = new ScreenCapture(rt.createScreenCapture(new Rectangle(
                    VideoRecorderConfiguration.getX(), VideoRecorderConfiguration.getY(),
                    VideoRecorderConfiguration.getWidth(),
                    VideoRecorderConfiguration.getHeight())));

                VideoRecorderEventObject videoRecorderEvObj = new VideoRecorderEventObject(this,
                    capture);

                //Exploring all the listeners
                for (VideoRecorderEventListener vr : listeners) {
                    //Creating the object that will be sent
                    vr.frameAdded(videoRecorderEvObj);
                }

                queue.put(capture); // put capture in queue and proceed IO steps in another thread
            } catch (Exception e) {
                System.out.println(Arrays.toString(e.getStackTrace()));
                setRecording(false);
            }
        }
    };

    static {
        try {
            rt = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * We don't allow to create objects for this class.
     */
    private VideoRecorder() {

    }

    public static void stopQueueExecutor() {
        processing.set(false);
        queueExecutor.shutdown();
    }

    /**
     * Strategy to record using {@link Thread}.
     */
    private static Thread getRecordThread() {
        return new Thread() {
            @Override
            public void run() {
                Robot rt;
                ScreenCapture capture;
                try {
                    rt = new Robot();
                    do {
                        capture = new ScreenCapture(rt.createScreenCapture(new Rectangle(
                            VideoRecorderConfiguration.getX(), VideoRecorderConfiguration.getY(),
                            VideoRecorderConfiguration.getWidth(),
                            VideoRecorderConfiguration.getHeight())));

                        VideoRecorderEventObject videoRecorderEvObj = new VideoRecorderEventObject(
                            this, capture);

                        //Exploring all the listeners
                        for (VideoRecorderEventListener vr : listeners) {

                            //Creating the object that will be sent
                            vr.frameAdded(videoRecorderEvObj);
                        }

                        frames.add(VideoRecorderUtil.saveIntoDirectory(capture, new File(
                            VideoRecorderConfiguration.getTempDirectory().getAbsolutePath()
                                + File.separatorChar
                                + videoName.replace(".mov", ""))));
                        Thread.sleep(VideoRecorderConfiguration.getCaptureInterval());
                    } while (recording);
                } catch (Exception e) {
                    System.out.println(Arrays.toString(e.getStackTrace()));
                    recording = false;
                }
            }
        };
    }

    public static void setRecording(boolean value) {
        if (recording != value) {
            recording = value;
            if (!recording) {
                executor.shutdownNow();
            }
        }
    }

    /**
     * It stops the recording and creates the video.
     *
     * @return a {@link String} with the path where the video was created or null if the video
     * couldn't be created.
     * @throws MalformedURLException
     */
    public static String stop() throws MalformedURLException, InterruptedException {
        String videoPathString = null;
        if (recording) {
            setRecording(false);
            stopQueueExecutor();
            latch.await(); // wait for the queue of captures to be processed
            System.out.println("Frames: " + frames.size());
            videoPathString = createVideo();
            if (!VideoRecorderConfiguration.wantToKeepFrames()) {
                deleteDirectory(
                    new File(VideoRecorderConfiguration.getTempDirectory().getAbsolutePath()
                        + File.separatorChar + videoName.replace(".mov", "")));
            }
        }
        return videoPathString;
    }

    /**
     * It starts recording (if it wasn't started before).
     *
     * @param newVideoName with the output of the video.
     */
    public static void start(String newVideoName) {
        if (!recording) {
            if (!VideoRecorderConfiguration.getTempDirectory().exists()) {
                VideoRecorderConfiguration.getTempDirectory().mkdirs();
            }
            calculateScreenshotSize();
            videoName = newVideoName;
            if (!videoName.endsWith(".mov")) {
                videoName += ".mov";
            }

            // initialize the executor and queue
            executor = Executors.newScheduledThreadPool(2);
            queue = new LinkedBlockingQueue<>();
            frames = new ArrayList<>();
            processing = new AtomicBoolean(true);
            latch = new CountDownLatch(1);
            setRecording(true);
            executor.scheduleAtFixedRate(task, 0, VideoRecorderConfiguration.getCaptureInterval(),
                TimeUnit.MILLISECONDS);
            queueExecutor.execute(new CaptureProcessorRunnable());
        }
    }

    /**
     * It starts recording (if it wasn't started before).
     *
     * @param newVideoName         with the output of the video.
     * @param scheduledThreadCount number of threads in newScheduledThreadPool
     */
    public static void start(String newVideoName, int scheduledThreadCount) {
        if (!recording) {
            if (!VideoRecorderConfiguration.getTempDirectory().exists()) {
                VideoRecorderConfiguration.getTempDirectory().mkdirs();
            }
            calculateScreenshotSize();
            videoName = newVideoName;
            if (!videoName.endsWith(".mov")) {
                videoName += ".mov";
            }

            executor = Executors.newScheduledThreadPool(scheduledThreadCount);
            queue = new LinkedBlockingQueue<>();
            frames = new ArrayList<>();
            processing = new AtomicBoolean(true);
            latch = new CountDownLatch(1);
            setRecording(true);
            executor.scheduleAtFixedRate(task, 0, VideoRecorderConfiguration.getCaptureInterval(),
                TimeUnit.MILLISECONDS);
            queueExecutor.execute(new CaptureProcessorRunnable());
        }
    }

    /**
     * It calculates the screenshot size before recording. If the useFullScreen was defined, the
     * width, height or x
     */
    private static void calculateScreenshotSize() {
        // if fullScreen was set, all the configuration will be changed back.
        if (VideoRecorderConfiguration.wantToUseFullScreen()) {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            VideoRecorderConfiguration.setWidth((int) size.getWidth());
            VideoRecorderConfiguration.setHeight((int) size.getHeight());
            VideoRecorderConfiguration.setCoordinates(0, 0);
        } else {
            // we have to check if x+width <= Toolkit.getDefaultToolkit().getScreenSize().getWidth() and the same for
            // the height
            if (VideoRecorderConfiguration.getX() + VideoRecorderConfiguration.getWidth()
                > Toolkit.getDefaultToolkit()
                .getScreenSize().getWidth()) {
                VideoRecorderConfiguration.setWidth(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()
                        - VideoRecorderConfiguration.getX()));
            }
            if (VideoRecorderConfiguration.getY() + VideoRecorderConfiguration.getHeight()
                > Toolkit.getDefaultToolkit()
                .getScreenSize().getHeight()) {
                VideoRecorderConfiguration.setHeight(
                    (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight()
                        - VideoRecorderConfiguration.getY()));
            }
        }
    }

    /**
     * It deletes recursively a directory.
     *
     * @param directory to be deleted.
     * @return true if the directory was deleted successfully.
     */
    private static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    /**
     * It creates the video.
     *
     * @return a {@link String} with the path where the video was created or null if the video
     * couldn't be created.
     * @throws MalformedURLException
     */
    private static String createVideo() throws MalformedURLException {
        Vector<String> vector = new Vector<String>(frames);
        String videoPathString = null;
        JpegImagesToMovie jpegImaveToMovie = new JpegImagesToMovie();
        if (!VideoRecorderConfiguration.getVideoDirectory().exists()) {
            VideoRecorderConfiguration.getVideoDirectory().mkdirs();
        }
        MediaLocator oml;
        String fileURL;
        if (Platform.isWindows()) {
            fileURL = "file://" + VideoRecorderConfiguration.getVideoDirectory().getPath()
                + File.separatorChar + videoName;
        } else {
            fileURL = VideoRecorderConfiguration.getVideoDirectory().getAbsolutePath()
                + File.separatorChar + videoName;
        }
        if ((oml = JpegImagesToMovie.createMediaLocator(fileURL)) == null) {
            System.exit(0);
        }
        if (jpegImaveToMovie.doIt(VideoRecorderConfiguration.getWidth(), VideoRecorderConfiguration
            .getHeight(), (1000 / VideoRecorderConfiguration.getCaptureInterval()), vector, oml)) {
            videoPathString = VideoRecorderConfiguration.getVideoDirectory().getAbsolutePath()
                + File.separatorChar
                + videoName;
        }
        return videoPathString;
    }

    /**
     * It adds the listeners to the list
     *
     * @param args
     */
    public static void addVideoRecorderEventListener(VideoRecorderEventListener args) {
        listeners.add(args);
    }

    private static class CaptureProcessorRunnable implements Runnable {

        @Override
        public void run() {
            while (processing.get() || !queue.isEmpty()) {
                try {
                    if (queue.isEmpty()) {
                        Thread.sleep(100);
                        continue;
                    }
                    ScreenCapture capture = queue.take();
                    frames.add(VideoRecorderUtil.saveIntoDirectory(capture, videoFile));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            latch.countDown();
        }
    }
}
