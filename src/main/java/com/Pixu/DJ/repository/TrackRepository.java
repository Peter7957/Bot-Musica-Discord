package com.Pixu.DJ.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.Pixu.DJ.models.TrackEntity;

import jakarta.transaction.Transactional;

public interface TrackRepository extends JpaRepository<TrackEntity, Long> {

  List<TrackEntity> findByMixName(String mixName);

  boolean existsByIdentifierAndMixName(String identifier, String mixName);

  List<TrackEntity> findByMixNameAndGuildId(String mixName, Long guildId);

  @Transactional
  void deleteByIdentifierAndGuildId(String identifier, Long guildId);

}
