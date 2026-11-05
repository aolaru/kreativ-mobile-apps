package com.kreativ.post;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ProjectStore {
    private final File root, media;
    ProjectStore(Context c) { root=new File(c.getFilesDir(),"projects");media=new File(c.getFilesDir(),"media");root.mkdirs();media.mkdirs(); }
    File media(String name) throws IOException {
        if(!name.matches("[a-fA-F0-9-]{36}\\.source")) throw new IOException("Invalid media reference"); return new File(media,name);
    }
    synchronized void save(Project p) throws IOException {
        p.updated=System.currentTimeMillis();
        try { write(new File(root,p.id+".json"),p.json().toString()); } catch(JSONException e) { throw new IOException("Could not save project",e); }
    }
    synchronized Project load(String id) throws IOException {
        if(!id.matches("[a-fA-F0-9-]{36}")) throw new IOException("Invalid project");
        try { return Project.read(new String(new AtomicFile(new File(root,id+".json")).readFully(),StandardCharsets.UTF_8)); }
        catch(JSONException e) { throw new IOException("Project could not be opened: "+e.getMessage(),e); }
    }
    List<File> files() { File[] files=root.listFiles((dir,name)->name.endsWith(".json")); List<File> list=new ArrayList<>(Arrays.asList(files==null?new File[0]:files)); list.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));return list; }
    Project duplicate(Project source) throws IOException { Project p=source.copy();p.id=UUID.randomUUID().toString();p.name=source.name+" (copy)";save(p);return p; }
    static void write(File file,String text) throws IOException {
        AtomicFile atomic=new AtomicFile(file); FileOutputStream stream=null;
        try { stream=atomic.startWrite();stream.write(text.getBytes(StandardCharsets.UTF_8));atomic.finishWrite(stream); }
        catch(IOException e) { if(stream!=null) atomic.failWrite(stream);throw e; }
    }
}
