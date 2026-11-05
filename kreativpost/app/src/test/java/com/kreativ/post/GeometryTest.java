package com.kreativ.post;
import org.junit.Test;
import static org.junit.Assert.*;
public class GeometryTest {
    @Test public void cropNeverLeavesEmptyEdges(){for(int w:new int[]{100,1000,8000})for(int h:new int[]{50,500,12000})for(float zoom:new float[]{1,2,3})for(float pan:new float[]{0,.5f,1}){float[] r=Geometry.crop(w,h,600,900,zoom,pan,pan);assertTrue(r[0]<=.01);assertTrue(r[1]<=.01);assertTrue(r[0]+r[2]>=599.99);assertTrue(r[1]+r[3]>=899.99);}}
    @Test public void snappingRespectsMarginsAndBounds(){assertEquals(.05f,Geometry.snap(.056f,.4f),.0001);assertEquals(.3f,Geometry.snap(.301f,.4f),.0001);assertEquals(.6f,Geometry.snap(1,.4f),.0001);assertEquals(0,Geometry.snap(-.4f,.4f),.0001);}
}
