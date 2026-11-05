package com.kreativ.post;

import android.content.*;
import android.graphics.*;
import android.net.Uri;
import androidx.test.core.app.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class StaticWorkflowTest {
    Context c;ProjectStore store;MediaLibrary media;DesignRenderer renderer;
    @Before public void setup(){c=ApplicationProvider.getApplicationContext();store=new ProjectStore(c);media=new MediaLibrary(c,store);renderer=new DesignRenderer(c,media);}
    @After public void cleanup(){media.clear();}
    @Test public void tomorrowWorkflowPreservesEverything()throws Exception{
        Project p=Templates.create(0);p.headline="Today's almond croissant";p.description="Butter, almonds & a little joy";p.price="€3.75";p.square.get("headline").tracking=.02f;p.vertical.get("photo").zoom=1.8f;
        Bitmap original=Bitmap.createBitmap(800,600,Bitmap.Config.ARGB_8888);original.eraseColor(0xffbc8654);File source=new File(c.getCacheDir(),"source.png");try(OutputStream out=new FileOutputStream(source)){original.compress(Bitmap.CompressFormat.PNG,100,out);}original.recycle();byte[] before=Files.readAllBytes(source.toPath());String ref=media.importUri(Uri.fromFile(source));p.photos.add(ref);p.logo=ref;
        assertArrayEquals(before,Files.readAllBytes(store.media(ref).toPath()));assertArrayEquals(before,Files.readAllBytes(source.toPath()));source.delete();store.save(p);
        Project tomorrow=new ProjectStore(c).load(p.id);assertEquals(p.json().toString(),tomorrow.json().toString());assertNotNull(new MediaLibrary(c,store).get(ref));
        Brand.from(p).save(c);Project other=Templates.create(2);Brand.load(c).apply(other);assertEquals(p.business,other.business);assertEquals(p.logo,other.logo);
        Project copy=store.duplicate(tomorrow);assertNotEquals(p.id,copy.id);copy.price="€4.00";store.save(copy);assertEquals("€3.75",store.load(p.id).price);assertEquals("€4.00",store.load(copy.id).price);
    }
    @Test public void allTemplatesFitAndExportsMatchPreview()throws Exception{
        File evidence=new File(c.getFilesDir(),"evidence");evidence.mkdirs();
        for(int t=0;t<5;t++)for(boolean story:new boolean[]{false,true}){Project p=Templates.create(t);p.story=story;
            for(String key:new String[]{"headline","description","price","brand"})assertFalse(Templates.NAMES[t]+" "+story+" "+key,renderer.fit(p,key,p.layout().get(key)).unreadable);
            ByteArrayOutputStream png=new ByteArrayOutputStream();StaticExport.write(renderer,p,false,png);Bitmap exported=BitmapFactory.decodeByteArray(png.toByteArray(),0,png.size());Bitmap preview=renderer.bitmap(p);assertTrue("PNG must exactly match the shared preview scene",preview.sameAs(exported));assertEquals(1080,exported.getWidth());assertEquals(story?1920:1080,exported.getHeight());
            try(OutputStream out=new FileOutputStream(new File(evidence,"template-"+t+(story?"-story":"-square")+".png"))){out.write(png.toByteArray());}
            ByteArrayOutputStream jpg=new ByteArrayOutputStream();StaticExport.write(renderer,p,true,jpg);Bitmap jpeg=BitmapFactory.decodeByteArray(jpg.toByteArray(),0,jpg.size());assertEquals(exported.getHeight(),jpeg.getHeight());assertEquals(exported.getWidth(),jpeg.getWidth());assertTrue(Color.red(jpeg.getPixel(0,0))>200);jpeg.recycle();exported.recycle();preview.recycle();
        }
    }
    @Test public void longTextWarnsAndShrinksWithoutMutatingWords(){Project p=Templates.create(1);String s="Fresh pastries and coffee for every neighbour. ".repeat(40);p.headline=s;Project.Element e=p.layout().get("headline");DesignRenderer.Fitted fit=renderer.fit(p,"headline",e);assertTrue(fit.unreadable);assertTrue(fit.size<e.size*1080);assertEquals(s,p.headline);p.headline="قهوة طازجة\nCafé & pão";e.w=.8f;e.h=.4f;assertFalse(renderer.fit(p,"headline",e).unreadable);p.ink=p.background;assertTrue(renderer.warnings(p).stream().anyMatch(w->w.contains("contrast")));}
    @Test public void layoutsAndHistoryStayIndependent(){Project p=Templates.create(0);History h=new History();h.record(p);p.square.get("headline").x=.12f;assertNotEquals(p.square.get("headline").x,p.vertical.get("headline").x,0);p=h.undo(p);assertEquals(.08f,p.square.get("headline").x,0);p=h.redo(p);assertEquals(.12f,p.square.get("headline").x,0);h.record(p);p.price="new";assertFalse(h.canRedo());}
    @Test public void failuresAreActionableAndDoNotOverwriteProject()throws Exception{Project p=Templates.create(3);store.save(p);String id=p.id;p.photos.add("00000000-0000-0000-0000-000000000000.source");assertFalse(media.missing(p).isEmpty());try{StaticExport.write(renderer,p,false,new ByteArrayOutputStream());fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("missing"));}assertTrue(store.load(id).photos.isEmpty());p.photos.clear();try{StaticExport.write(renderer,p,false,new OutputStream(){public void write(int b)throws IOException{throw new IOException("disk full");}});fail();}catch(IOException expected){}try{Project.read(p.json().put("version",999).toString());fail();}catch(org.json.JSONException expected){}File bad=new File(c.getCacheDir(),"bad.png");Files.write(bad.toPath(),new byte[]{1,2,3});try{media.importUri(Uri.fromFile(bad));fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("JPEG"));}}
    @Test public void uiRecreateReopenUpdateAndCancelImport()throws Exception{
        c.getSharedPreferences("MainActivity",0).edit().clear().commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{a.project=Templates.create(0);a.project.headline="Tomorrow's special";a.project.price="€2.90";a.save(false);a.editor();a.onActivityResult(101,android.app.Activity.RESULT_CANCELED,null);assertEquals("Tomorrow's special",a.project.headline);});
            scenario.recreate();scenario.onActivity(a->{assertEquals("Tomorrow's special",a.project.headline);assertEquals("€2.90",a.project.price);a.project.price="€3.20";a.save(false);});scenario.recreate();scenario.onActivity(a->assertEquals("€3.20",a.project.price));
        }
    }
}
