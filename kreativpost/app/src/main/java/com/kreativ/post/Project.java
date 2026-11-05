package com.kreativ.post;

import org.json.*;
import java.util.*;

final class Project {
    static final int VERSION = 1;
    static final String[] ELEMENTS = {"photo", "headline", "description", "price", "brand", "logo"};
    String id = UUID.randomUUID().toString(), name = "Today's promotion", headline = "Fresh from\nthe oven", description = "Baked here. Enjoyed together.", price = "€4.50", business = "YOUR LOCAL BAKERY", hours = "Every morning, made with care";
    String logo = "", font = "lora";
    ArrayList<String> photos = new ArrayList<>();
    int template = 0, background = 0xfff7f0df, ink = 0xff243d32, accent = 0xffb25032;
    boolean story;
    long updated;
    LinkedHashMap<String, Element> square = new LinkedHashMap<>(), vertical = new LinkedHashMap<>();
    static final class Element {
        float x,y,w,h,zoom=1,panX=.5f,panY=.5f,size=.10f,tracking=0,leading=1.05f;
        int alignment=0, color=0; // 0 inherits the project ink
        Element(float x,float y,float w,float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        JSONObject json() throws JSONException {
            return new JSONObject().put("x",x).put("y",y).put("w",w).put("h",h).put("zoom",zoom).put("panX",panX).put("panY",panY).put("size",size).put("tracking",tracking).put("leading",leading).put("alignment",alignment).put("color",color);
        }
        static Element read(JSONObject j) throws JSONException {
            Element e=new Element(f(j,"x",0,1),f(j,"y",0,1),f(j,"w",.04f,1),f(j,"h",.03f,1));
            e.x=Geometry.clamp(e.x,0,1-e.w); e.y=Geometry.clamp(e.y,0,1-e.h);
            e.zoom=f(j,"zoom",1,3); e.panX=f(j,"panX",0,1); e.panY=f(j,"panY",0,1);
            e.size=f(j,"size",.016f,.22f); e.tracking=f(j,"tracking",-.03f,.12f); e.leading=f(j,"leading",1,1.8f);
            e.alignment=Math.max(0,Math.min(2,j.optInt("alignment"))); e.color=j.optInt("color"); return e;
        }
        private static float f(JSONObject j,String key,float min,float max) throws JSONException {
            double n=j.getDouble(key); if (!Double.isFinite(n)) throw new JSONException("Invalid geometry");
            return Geometry.clamp((float)n,min,max);
        }
    }
    Map<String,Element> layout() { return story ? vertical : square; }
    String text(String key) { switch(key) { case "headline":return headline; case "description":return description; case "price":return price; case "brand":return business+(hours.isBlank()?"":"\n"+hours); default:return ""; } }
    JSONObject json() throws JSONException {
        JSONObject j=new JSONObject().put("version",VERSION).put("id",id).put("name",name).put("template",template).put("headline",headline).put("description",description).put("price",price).put("business",business).put("hours",hours).put("font",font).put("logo",logo).put("photos",new JSONArray(photos)).put("background",background).put("ink",ink).put("accent",accent).put("story",story).put("updated",updated);
        j.put("square",layoutJson(square)); j.put("vertical",layoutJson(vertical)); return j;
    }
    private JSONObject layoutJson(Map<String,Element> map) throws JSONException { JSONObject j=new JSONObject(); for(String key:ELEMENTS) j.put(key,map.get(key).json()); return j; }
    static Project read(String source) throws JSONException {
        JSONObject j=new JSONObject(source);
        if(j.getInt("version")!=VERSION) throw new JSONException("This project needs a newer app version");
        Project p=new Project(); p.id=j.getString("id");
        if(!p.id.matches("[a-fA-F0-9-]{36}")) throw new JSONException("Invalid project identity");
        p.name=j.getString("name"); p.template=j.getInt("template"); if(p.template<0 || p.template>=Templates.NAMES.length) throw new JSONException("Unknown template");
        p.headline=j.getString("headline"); p.description=j.getString("description"); p.price=j.getString("price"); p.business=j.getString("business"); p.hours=j.getString("hours");
        p.font=j.getString("font"); if(!p.font.equals("lora") && !p.font.equals("dmsans")) p.font="lora";
        p.logo=j.getString("logo"); JSONArray a=j.getJSONArray("photos"); if(a.length()>3) throw new JSONException("Too many photos"); for(int i=0;i<a.length();i++) p.photos.add(a.getString(i));
        p.background=j.getInt("background"); p.ink=j.getInt("ink"); p.accent=j.getInt("accent"); p.story=j.getBoolean("story"); p.updated=j.optLong("updated");
        for(String key:ELEMENTS) { p.square.put(key,Element.read(j.getJSONObject("square").getJSONObject(key))); p.vertical.put(key,Element.read(j.getJSONObject("vertical").getJSONObject(key))); }
        return p;
    }
    Project copy() { try { return read(json().toString()); } catch(JSONException e) { throw new IllegalStateException(e); } }
}
