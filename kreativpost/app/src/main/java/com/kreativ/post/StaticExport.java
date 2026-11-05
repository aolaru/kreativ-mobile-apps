package com.kreativ.post;
import android.graphics.Bitmap;
import java.io.*;
final class StaticExport {
    static void write(DesignRenderer renderer,Project project,boolean jpeg,OutputStream out)throws IOException {
        if(!renderer.media.missing(project).isEmpty())throw new IOException("Replace missing media before exporting.");
        Bitmap bitmap=renderer.bitmap(project);
        try{if(!bitmap.compress(jpeg?Bitmap.CompressFormat.JPEG:Bitmap.CompressFormat.PNG,jpeg?95:100,out))throw new IOException("Image encoder failed. Try PNG or another save location.");out.flush();}finally{bitmap.recycle();}
    }
}
