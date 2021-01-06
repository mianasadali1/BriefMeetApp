package com.subhadip.briefmeet.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


import com.subhadip.briefmeet.bean.Intro;
import com.subhadip.briefmeet.databinding.ItemviewIntroBinding;

import java.util.ArrayList;


public class IntroPagerAdapter extends PagerAdapter {

    ArrayList<Intro> arrSlider;
    LayoutInflater inflater;
    Context context;

    public IntroPagerAdapter(Context context, ArrayList<Intro> arrSlider) {
        this.context = context;
        this.arrSlider = arrSlider;
    }

    @Override
    public int getCount() {
        return arrSlider.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((LinearLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ItemviewIntroBinding binding = ItemviewIntroBinding.inflate(inflater, container, false);

        binding.title.setText(arrSlider.get(position).getTitle());
        binding.animation.setAnimation(arrSlider.get(position).getAnimation());
        binding.animation.playAnimation();
        binding.message.setText(arrSlider.get(position).getMessage());

        //add item.xml to viewpager
        ((ViewPager) container).addView(binding.getRoot());
        return binding.getRoot();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        // Remove viewpager_item.xml from ViewPager
        ((ViewPager) container).removeView((LinearLayout) object);
    }

    /*@Override
    public float getPageWidth(int position) {
        return .20f;   //it is used for set page widht of view pager
    }*/
}
