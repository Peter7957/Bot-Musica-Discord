package com.Pixu.DJ.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

@Slf4j
@Getter
public class TrackScheduler extends AudioEventAdapter {

  private final AudioPlayer player;
  private final BlockingQueue<AudioTrack> queue;
  private MessageChannel announcementChannel;
  private AudioTrack lastTrack;
  private final Long guildId;

  private final MusicService musicService;
  private final GuildMusicManager guildMusicManager;

  public TrackScheduler(AudioPlayer player, MusicService musicService, Long guildId, GuildMusicManager guildMusicManager) {
    this.player = player;
    this.guildId = guildId;
    this.queue = new LinkedBlockingQueue<>();
    this.musicService = musicService;
    this.guildMusicManager = guildMusicManager;
  }

  public void setAnnouncementChannel(MessageChannel channel) {
    this.announcementChannel = channel;
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    if (player != null && track != null) {
      if (guildMusicManager != null) {
        guildMusicManager.resetInactivityTimer();
      }
      musicService.saveOnHistory(track, this.guildId);

      if (announcementChannel != null) {
        announcementChannel.sendMessage("🎶 Reproduciendo ahora: **" + track.getInfo().title + "**").queue();
      }

      log.info("[TrackStart] Reproduciendo: {}", track.getInfo().title);
    }
  }

  public void queue(AudioTrack track) {
    if (!player.startTrack(track, true)) {
      queue.offer(track);
    }
  }

  public void nextTrack() {
    AudioTrack current = player.getPlayingTrack();
    if (current != null) {
      this.lastTrack = current.makeClone();
    }
    AudioTrack next = queue.poll();
    if (next == null) {
      player.stopTrack();
      if (guildMusicManager != null) {
        guildMusicManager.startInactivityTimer();
      }
    } else {
      player.startTrack(next, false);
    }
  }

  public void previousTrack() {
    if (lastTrack != null) {
      // Ponemos la actual de vuelta al inicio de la cola y tocamos la anterior
      AudioTrack current = player.getPlayingTrack();
      if (current != null)
        queue.add(current.makeClone());

      player.startTrack(lastTrack.makeClone(), false);
      lastTrack = null; // Limpiamos para evitar bucles infinitos
    }
  }

  public void pause() {
    player.setPaused(true);
  }

  public void resume() {
    player.setPaused(false);
  }

  public void stop() {
    queue.clear(); // Limpiamos la cola
    player.stopTrack(); // Detenemos la canción actual
    player.setPaused(false); // Por si acaso estaba pausado
  }

  // Se ejecuta automáticamente cuando una canción termina
  @Override
  public void onTrackEnd(AudioPlayer player,
      AudioTrack track,
      AudioTrackEndReason endReason) {
    if (endReason.mayStartNext) {
      this.lastTrack = track.makeClone();
      nextTrack();
    } else {
      // No hay más música o fue detenido
      if (guildMusicManager != null) {
        guildMusicManager.startInactivityTimer();
      }
    }
  }

  public void skip10Seconds() {
    AudioTrack track = player.getPlayingTrack();
    if (track != null) {
      long newPosition = track.getPosition() + 10000; // 10 segundos en milisegundos
      if (newPosition < track.getDuration()) {
        track.setPosition(newPosition);
      } else {
        nextTrack(); // Si se pasa del final, pasa a la siguiente pista
      }
    }
  }

  public List<AudioTrack> getQueueList() {
    return new ArrayList<>(queue);
  }

  public int getVolume() {
    return player.getVolume();
  }

  public void setVolume(int volume) {
    player.setVolume(volume);
  }

  public void shuffleQueue() {
    List<AudioTrack> tracks = new ArrayList<>(queue);
    Collections.shuffle(tracks);
    queue.clear();
    queue.addAll(tracks);
  }

}
