package com.kreativ.post;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class Brand {
    String business="",hours="",logo="",font="lora";
    int background=0xfff7f0df,ink=0xff243d32,accent=0xffb25032;
    static Brand from(Project p){Brand b=new Brand();b.business=p.business;b.hours=p.hours;b.logo=p.logo;b.font=p.font;b.background=p.background;b.ink=p.ink;b.accent=p.accent;return b;}
    void apply(Project p){p.business=business;p.hours=hours;p.logo=logo;p.font=font;p.background=background;p.ink=ink;p.accent=accent;for(Project.Element e:p.square.values())e.color=0;for(Project.Element e:p.vertical.values())e.color=0;}
    void save(Context c)throws IOException {try {ProjectStore.write(new File(c.getFilesDir(),"brand.json"),new JSONObject().put("business",business).put("hours",hours).put("logo",logo).put("font",font).put("background",background).put("ink",ink).put("accent",accent).toString());}catch(JSONException e){throw new IOException(e);}}
    static Brand load(Context c)throws IOException {File f=new File(c.getFilesDir(),"brand.json");if(!f.exists())return null;try {JSONObject j=new JSONObject(new String(new android.util.AtomicFile(f).readFully(),StandardCharsets.UTF_8));Brand b=new Brand();b.business=j.getString("business");b.hours=j.getString("hours");b.logo=j.getString("logo");b.font=j.getString("font");b.background=j.getInt("background");b.ink=j.getInt("ink");b.accent=j.getInt("accent");return b;}catch(JSONException e){throw new IOException("Saved brand cannot be read",e);}}
}
