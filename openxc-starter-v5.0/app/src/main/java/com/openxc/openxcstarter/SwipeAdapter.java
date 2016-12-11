package com.openxc.openxcstarter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by niulongjia on 2016/11/4.
 */

public class SwipeAdapter extends FragmentPagerAdapter
{

    public List<Fragment> fragments;

    public SwipeAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments=fragments;
    }

    @Override
    public Fragment getItem(int position)
    {


        Fragment fragment=this.fragments.get(position);

        /*
        // a traditional way to transfer data.
        // now we use greenrobot EventBus!
        Bundle bundle = new Bundle();
        bundle.putInt("test",1);
        fragment.setArguments(bundle);
        */
        return fragment;
    }

    @Override
    public int getCount() {
        return 4;
    }
}

