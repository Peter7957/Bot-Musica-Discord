package com.Pixu.DJ.music;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import net.dv8tion.jda.api.audio.AudioSendHandler;

public class GuildMusicManager {

  public final AudioPlayer player;
  public final TrackScheduler scheduler;
  private final AudioPlayerSendHandler sendHandler;

  public GuildMusicManager(AudioPlayerManager manager, MusicService musicService, Long guildId) {
    this.player = manager.createPlayer();
    this.scheduler = new TrackScheduler(player, musicService, guildId);
    this.player.addListener(scheduler);
    this.sendHandler = new AudioPlayerSendHandler(player);
  }

  public AudioSendHandler getSendHandler() {
    return sendHandler;
  }
}
