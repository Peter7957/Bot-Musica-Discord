package com.Pixu.DJ.service;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.Pixu.DJ.models.TrackEntity;
import com.Pixu.DJ.music.GuildMusicManager;
import com.Pixu.DJ.repository.TrackRepository;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import jakarta.transaction.Transactional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

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
      System.out.println("💾 [Service] Guardada en H2: " + trackEntity.getTitle());
    }
  }

  List<TrackEntity> getHistory() {
    return trackRepository.findByMixName("History");
  }

  public void getMixTracks(String mixName, Long guildId, AudioChannel targetVoiceChannel, MessageChannel textChannel) {

    List<TrackEntity> tracks = trackRepository.findByMixNameAndGuildId(mixName, guildId);
    Guild guild = jda.getGuildById(guildId);
    if (guild == null) {
      ResponseEntity.badRequest().body("Servidor no encontrado");
      return;
    }

    AudioChannel voiceChannel = (targetVoiceChannel != null) ? targetVoiceChannel : guild.getVoiceChannels().get(0);

    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
    guild.getAudioManager().openAudioConnection(voiceChannel);

    if (textChannel != null) {
      musicManager.scheduler.setAnnouncementChannel(textChannel);
    }

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

  public void playFromWeb(String trackUrl, Long guildId, AudioChannel targetVoiceChannel, MessageChannel textChannel,
      Consumer<String> successCallback, Consumer<String> errorCallback) {

    Guild guild = jda.getGuildById(guildId);
    if (guild == null) {
      ResponseEntity.badRequest().body("Servidor no encontrado");
      return;
    }

    AudioChannel voiceChannel = (targetVoiceChannel != null) ? targetVoiceChannel : guild.getVoiceChannels().get(0);

    GuildMusicManager musicManager = musicStateService.GetGuildMusicManager(guildId);
    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
    guild.getAudioManager().openAudioConnection(voiceChannel);

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

        // 1. Definimos el límite (luego este número vendrá desde Angular)
        int limiteMaximo = 15;

        if (selectedTrack != null) {
          if (successCallback != null) {
            successCallback.accept("✨ Mix detectado. Cargando las primeras " + limiteMaximo + " canciones...");
          }
          musicManager.scheduler.queue(selectedTrack);
        } else {
          if (successCallback != null) {
            successCallback.accept("🎶 Añadiendo playlist: **" + playlist.getName() + "**");
          }
        }

        // 2. Filtramos y limitamos la lista usando Streams
        List<AudioTrack> tracksToLoad = playlist.getTracks().stream()
            .filter(track -> !track.equals(selectedTrack)) // Evitamos duplicar la seleccionada
            .limit(selectedTrack != null ? limiteMaximo - 1 : limiteMaximo) // Ajustamos el límite
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
          System.out.println("✅ Carga de Mix/Playlist finalizada con éxito.");
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
}
