package com.Pixu.DJ.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.Pixu.DJ.models.TrackEntity;
import com.Pixu.DJ.repository.TrackRepository;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import jakarta.transaction.Transactional;

@Service
public class MusicService {

  private final TrackRepository trackRepository;

  public MusicService(TrackRepository trackRepository) {
    this.trackRepository = trackRepository;
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
}
