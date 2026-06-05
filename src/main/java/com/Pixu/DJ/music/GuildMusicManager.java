package com.Pixu.DJ.music;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import net.dv8tion.jda.api.audio.AudioSendHandler;

public class GuildMusicManager {

  public final AudioPlayer player;
  public final TrackScheduler scheduler;
  private final AudioPlayerSendHandler sendHandler;
  private final ScheduledExecutorService timeoutExecutor;
  private ScheduledFuture<?> timeoutTask;
  private Runnable disconnectAction;
  private static final long INACTIVITY_TIMEOUT_MINUTES = 5;

  public GuildMusicManager(AudioPlayerManager manager, MusicService musicService, Long guildId) {
    this.player = manager.createPlayer();
    this.scheduler = new TrackScheduler(player, musicService, guildId, this);
    this.player.addListener(scheduler);
    this.sendHandler = new AudioPlayerSendHandler(player);
    this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
  }

  public void setDisconnectAction(Runnable disconnectAction) {
    this.disconnectAction = disconnectAction;
  }

  public void resetInactivityTimer() {
    if (timeoutTask != null) {
      timeoutTask.cancel(false);
      timeoutTask = null;
    }
  }

  public void startInactivityTimer() {
    resetInactivityTimer();
    if (disconnectAction != null) {
      timeoutTask = timeoutExecutor.schedule(disconnectAction, INACTIVITY_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }
  }

  public void shutdown() {
    resetInactivityTimer();
    timeoutExecutor.shutdownNow();
  }

  public AudioSendHandler getSendHandler() {
    return sendHandler;
  }
}
