package com.kreativ.post;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.util.LruCache;
import java.io.*;
import java.util.*;

/** Imports private byte-for-byte originals; decoding always uses bounded working copies. */
final class MediaLibrary {
    final ProjectStore store;
    private final Context context;
    private final LruCache<String,Bitmap> cache=new LruCache<>(24*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
    };
    MediaLibrary(Context c,ProjectStore s){context=c;store=s;}
    String importUri(Uri uri)throws IOException {
        String name=UUID.randomUUID()+".source";File file=store.media(name);
        try {
            try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)) {
                if(in==null)throw new IOException("The selected file cannot be read. Choose a local image.");
                byte[] buffer=new byte[32768];long size=0;int count;
                while((count=in.read(buffer))!=-1){size+=count;if(size>40L*1024*1024)throw new IOException("Image exceeds 40 MB. Choose a smaller copy.");out.write(buffer,0,count);}
            }
            Bitmap bitmap=decode(file);cache.put(name,bitmap);return name;
        }catch(IOException|RuntimeException|OutOfMemoryError e){file.delete();throw new IOException("Image import failed. Use a JPEG, PNG or WebP under 40 MB. "+e.getMessage(),e);}
    }
    Bitmap get(String name) {
        if(name==null||name.isEmpty())return null;Bitmap b=cache.get(name);if(b!=null)return b;
        try{b=decode(store.media(name));cache.put(name,b);return b;}catch(IOException|RuntimeException|OutOfMemoryError e){return null;}
    }
    private Bitmap decode(File file)throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(file),(decoder,info,source)->{
            String mime=info.getMimeType();if(!Arrays.asList("image/jpeg","image/png","image/webp").contains(mime))throw new IllegalArgumentException("Unsupported input: "+mime);
            int w=info.getSize().getWidth(),h=info.getSize().getHeight();
            if((long)w*h>300_000_000L)throw new IllegalArgumentException("Image dimensions too large");
            double scale=Math.min(1,Math.min(2048d/Math.max(w,h),Math.sqrt(4_000_000d/((long)w*h))));
            decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
        });
    }
    List<String> missing(Project p){List<String> messages=new ArrayList<>();for(int i=0;i<p.photos.size();i++)if(get(p.photos.get(i))==null)messages.add("Photo "+(i+1)+" is missing or unreadable. Replace it using Photos.");if(!p.logo.isEmpty()&&get(p.logo)==null)messages.add("Logo is missing or unreadable. Import it again.");return messages;}
    void clear(){cache.evictAll();}
}
