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
import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;

class VideoRecorderUtil {

    public static String saveIntoDirectory(ScreenCapture capture, File directory)
        throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String savedPath =
            directory.getAbsolutePath() + File.separatorChar + System.currentTimeMillis() + ".jpeg";

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

        // Create the ImageWriteParam to compress the image.
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(VideoRecorderConfiguration.getImageCompressionQuality()); // set compression quality, from 0.0f to 1.0f

        File outputFile = new File(savedPath);
        ImageOutputStream outputStream = ImageIO.createImageOutputStream(outputFile);

        writer.setOutput(outputStream);
        writer.write(null, new javax.imageio.IIOImage(capture.getSource(), null, null), param);
        outputStream.close();
        writer.dispose();

        return savedPath;
    }
}
