package com.mx.lb.networkerimageloader.utils;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;

/**
 * Created on 2015/11/18 20:12
 * Created by Author boobooL
 * 邮箱：boobooMX@163.com
 */
public class ImageSizeUtil {

    /**
     * 根据需求的宽和高以及图片实际的宽和高计算SampleSize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int caculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight){
        int width=options.outWidth;
        int height=options.outHeight;

        int inSampleSize=1;
        if(width>reqWidth || height>reqHeight){
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize=Math.max(widthRadio,heightRadio);
        }
        return  inSampleSize;

    }


    public static class ImageSize{
        int width;
        int height;
    }

    /**
     * 根据ImageView获取适当的压缩的宽和高
     * @param imageView
     * @return
     */
    public static ImageSize getImageViewSize(ImageView imageView){

        ImageSize imageSize=new ImageSize();
        DisplayMetrics displayMetrics=
                imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        int width=imageView.getWidth();//获取ImageView的实际宽度
        if(width<=0){
            width=lp.width;//获取imageView在Layout中声明的宽度
        }
        if(width<=0){
            //width=imageView.getMaxWidth();//检查最大值，最小的API是16
            width=getImageViewFieldValue(imageView,"mMaxWidth");
        }
        if(width<0){
            width=displayMetrics.widthPixels;
        }


        int height=imageView.getHeight();
        if(height<=0){
            height=lp.height;//获取imageView在layout中声明的宽度
        }
        if(height<=0){
            //检查最大值
            height=getImageViewFieldValue(imageView,"mMaxHeight");
        }
        if(height<=0){
            height=displayMetrics.heightPixels;
        }

        imageSize.width=width;
        imageSize.height=height;
        return  imageSize;

    }

    /**
     * 通过反射获取imageView的某个属性值
     * @param object
     * @param filedName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String filedName) {

        int value=0;
         try{
             Field field=ImageView.class.getDeclaredField(filedName);
             field.setAccessible(true);
             int filedValue = field.getInt(object);
             if(filedValue>0 && filedValue<Integer.MAX_VALUE){
                 value=filedValue;
             }

         }catch (Exception e){
             e.printStackTrace();
         }
        return value;

    }

}
