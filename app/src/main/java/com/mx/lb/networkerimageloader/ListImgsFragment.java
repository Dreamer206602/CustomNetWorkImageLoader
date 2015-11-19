package com.mx.lb.networkerimageloader;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;

import com.mx.lb.networkerimageloader.utils.ImageLoader;
import com.mx.lb.networkerimageloader.utils.Images;

/**
 * Created on 2015/11/19 14:27
 * Created by Author boobooL
 * 邮箱：boobooMX@163.com
 */
public class ListImgsFragment extends Fragment {

    private GridView mGridView;
    private String[] mUrlStrs= Images.imageThumbUrls;
    private ImageLoader mImageLoader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageLoader=ImageLoader.getInstance(3, ImageLoader.Type.LIFO);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

       View ret=inflater.inflate(R.layout.fragment_list_imgs,container,false);
        mGridView= (GridView) ret.findViewById(R.id.id_gridview);

        setUpAdapter();

        return ret;
    }

    private void setUpAdapter() {

        if(getActivity()==null || mGridView==null){
            return;
        }
        if(mUrlStrs!=null){
            mGridView.setAdapter(new ListImgItemAdapter(getActivity(),0,mUrlStrs) );
        }else{
            mGridView.setAdapter(null);
        }

    }

    private class ListImgItemAdapter extends ArrayAdapter<String>{

        public ListImgItemAdapter(Context context, int resource, String[] datas) {
            super(getActivity(), 0, datas);
            Log.e("TAG","ListImgItemAdapter");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView=getActivity().getLayoutInflater().inflate(R.layout.item_fragment_list_imgs,parent,false);

            }
            ImageView imageView= (ImageView) convertView.findViewById(R.id.id_img);
            imageView.setImageResource(R.mipmap.ic_launcher);
            mImageLoader.loadImage(getItem(position),imageView,true);
            return convertView;
        }
    }
}
