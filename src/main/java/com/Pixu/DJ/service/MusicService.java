package com.Pixu.DJ.service;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.Pixu.DJ.models.TrackEntity;
import com.Pixu.DJ.music.GuildMusicManager;
import com.Pixu.DJ.music.MusicManager;
import com.Pixu.DJ.repository.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import jakarta.transaction.Transactional;

@Service
public class MusicService {

  private final TrackRepository trackRepository;
  private final AudioPlayerManager playerManager;

  public MusicService(TrackRepository trackRepository, @Lazy AudioPlayerManager playerManager) {
    this.trackRepository = trackRepository;
    this.playerManager = playerManager;
  }

  @Transactional
  public void saveOnHistory(AudioTrack track, Long guildId) {
    String identifier = track.getInfo().identifier;

    if (!trackRepository.existsByIdentifierAndMixName(identifier, "History")) {
      TrackEntity trackEntity = TrackEntity.builder()
          .title(track.getInfo().title)
          .author(track.getInfo().author)
          .url(track.getInfo().uri)
          .identifier(identifier)
          .duration(track.getDuration())
          .mixName("History")
          .guildId(guildId)
          .build();

      trackRepository.save(trackEntity);
      System.out.println("💾 [Service] Guardada en H2: " + trackEntity.getTitle());
    }
  }

  List<TrackEntity> getHistory() {
    return trackRepository.findByMixName("History");
  }

  public void getMixTracks(String mixName, Long guildId, GuildMusicManager musicManager) {

    List<TrackEntity> tracks = trackRepository.findByMixNameAndGuildId(mixName, guildId);

    for (TrackEntity entity : tracks) {
      playerManager.loadItemOrdered(musicManager, entity.getUrl(), new AudioLoadResultHandler() {
        @Override
        public void trackLoaded(AudioTrack track) {
          musicManager.scheduler.queue(track);
        }

        // Implementar los otros métodos (vacíos o con logs)
        @Override
        public void playlistLoaded(AudioPlaylist p) {
        }

        @Override
        public void noMatches() {
        }

        @Override
        public void loadFailed(FriendlyException e) {
        }
      });
    }
  }

  public void deleteFromMix(String identifier, Long guildId) {
    trackRepository.deleteByIdentifierAndGuildId(identifier, guildId);
  }

}
