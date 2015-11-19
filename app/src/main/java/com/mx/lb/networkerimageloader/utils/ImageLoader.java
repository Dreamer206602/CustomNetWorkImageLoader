package com.mx.lb.networkerimageloader.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created on 2015/11/18 21:36
 * Created by Author boobooL
 * 邮箱：boobooMX@163.com
 */
public class ImageLoader {
    public  static ImageLoader mInstance;

    /**
     * 图片缓存的核心对象
     */
    private LruCache<String,Bitmap>mLruCache;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;
    private static final int DEFAULT_THREAD_COUNT=1;

    //队列的调度方式
    private Type mType=Type.LIFO;

    //任务队列
    private LinkedList<Runnable>mTaskQueue;

    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * UI 线程中的handler
     */
    private Handler mUIHandler;

    private Semaphore mSemaphorePoolThreadHandler=new Semaphore(0);
    private Semaphore mSemaphoreThreadPool;

    private boolean isDiskCacheEnable=true;
    public static final String TAG="ImageLoader";

    public enum Type{
        FIFO,LIFO;
    }

    public  static ImageLoader getInstance(){
        if(mInstance==null){
            synchronized (ImageLoader.class){
                if(mInstance==null){
                    mInstance=new ImageLoader(DEFAULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }

    public  static ImageLoader getInstance(int threadCount,Type type){
        if(mInstance==null){
            synchronized (ImageLoader.class){
                if(mInstance==null){
                    mInstance=new ImageLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }


    private ImageLoader(int threadCount,Type type){
        init(threadCount,type);
    }

    /**
     * 初始化
     * @param threadCount
     * @param type
     */
    private void init(int threadCount, Type type) {

        initBackThread();

        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache=new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };

        //创建线程池
        mThreadPool= Executors.newFixedThreadPool(threadCount);
        mTaskQueue=new LinkedList<>();
        mType=type;
        mSemaphoreThreadPool=new Semaphore(threadCount);
    }

    /**
     * 初始化后台轮询线程
     */
    private void initBackThread() {
        //后台轮询线程
        mPoolThread=new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务执行
                        mThreadPool.execute(getTask());
                        try{
                            mSemaphoreThreadPool.acquire();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();

            }
        };
        mPoolThread.start();


    }

    /**
     * 从任务队列中取出一个方法
     * @return
     */
    private Runnable getTask() {
        if(mType==Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType==Type.LIFO){
            return  mTaskQueue.removeLast();
        }
        return null;
    }
    private class ImgBeanHolder{
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    public void loadImage(final String path,final ImageView imageView, final boolean isFromNet){
        imageView.setTag(path);
        if(mUIHandler==null){
            mUIHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {

                  ImgBeanHolder holder= (ImgBeanHolder) msg.obj;
                    ImageView imageView = holder.imageView;
                    String path = holder.path;
                    Bitmap bm = holder.bitmap;
                    if(imageView.getTag().toString().equals(path)){
                        imageView.setImageBitmap(bm);
                    }


                }
            };
        }

        //根据path在缓存中获取bitmap
        Bitmap bm=getBitmapFromLruCache(path);
        if(bm!=null){
            refreshBitmap(path,imageView,bm);
        }else{
            addTask(buildTask(path,imageView,isFromNet));
        }

    }

    private  synchronized void addTask(Runnable runnable) {

        mTaskQueue.add(runnable);
        try{

            if(mPoolThreadHandler==null){
                mSemaphorePoolThreadHandler.acquire();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);

    }

    private Runnable buildTask(final String path, final ImageView imageView, final boolean isFromNet) {

        return  new Runnable() {
            @Override
            public void run() {
                Bitmap bm=null;
                if(isFromNet){
                    File file=getDiskCacheDir(imageView.getContext(),md5(path));
                    if(file.exists()){//如果在缓存中发现
                        Log.e(TAG,"find image"+path+ "in disk cache");
                        bm=loadImageFromLocal(file.getAbsolutePath(), imageView);
                    }else{
                        if(isDiskCacheEnable){//检测是否开启硬盘缓存
                            boolean downloadState=DownloadImgUtils.downloadImgByUrl(path,file);
                            if(downloadState){//如果下载成功
                                Log.e(TAG,"download image:"+path+" to disk cache .path is"+file.getAbsolutePath());
                                bm=loadImageFromLocal(file.getAbsolutePath(), imageView);
                            }

                        }else{
                            //直接从网络加载
                            Log.e(TAG,"load image:"+path+" to memory.");
                            bm=DownloadImgUtils.downloadImgByUrl(path,imageView);
                        }
                    }
                }else{
                    bm=loadImageFromLocal(path,imageView);
                }

                //3.把图片加入缓存
                addBitmapToLruCache(path,bm);
                refreshBitmap(path, imageView, bm);
                mSemaphoreThreadPool.release();
            }
        };

    }

    /**
     * 获取硬盘内存的文件
     * @param context
     * @param uniqueName
     * @return
     */
    private File getDiskCacheDir(Context context, String uniqueName) {
        String cachePath;
        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
            cachePath=context.getExternalCacheDir().getPath();
        }else{
            cachePath=context.getCacheDir().getPath();
        }

        return new File(cachePath+File.separator+uniqueName);
    }

    private String md5(String str) {
        byte[] digest=null;
        try{
            MessageDigest md = MessageDigest.getInstance("md5");
           digest = md.digest(str.getBytes());
            return bytes2hex02(digest);

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 方式二
     * @param digest
     * @return
     */
    private String bytes2hex02(byte[] digest) {
        StringBuilder sb=new StringBuilder();
        String tmp=null;
        for (byte b:digest) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp=Integer.toHexString(0xFF & b);
            if(tmp.length()==1){
                // 每个字节8为，转为16进制标志，2个16进制位
                tmp="0"+tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();
    }


    /**
     * 添加到内存中
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path)==null){
            if(bm!=null){
                mLruCache.put(path,bm);
            }
        }
    }

    /**
     * 根据path在缓存中获取Bitmap
     * @param key
     * @return
     */
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    /**
     * 从本地中进行加载
     * @param path
     * @param imageView
     * @return
     */
    private Bitmap loadImageFromLocal(String path, ImageView imageView) {
        Bitmap bm;
        //加载图片
        //图片的压缩
        //1.获得图片要显示的大小
        ImageSizeUtil.ImageSize imageSize = ImageSizeUtil.getImageViewSize(imageView);
        //2.压缩图片
        bm=decodeSampledBitmapFromPath(path,imageSize.width,imageSize.height);
        return bm;
    }

    /**
     * 根据图片需要显示的宽和高对图片进行压缩
     * @param path
     * @param width
     * @param height
     * @return
     */
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        // 获得图片的宽和高，并不把图片加载到内存中
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(path,options);

        options.inSampleSize=ImageSizeUtil.caculateInSampleSize(options,width,height);
        //使用得到的InSampleSize再次解析图片
        options.inJustDecodeBounds=false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }


    private void refreshBitmap(String path, ImageView imageView, Bitmap bm) {
        Message message = Message.obtain();

        ImgBeanHolder holder=new ImgBeanHolder();

        holder.bitmap=bm;
        holder.path=path;
        holder.imageView=imageView;
        message.obj=holder;
        mUIHandler.sendMessage(message);

    }


}
