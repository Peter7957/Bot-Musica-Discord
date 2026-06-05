package com.Pixu.DJ.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.Pixu.DJ.exception.NoGuildChannelException;
import com.Pixu.DJ.exception.NoVoiceChannelException;
import com.Pixu.DJ.models.TrackEntity;
import com.Pixu.DJ.music.GuildMusicManager;
import com.Pixu.DJ.repository.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

@Slf4j
@Service
public class MusicService {

  private final TrackRepository trackRepository;
  private final AudioPlayerManager playerManager;

  @Autowired
  private MusicStateService musicStateService;

  @Autowired
  @Lazy
  private JDA jda;

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
      log.info("💾 [Service] Guardada en H2: {}", trackEntity.getTitle());
    }
  }

  List<TrackEntity> getHistory() {
    return trackRepository.findByMixName("History");
  }

  public void getMixTracks(String mixName, Long guildId, AudioChannel targetVoiceChannel, MessageChannel textChannel) {

    List<TrackEntity> tracks = trackRepository.findByMixNameAndGuildId(mixName, guildId);
    Guild guild = jda.getGuildById(guildId);
    AudioChannel voiceChannel;

    if (guild.getVoiceChannels().isEmpty()) {
      throw new NoVoiceChannelException("No hay canales de voz disponibles en este servidor.");
    }

    voiceChannel = (targetVoiceChannel != null) ? targetVoiceChannel : guild.getVoiceChannels().get(0);

    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(
        guildId);
    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
    guild.getAudioManager().openAudioConnection(voiceChannel);

    // Configurar desconexión por inactividad
    musicManager.setDisconnectAction(() -> {
      guild.getAudioManager().closeAudioConnection();
      log.info("⏰ Bot desconectado por inactividad en guild {}", guildId);
    });

    if (textChannel != null) {
      musicManager.scheduler.setAnnouncementChannel(textChannel);
    }

    Collections.shuffle(tracks);

    for (TrackEntity entity : tracks) {
      playerManager.loadItemOrdered(musicManager, entity.getUrl(), new AudioLoadResultHandler() {
        @Override
        public void trackLoaded(AudioTrack track) {
          musicManager.scheduler.queue(track);
        }

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

  public void playFromWeb(String trackUrl, Long guildId, AudioChannel targetVoiceChannel, MessageChannel textChannel,
      Consumer<String> successCallback, Consumer<String> errorCallback) {

    Guild guild = jda.getGuildById(guildId);

    AudioChannel voiceChannel;

    if (guild == null) {
      throw new NoGuildChannelException("No se encontró el servidor");
    }

    if (guild.getVoiceChannels().isEmpty()) {
      throw new NoVoiceChannelException("No hay canales de voz disponibles en este servidor.");
    }

    voiceChannel = (targetVoiceChannel != null) ? targetVoiceChannel : guild.getVoiceChannels().get(0);

    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
    guild.getAudioManager().openAudioConnection(voiceChannel);

    // Configurar desconexión por inactividad
    musicManager.setDisconnectAction(() -> {
      guild.getAudioManager().closeAudioConnection();
      log.info("⏰ Bot desconectado por inactividad en guild {}", guildId);
    });

    if (textChannel != null) {
      musicManager.scheduler.setAnnouncementChannel(textChannel);
    }

    playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        if (successCallback != null) {
          successCallback.accept("✅ Añadido a la cola: **" + track.getInfo().title + "**");
        }
        musicManager.scheduler.queue(track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack selectedTrack = playlist.getSelectedTrack();
        List<AudioTrack> allTracks = playlist.getTracks();

        // Si es ytsearch (sin track seleccionado), tomamos el primero
        if (selectedTrack == null && !allTracks.isEmpty()) {
          selectedTrack = allTracks.get(0);
        }

        // Variable final para usar en lambdas
        final AudioTrack finalSelectedTrack = selectedTrack;

        // Detectar si es búsqueda por nombre (ytsearch:)
        boolean isSearch = trackUrl.startsWith("ytsearch:");
        int limiteMaximo = isSearch ? 1 : 15;

        if (finalSelectedTrack != null) {
          if (successCallback != null) {
            if (isSearch) {
              successCallback.accept("✅ Añadido a la cola: **" + finalSelectedTrack.getInfo().title + "**");
            } else {
              successCallback.accept("✨ Mix detectado. Cargando las primeras " + limiteMaximo + " canciones...");
            }
          }
          musicManager.scheduler.queue(finalSelectedTrack);
        } else {
          if (successCallback != null) {
            successCallback.accept("❌ No se encontraron resultados.");
          }
          return;
        }

        // 2. Filtramos y limitamos la lista usando Streams
        List<AudioTrack> tracksToLoad = allTracks.stream()
            .filter(track -> !track.equals(finalSelectedTrack)) // Evitamos duplicar la seleccionada
            .limit(isSearch ? 0 : limiteMaximo - 1) // Si es búsqueda, solo 1 canción
            .toList();

        // 3. Hilo separado con "Sleep" para no saturar el ancho de banda
        new Thread(() -> {
          for (AudioTrack track : tracksToLoad) {
            try {
              // Pausa de 800ms entre peticiones.
              Thread.sleep(800);
              musicManager.scheduler.queue(track);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
          log.info("✅ Carga de Mix/Playlist finalizada con éxito.");
        }).start();
      }

      @Override
      public void noMatches() {
        if (errorCallback != null) {
          errorCallback.accept("❌ No encontré nada para: " + trackUrl);
        }
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        if (errorCallback != null) {
          errorCallback.accept("💥 Error al cargar la canción: " + exception.getMessage());
        }
      }
    });
  }

  public boolean pauseTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null && musicManager.player.getPlayingTrack() != null) {
      musicManager.player.setPaused(true);
      return true;
    }
    return false;
  }

  public boolean resumeTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null && musicManager.player.getPlayingTrack() != null) {
      musicManager.player.setPaused(false);
      return true;
    }
    return false;
  }

  public boolean stopTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      musicManager.scheduler.stop();
      return true;
    }
    return false;
  }

  public AudioTrack getCurrentTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      return musicManager.player.getPlayingTrack();
    }
    return null;
  }

  public boolean skipTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      musicManager.scheduler.nextTrack();
      return true;
    }
    return false;
  }

  public boolean previousTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null && musicManager.scheduler.getLastTrack() != null) {
      musicManager.scheduler.previousTrack();
      return true;
    }
    return false;
  }

  public boolean deleteMix(Long guildId) {
    AudioTrack currentTrack = getCurrentTrack(guildId);
    if (currentTrack != null) {
      String identifier = currentTrack.getInfo().identifier;
      trackRepository.deleteByIdentifierAndGuildId(identifier, guildId);
      return true;
    }
    return false;
  }

  public List<AudioTrack> getQueueListTrack(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      return musicManager.scheduler.getQueueList();
    } else {
      return new ArrayList<>();
    }
  }

  public int getVolume(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      return musicManager.player.getVolume();
    }
    return 100;
  }

  public void setVolume(Long guildId, int volume) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null) {
      musicManager.player.setVolume(volume);
    }
  }

  public boolean shuffleQueue(Long guildId) {
    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    if (musicManager != null && !musicManager.scheduler.getQueueList().isEmpty()) {
      musicManager.scheduler.shuffleQueue();
      return true;
    }
    return false;
  }
}
