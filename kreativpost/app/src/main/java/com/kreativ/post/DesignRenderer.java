package com.kreativ.post;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import java.util.*;

/** All layout is resolved in canonical output pixels, then scaled as one scene. */
final class DesignRenderer {
    final MediaLibrary media;
    private final Typeface serif,sans;
    DesignRenderer(Context c,MediaLibrary m){media=m;serif=Typeface.createFromAsset(c.getAssets(),"fonts/lora.ttf");sans=Typeface.createFromAsset(c.getAssets(),"fonts/dmsans.ttf");}
    static final class Fitted {
        final StaticLayout layout;final float size;final boolean unreadable;
        Fitted(StaticLayout l,float size,boolean bad){layout=l;this.size=size;unreadable=bad;}
    }
    Fitted fit(Project p,String key,Project.Element e) {
        String text=p.text(key); float height=e.h*(p.story?1920:1080);int width=Math.max(1,Math.round(e.w*1080));
        float low=8,high=e.size*1080;StaticLayout layout=layout(p,key,e,text,width,high);
        if(layout.getHeight()>height){for(int n=0;n<14;n++){float mid=(low+high)/2;StaticLayout candidate=layout(p,key,e,text,width,mid);if(candidate.getHeight()<=height)low=mid;else high=mid;}high=low;layout=layout(p,key,e,text,width,high);}
        boolean widthOverflow=false;for(int i=0;i<layout.getLineCount();i++) if(layout.getLineWidth(i)>width+.5f)widthOverflow=true;
        return new Fitted(layout,high,!text.isEmpty()&&(high<(key.equals("headline")?36:24)||layout.getHeight()>height||widthOverflow));
    }
    private StaticLayout layout(Project p,String key,Project.Element e,String text,int width,float size){
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(e.color==0?p.ink:e.color);paint.setTextSize(size);
        paint.setTypeface(key.equals("headline")?(p.font.equals("lora")?serif:sans):sans);paint.setLetterSpacing(e.tracking);
        return StaticLayout.Builder.obtain(text,0,text.length(),paint,width).setAlignment(e.alignment==1?Layout.Alignment.ALIGN_CENTER:e.alignment==2?Layout.Alignment.ALIGN_OPPOSITE:Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(0,e.leading).setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY).build();
    }
    List<String> warnings(Project p){List<String>w=new ArrayList<>(media.missing(p));for(String key:new String[]{"headline","description","price","brand"}){Project.Element e=p.layout().get(key);if(fit(p,key,e).unreadable)w.add(key+": text is too small or clipped. Shorten it or enlarge its box.");int color=e.color==0?p.ink:e.color;if(!p.text(key).isEmpty()&&contrast(color,p.background)<4.5)w.add(key+": low contrast. Choose a darker text or lighter background colour.");}return w;}
    private double luminance(int c){double[]v={Color.red(c)/255d,Color.green(c)/255d,Color.blue(c)/255d};for(int i=0;i<3;i++)v[i]=v[i]<=.04045?v[i]/12.92:Math.pow((v[i]+.055)/1.055,2.4);return .2126*v[0]+.7152*v[1]+.0722*v[2];}
    double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    Bitmap bitmap(Project p){Bitmap b=Bitmap.createBitmap(1080,p.story?1920:1080,Bitmap.Config.ARGB_8888);draw(new Canvas(b),p,b.getWidth(),b.getHeight(),-1);return b;}
    void draw(Canvas canvas,Project p,int width,int height,double seconds){
        canvas.save();canvas.scale(width/1080f,height/(p.story?1920f:1080f));float h=p.story?1920:1080;canvas.drawColor(p.background);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(p.accent);paint.setStrokeWidth(3);
        canvas.drawRect(86,h*.108f,994,h*.111f,paint);
        if(p.template==2){paint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(35,35,1045,h-35,28,28,paint);paint.setStyle(Paint.Style.FILL);}
        if(p.template==3){paint.setColor(p.accent);canvas.drawCircle(1035,h*.51f,14,paint);canvas.drawCircle(1035,h*.54f,7,paint);}
        Project.Element photo=p.layout().get("photo");
        if(seconds<0){drawPhoto(canvas,p,photo,p.photos.isEmpty()?null:media.get(p.photos.get(0)),1,photo.zoom,photo.panX);}
        else {
            int count=Math.max(1,p.photos.size());double segment=15d/count;int index=Math.min(count-1,(int)(seconds/segment));double local=seconds-index*segment;
            float t=(float)(local/segment),fade=(float)Math.min(1,local/.65);
            if(index>0&&fade<1)drawPhoto(canvas,p,photo,media.get(p.photos.get(index-1)),1,photo.zoom*1.06f,photo.panX);
            drawPhoto(canvas,p,photo,p.photos.isEmpty()?null:media.get(p.photos.get(index)),index==0?1:fade,photo.zoom*(1+.06f*t),Geometry.clamp(photo.panX+.08f*t,0,1));
        }
        for(String key:new String[]{"brand","headline","description","price","logo"}){
            Project.Element e=p.layout().get(key);float alpha=1,dy=0;
            if(seconds>=0&&!key.equals("brand")&&!key.equals("description")){float delay=key.equals("headline")?.1f:key.equals("price")?.5f:.9f;float t=Geometry.clamp(((float)seconds-delay)/.65f,0,1);alpha=t;dy=(1-t)*28;}
            int layer=canvas.saveLayerAlpha(e.x*1080,e.y*h+dy,(e.x+e.w)*1080,(e.y+e.h)*h+dy,Math.round(alpha*255));
            canvas.translate(e.x*1080,e.y*h+dy);canvas.clipRect(0,0,e.w*1080,e.h*h);
            if(key.equals("logo")){Bitmap logo=media.get(p.logo);if(logo!=null){float s=Math.min(e.w*1080/logo.getWidth(),e.h*h/logo.getHeight());float w=logo.getWidth()*s,lh=logo.getHeight()*s;canvas.drawBitmap(logo,null,new RectF((e.w*1080-w)/2,(e.h*h-lh)/2,(e.w*1080+w)/2,(e.h*h+lh)/2),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));}}
            else fit(p,key,e).layout.draw(canvas);
            canvas.restoreToCount(layer);
        }
        canvas.restore();
    }
    private void drawPhoto(Canvas c,Project p,Project.Element e,Bitmap b,float opacity,float zoom,float panX){
        float h=p.story?1920:1080;float x=e.x*1080,y=e.y*h,w=e.w*1080,bh=e.h*h;
        int layer=c.saveLayerAlpha(x,y,x+w,y+bh,Math.round(opacity*255));c.translate(x,y);
        Path path=new Path();path.addRoundRect(new RectF(0,0,w,bh),p.template==2?Math.min(w,bh)/2:10,p.template==2?Math.min(w,bh)/2:10,Path.Direction.CW);c.clipPath(path);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        if(b!=null){float[]crop=Geometry.crop(b.getWidth(),b.getHeight(),w,bh,zoom,panX,e.panY);c.drawBitmap(b,null,new RectF(crop[0],crop[1],crop[0]+crop[2],crop[1]+crop[3]),paint);}
        else {
            paint.setColor(0xffe0c29c);c.drawRect(0,0,w,bh,paint);
            // Original line-art bread illustration; usable until a product photo is imported.
            c.save();c.translate(w*.5f,bh*.52f);c.rotate(-18);paint.setColor(0xffb97c43);c.drawOval(new RectF(-w*.30f,-bh*.25f,w*.30f,bh*.25f),paint);paint.setColor(0xfff1d6ae);paint.setStrokeWidth(Math.max(3,w*.025f));
            for(int i=-1;i<=1;i++)c.drawLine(-w*.10f,i*bh*.11f,w*.12f,i*bh*.11f-bh*.08f,paint);c.restore();
        }
        c.restoreToCount(layer);
    }
}
