package io.openliberty.tools.junit5;

import static com.automation.remarks.video.RecordingUtils.doVideoProcessing;
import static com.automation.remarks.video.RecordingUtils.videoEnabled;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import com.automation.remarks.video.recorder.monte.MonteScreenRecorder;
import com.automation.remarks.video.recorder.monte.MonteScreenRecorderBuilder;

public class LTEVideoExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

  private MonteScreenRecorder recorder;
  
  @Override
  public void beforeTestExecution(ExtensionContext context) throws Exception {
    if (videoDisabled(context.getTestMethod().get())) {
      return;
   }
     
    recorder = MonteScreenRecorderBuilder.builder().build();
    
    System.out.println("SKSK: recorder =" + recorder);
  }

  @Override
  public void afterTestExecution(ExtensionContext context) throws Exception {
    if (videoDisabled(context.getTestMethod().get())) {
      return;
    }

    String fileName = getFileName(context.getTestMethod().get());
    File video = stopRecording(fileName);
    if (context.getExecutionException().isPresent()) {
      doVideoProcessing(false, video);
    } else {
      doVideoProcessing(true, video);
    }
  }

  private boolean videoDisabled(Method testMethod) {
    Optional<com.automation.remarks.video.annotations.Video> video = AnnotationUtils.findAnnotation(testMethod, com.automation.remarks.video.annotations.Video.class);

    return !videoEnabled(video.orElse(null));
  }

  private String getFileName(Method testMethod) {
    String methodName = testMethod.getName();
    LTEVideo video = testMethod.getAnnotation(LTEVideo.class);
    return getVideoFileName(video, methodName);
  }

  private static String getVideoFileName(LTEVideo annotation, String methodName) {
    if (annotation == null) {
      return methodName;
    }
    String name = annotation.name();
    return name.length() > 1 ? name : methodName;
  }

  private File stopRecording(String filename) {
    File video = null;
    if (recorder != null) {
        try {
            video = recorder.saveAs(filename);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalStateException("Video recording wasn't started");
        }
    }
    return video;
  }
}
