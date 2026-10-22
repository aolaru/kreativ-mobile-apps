package com.kreativ.photofeather;

import static org.junit.Assert.*;
import android.graphics.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import com.kreativ.imageengine.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;

public class ReviewProbeTest extends WorkflowTest {
  @Test public void reviewTruncatedJpeg() throws Exception {
    File original=fixture("complete.jpg",1200,800);
    byte[] bytes=Files.readAllBytes(original.toPath());
    File truncated=new File(session.exports,"truncated.jpg");
    Files.write(truncated.toPath(),Arrays.copyOf(bytes,bytes.length/2));
    session.add(Collections.singletonList(uri(truncated)));idle();
    session.process(new CompressionOptions(500000,1200,85,Color.WHITE,false));idle();
    PhotoItem p=session.items.get(0);
    String message="TRUNCATED: sourceBytes="+truncated.length()+" originalBytes="+bytes.length+" status="+p.status+" outputBytes="+p.after+" outputExists="+p.hasOutput();
    if(p.hasOutput()) {Bitmap b=BitmapFactory.decodeFile(p.output);message+=" bottomPixel="+Integer.toHexString(b.getPixel(b.getWidth()/2,b.getHeight()-10));b.recycle();}
    Log.i("PhotoFeatherReview",message);
    if(p.hasOutput()){
      File dir=new File(context.getExternalFilesDir(null),"review-evidence");dir.mkdirs();
      Files.copy(truncated.toPath(),new File(dir,"truncated-input.jpg").toPath(),StandardCopyOption.REPLACE_EXISTING);
      Files.copy(new File(p.output).toPath(),new File(dir,"accepted-output.jpg").toPath(),StandardCopyOption.REPLACE_EXISTING);
      try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){scenario.onActivity(a->click(a,"Compare before & after"));Thread.sleep(700);screen("review-truncated-comparison");androidx.test.uiautomator.UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()).pressBack();}
    }
    assertFalse("Truncated JPEG silently offered for export",p.exportable());
  }
  @Test public void reviewCancelledRetryRetainsPreviousResults() throws Exception {
    importOne();process();PhotoItem p=session.items.get(0);String old=p.output;assertTrue(p.exportable());
    session.process(new CompressionOptions(1000,2560,85,Color.WHITE,true));session.cancel();idle();
    Log.i("PhotoFeatherReview","RETRY: priorFileStillExists="+new File(old).exists()+" currentOutput='"+p.output+"' status="+p.status+" exportable="+p.exportable());
    assertTrue("Cancelled retry discards access to successful previous preview",p.hasOutput());
  }
  ScrollView scroll(View view){if(view instanceof ScrollView)return (ScrollView)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){ScrollView s=scroll(group.getChildAt(i));if(s!=null)return s;}}return null;}
  @Test public void reviewScrollStability() throws Exception {
    importOne();process();final int[] positions=new int[2];
    try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
      scenario.onActivity(a->{ScrollView s=scroll(a.getWindow().getDecorView());s.scrollTo(0,1000);positions[0]=s.getScrollY();click(a,"Advanced JPEG quality");});
      androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
      scenario.onActivity(a->{positions[1]=scroll(a.getWindow().getDecorView()).getScrollY();});
    }
    Log.i("PhotoFeatherReview","SCROLL: before="+positions[0]+" after="+positions[1]);
    assertTrue("Changing an option returns user to the top",positions[1]>0);
  }
  @Test public void reviewPublishedCopySurvivesJournalFailure() throws Exception {
    importOne();process();PhotoItem p=session.items.get(0);
    // Inject IOException into AtomicFile only after the real pending destination has been recorded.
    class FailingAtomicFile extends android.util.AtomicFile {
      boolean pendingWritten;
      FailingAtomicFile(){super(session.state.getBaseFile());}
      @Override public FileOutputStream startWrite() throws IOException {
        if(pendingWritten&&session.pending.isEmpty())throw new IOException("Injected journal write failure after published export");
        return super.startWrite();
      }
      @Override public void finishWrite(FileOutputStream stream){super.finishWrite(stream);if(!session.pending.isEmpty())pendingWritten=true;}
    }
    FailingAtomicFile failing=new FailingAtomicFile();
    java.lang.reflect.Field field=Session.class.getDeclaredField("state");field.setAccessible(true);field.set(session,failing);
    session.save(null);idle();
    PhotoItem copy=session.items.get(0);android.net.Uri savedUri=android.net.Uri.parse(copy.savedUri);saved.add(savedUri);
    boolean existedBefore;
    try(InputStream in=context.getContentResolver().openInputStream(savedUri)){existedBefore=in!=null;}
    // Restore uses the last successfully written journal, which still contains the completed URI as pending.
    Session restored=new Session(context);while(restored.busy)Thread.sleep(20);
    boolean existsAfter=true;try(InputStream in=context.getContentResolver().openInputStream(savedUri)){existsAfter=in!=null;}catch(FileNotFoundException e){existsAfter=false;}
    Log.i("PhotoFeatherReview","JOURNAL: commitWasInjected="+failing.pendingWritten+" publishedBeforeRestart="+existedBefore+" survivesRestart="+existsAfter+" reportedSavedUri="+!copy.savedUri.isEmpty());
    field.set(session,new android.util.AtomicFile(failing.getBaseFile()));restored.worker.shutdownNow();
    assertTrue("A completed saved JPEG was deleted after failed journal commit",existsAfter);
  }
}
