package com.Pixu.DJ.music;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import dev.lavalink.youtube.YoutubeAudioSourceManager;

@Configuration
public class MusicManager {
  @Value("${youtube.refresh-token}")
  private String youtubeRefreshToken;

  @Bean
  public AudioPlayerManager audioPlayerManager(MusicService musicService) {

    AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    // 1. Aumenta el búfer: Prepara 1 segundo de audio en lugar de los 100ms por
    // defecto
    playerManager.setFrameBufferDuration(1000);
    playerManager.setItemLoaderThreadPoolSize(2);

    playerManager.getConfiguration()
        .setResamplingQuality(com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality.HIGH);

    YoutubeAudioSourceManager youtube = new YoutubeAudioSourceManager();

    youtube.useOauth2(youtubeRefreshToken, true);

    playerManager.registerSourceManager(youtube);

    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);

    return playerManager;
  }

}
