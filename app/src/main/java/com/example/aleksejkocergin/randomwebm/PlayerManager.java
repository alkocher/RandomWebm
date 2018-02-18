package com.example.aleksejkocergin.randomwebm;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;


public class PlayerManager {

    private final DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private long contentPosition;

    public PlayerManager(Context context) {
        mediaDataSourceFactory =
                new DefaultDataSourceFactory(
                    context,
                    Util.getUserAgent(context, "RandomWebm"),
                    new DefaultBandwidthMeter());
    }

    public void init(Context context, SimpleExoPlayerView simpleExoPlayerView, String videoUrl) {
        // Create a default track selector.
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory trackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);

        // Create a player instance.
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);

        // Bind the player to the view.
        simpleExoPlayerView.setPlayer(player);

        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "RandomWebm"));

        // This is the MediaSource representing the content media.
        MediaSource mediaSource =
                new ExtractorMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(Uri.parse(videoUrl));

        // Prepare the player with yhe source.
        player.seekTo(contentPosition);
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    public void reset() {
        if (player != null) {
            contentPosition = player.getContentPosition();
            player.release();
            player = null;
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}