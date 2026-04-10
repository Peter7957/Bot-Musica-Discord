package com.Pixu.DJ.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.Pixu.DJ.music.GuildMusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

@Service
public class MusicStateService {

  @Autowired
  @Lazy
  private AudioPlayerManager audioPlayerManager;

  @Autowired
  @Lazy
  private MusicService musicService;

  private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

  public GuildMusicManager GetGuildMusicManager(Long guildId) {

    return musicManagers.computeIfAbsent(guildId,
        id -> new GuildMusicManager(audioPlayerManager, musicService, guildId));
  }

}
