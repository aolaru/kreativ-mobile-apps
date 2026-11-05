package com.kreativ.post;

import android.content.Context;
import android.graphics.*;
import android.view.*;

final class EditorCanvas extends View {
    interface Listener { void select(String key);void beforeChange();void changed(); }
    Project project; final DesignRenderer renderer;Listener listener;String selected="headline";
    boolean editable=true;private float startX,startY,x,y,w,h;private boolean resize,drag,moved;
    EditorCanvas(Context c,DesignRenderer r,Project p){super(c);renderer=r;project=p;setContentDescription("Promotion preview. Select an element below for accessible position and size controls.");setFocusable(true);}
    @Override protected void onMeasure(int ws,int hs){int width=MeasureSpec.getSize(ws);setMeasuredDimension(width,Math.round(width*(project.story?1920f/1080:1)));}
    @Override protected void onDraw(Canvas c){renderer.draw(c,project,getWidth(),getHeight(),-1);if(!editable)return;
        Project.Element e=project.layout().get(selected);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(0xff28594c);paint.setStrokeWidth(2*getResources().getDisplayMetrics().density);paint.setStyle(Paint.Style.STROKE);
        RectF box=new RectF(e.x*getWidth(),e.y*getHeight(),(e.x+e.w)*getWidth(),(e.y+e.h)*getHeight());c.drawRect(box,paint);paint.setStyle(Paint.Style.FILL);c.drawCircle(box.right,box.bottom,9*getResources().getDisplayMetrics().density,paint);
        if(drag){paint.setColor(0xff9e472f);paint.setStrokeWidth(getResources().getDisplayMetrics().density);for(float line:new float[]{.05f,.5f,.95f}){c.drawLine(line*getWidth(),0,line*getWidth(),getHeight(),paint);c.drawLine(0,line*getHeight(),getWidth(),line*getHeight(),paint);}}
    }
    @Override public boolean onTouchEvent(android.view.MotionEvent ev){if(!editable)return false;
        float px=ev.getX()/getWidth(),py=ev.getY()/getHeight();
        if(ev.getAction()==MotionEvent.ACTION_DOWN){
            Project.Element active=project.layout().get(selected);float tolerance=24*getResources().getDisplayMetrics().density/getWidth();resize=Math.abs(px-active.x-active.w)<tolerance&&Math.abs(py-active.y-active.h)<tolerance;
            if(!resize){String hit=null;for(int i=Project.ELEMENTS.length-1;i>=0;i--){String key=Project.ELEMENTS[i];Project.Element e=project.layout().get(key);if(px>=e.x&&px<=e.x+e.w&&py>=e.y&&py<=e.y+e.h){hit=key;break;}}if(hit==null)return false;selected=hit;listener.select(hit);active=project.layout().get(selected);}
            startX=px;startY=py;x=active.x;y=active.y;w=active.w;h=active.h;drag=true;moved=false;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
        }
        if(ev.getAction()==MotionEvent.ACTION_MOVE&&drag){if(!moved){listener.beforeChange();moved=true;}Project.Element e=project.layout().get(selected);
            if(resize){e.w=Geometry.clamp(w+px-startX,.05f,1-e.x);e.h=Geometry.clamp(h+py-startY,.03f,1-e.y);}
            else{e.x=Geometry.snap(x+px-startX,w);e.y=Geometry.snap(y+py-startY,h);}invalidate();return true;
        }
        if(ev.getAction()==MotionEvent.ACTION_UP||ev.getAction()==MotionEvent.ACTION_CANCEL){drag=false;getParent().requestDisallowInterceptTouchEvent(false);if(moved)listener.changed();else performClick();invalidate();return true;}return drag;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
