package com.blkxltng.mp3player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.alterac.blurkit.BlurKit;
import io.alterac.blurkit.BlurLayout;
import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.ColorFilterTransformation;

public class PlayerFragment extends Fragment {

    private static final String TAG = "PlayerFragment";

    private SimpleExoPlayer simpleExoPlayer;
    private Context mContext;
    private ImageButton imageButtonPlayPause, imageButtonLastTrack, imageButtonNextTrack;
    private TextView textViewProgress, textViewDuration;
    private ImageView imageViewBackground;
    private SeekBar seekBarProgress;
    private Handler handler;
    private Runnable runnable;

    //Used to restore the playback position if orientation is changed
    private boolean restoringState = false;
    private long currentPosition;

//    BlurLayout blurLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getActivity().getApplicationContext() != null)
            mContext = getActivity().getApplicationContext();
        BlurKit.init(mContext);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_player, container, false);
//        blurLayout = view.findViewById(R.id.fragmentPlayer_blurLayout);
        TextView textViewSongName = view.findViewById(R.id.fragmentPlayer_textViewSongName);
        TextView textViewArtistName = view.findViewById(R.id.fragmentPlayer_textViewArtistName);
        TextView textViewAlbumName = view.findViewById(R.id.fragmentPlayer_textViewAlbumName);
        ImageView imageViewAlbumArt = view.findViewById(R.id.fragmentPlayer_imageViewAlbumArt);
        imageViewBackground = view.findViewById(R.id.fragmentPlayer_imageViewBackground);
        imageButtonPlayPause = view.findViewById(R.id.fragmentPlayer_imageButtonPlayPause);
        imageButtonLastTrack = view.findViewById(R.id.fragmentPlayer_imageButtonLastTrack);
        seekBarProgress = view.findViewById(R.id.fragmentPlayer_seekBarProgress);
        textViewProgress = view.findViewById(R.id.fragmentPlayer_textViewProgress);
        textViewDuration = view.findViewById(R.id.fragmentPlayer_textViewDuration);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
//            AssetFileDescriptor dataSource = mContext.getAssets().openFd("lastsurprise.mp3");
            AssetFileDescriptor dataSource = mContext.getAssets().openFd("bensound-summer.mp3");
            mmr.setDataSource(dataSource.getFileDescriptor(), dataSource.getStartOffset(), dataSource.getLength());
        } catch (IOException e) {
            e.printStackTrace();
        }

        String albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        byte[] albumArtData = mmr.getEmbeddedPicture();
        Bitmap albumArt = BitmapFactory.decodeByteArray(albumArtData, 0, albumArtData.length);
        String songName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        final long duration = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        String artistName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        textViewSongName.setText("Title: " + songName);
        textViewArtistName.setText("Artist: " + artistName);
        textViewAlbumName.setText("Album: " + albumName);
        Glide.with(mContext).load(albumArt).into(imageViewAlbumArt);
        MultiTransformation<Bitmap> multi = new MultiTransformation<>(new BlurTransformation(25), new CenterCrop(), new ColorFilterTransformation(Color.argb(150, 255, 255, 255)));
        Glide.with(mContext).load(albumArt)
                .apply(RequestOptions.bitmapTransform(multi))
                .into(imageViewBackground);

//        imageViewBackground.post(new Runnable() {
//            @Override
//            public void run() {
//                BlurKit.getInstance().blur(imageViewBackground, 10);
//            }
//        });
//        BlurKit.getInstance().blur(imageViewBackground, 1);

        String time = String.format(Locale.getDefault(),"%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));

        textViewDuration.setText(time);

        imageButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simpleExoPlayer.setPlayWhenReady(!simpleExoPlayer.getPlayWhenReady());

                if(!simpleExoPlayer.getPlayWhenReady()) {
                    imageButtonPlayPause.setImageDrawable(mContext.getDrawable(android.R.drawable.ic_media_play));
                } else {
                    imageButtonPlayPause.setImageDrawable(mContext.getDrawable(android.R.drawable.ic_media_pause));
                }
            }
        });

        imageButtonLastTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simpleExoPlayer.setPlayWhenReady(false);
                simpleExoPlayer.seekTo(0);
                simpleExoPlayer.setPlayWhenReady(true);
            }
        });

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    long position = progress * duration / 100;
                    Log.d(TAG, "onProgressChanged: progress is " + progress);
                    Log.d(TAG, "onProgressChanged: position is " + position);
                    simpleExoPlayer.seekTo(position);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                simpleExoPlayer.setPlayWhenReady(false);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                simpleExoPlayer.setPlayWhenReady(true);
            }
        });

//        seekBarProgress.setProgress( (int) simpleExoPlayer.getCurrentPosition());
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                seekBarProgress.setProgress((int) ((simpleExoPlayer.getCurrentPosition()*100)/simpleExoPlayer.getDuration()));
                long progress = simpleExoPlayer.getCurrentPosition();
                String progressString = String.format(Locale.getDefault(),"%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(progress),
                        TimeUnit.MILLISECONDS.toMinutes(progress) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(progress)),
                        TimeUnit.MILLISECONDS.toSeconds(progress) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(progress)));
                textViewProgress.setText(progressString);
                handler.postDelayed(runnable, 1000);
            }
        };
        handler.postDelayed(runnable, 0);

        return view;
    }

    private void initializeExoPlayer() {
        final DefaultRenderersFactory renderersFactory =  new DefaultRenderersFactory(mContext, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
        final DefaultTrackSelector trackSelector = new DefaultTrackSelector();
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(renderersFactory,trackSelector);
        final String userAgent = Util.getUserAgent(mContext, "ExoPlayerIntro");
        final ExtractorMediaSource mediaSource = new ExtractorMediaSource(
//                Uri.parse("asset:///lastsurprise.mp3"),
                Uri.parse("asset:///bensound-summer.mp3"),
                new DefaultDataSourceFactory(mContext, userAgent),
                new DefaultExtractorsFactory(),
                null,
                null
        );

        simpleExoPlayer.prepare(mediaSource);
        simpleExoPlayer.setPlayWhenReady(true);

        if(simpleExoPlayer.getPlayWhenReady()) {
            imageButtonPlayPause.setImageDrawable(mContext.getDrawable(android.R.drawable.ic_media_pause));
        }

        //If coming from a configuration change, continue playing from the previous point
        if(restoringState) {
            simpleExoPlayer.seekTo(currentPosition);
            restoringState = false;
        }
    }

    private void releaseExoPlayer() {
        simpleExoPlayer.release();
    }

//    private String covertDuration(long millis) {
//
//        return String.format("%02d:%02d:%02d",
//                TimeUnit.MILLISECONDS.toHours(millis),
//                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
//                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
//    }

    @Override
    public void onStart() {
        super.onStart();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            initializeExoPlayer();
        }
//        blurLayout.startBlur();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            initializeExoPlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            releaseExoPlayer();
        }
    }

    @Override
    public void onStop() {
//        blurLayout.pauseBlur();
        super.onStop();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            releaseExoPlayer();
        }
    }

    //Use onSaveInstanceSTate and onActivityCreated to resume playback on rotation. A simple, "hacky" way of restoring for now.
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong("currentPosition", simpleExoPlayer.getCurrentPosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            restoringState = true;
            currentPosition = savedInstanceState.getLong("currentPosition");
        }
        super.onActivityCreated(savedInstanceState);
    }
}
