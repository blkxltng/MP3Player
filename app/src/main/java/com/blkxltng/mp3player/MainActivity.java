package com.blkxltng.mp3player;

import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.exoplayer2.Player;

import io.alterac.blurkit.BlurLayout;

public class MainActivity extends SingleFragmentActivity {

//    BlurLayout blurLayout;

    @Override
    protected Fragment createFragment() {
        return new PlayerFragment();
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_player);
//
//        blurLayout = findViewById(R.id.activityPlayer_blurLayout);
//    }

    @Override
    protected void onStart() {
        super.onStart();
//        blurLayout.startBlur();
    }

    @Override
    protected void onStop() {
//        blurLayout.pauseBlur();
        super.onStop();
    }
}
