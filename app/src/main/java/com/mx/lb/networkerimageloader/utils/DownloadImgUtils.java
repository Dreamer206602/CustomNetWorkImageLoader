package com.mx.lb.networkerimageloader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created on 2015/11/18 20:55
 * Created by Author boobooL
 * 邮箱：boobooMX@163.com
 */
public class DownloadImgUtils {

    public static boolean downloadImgByUrl(String urlStr,File file){
        FileOutputStream fos=null;
        InputStream is=null;
        try{
            URL url=new URL(urlStr);
            HttpURLConnection conn= (HttpURLConnection) url.openConnection();

            is=conn.getInputStream();
            fos=new FileOutputStream(file);
            byte[] buf=new byte[1024];
            int len=0;
            while((len=is.read(buf))!=-1){
                fos.write(buf,0,len);
            }
            fos.flush();
            return true;

        }catch(Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            try {
                if(fos!=null){
                    fos.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return false;

    }





    /**
     * 2、网络图片的压缩
     a、直接下载存到sd卡，然后采用本地的压缩方案。这种方式当前是在硬盘缓存开启的情况下，如果没有开启呢？
     b、使用BitmapFactory.decodeStream(is, null, opts);
     */
    /**
     * 根据url下载图片在指定的文件
     * @param urlStr
     * @param imageView
     * @return
     */
    public static Bitmap downloadImgByUrl(String urlStr,ImageView imageView){
        FileOutputStream fos=null;
        InputStream is=null;
        try{
            URL url=new URL(urlStr);
            HttpURLConnection conn= (HttpURLConnection) url.openConnection();
            is=new BufferedInputStream(conn.getInputStream());
            is.mark(is.available());

            BitmapFactory.Options opts=new BitmapFactory.Options();
            opts.inJustDecodeBounds=true;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

            //获取imageView想要的宽和高
            ImageSizeUtil.ImageSize imageViewSize = ImageSizeUtil.getImageViewSize(imageView);
            opts.inSampleSize=ImageSizeUtil.caculateInSampleSize(opts,imageViewSize.width,imageViewSize.height);

            opts.inJustDecodeBounds=false;
            is.reset();
            bitmap=BitmapFactory.decodeStream(is,null,opts);

            conn.disconnect();
            return bitmap;


        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(is!=null){
                    is.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                if(fos!=null){
                    fos.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return null;
    }

}
