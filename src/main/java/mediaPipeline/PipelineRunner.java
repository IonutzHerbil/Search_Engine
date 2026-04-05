package mediaPipeline;

import mediaPipeline.model.*;
import mediaPipeline.stage.PipelineContext;
import java.nio.file.Path;

public class PipelineRunner {
    public static void main(String[] args) {
        System.out.println("=== Pipeline Runner ===");

        var video = new VideoFile("movie_101", Path.of("sample.mp4"));
        var ctx = new PipelineContext(video, Path.of("output/movie_101"));

        System.out.println("Output root: " + ctx.outputRoot());
        ctx.put("test_key", "hello");
        System.out.println("Context get: " + ctx.getString("test_key"));

        System.out.println("=== OK ===");
    }
}