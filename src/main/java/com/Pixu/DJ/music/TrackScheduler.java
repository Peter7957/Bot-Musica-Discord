package com.Pixu.DJ.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import lombok.Getter;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

@Getter
public class TrackScheduler extends AudioEventAdapter {

  private final AudioPlayer player;
  private final BlockingQueue<AudioTrack> queue;
  private MessageChannel announcementChannel;
  private AudioTrack lastTrack;
  private final Long guildId;

  private final MusicService musicService;

  public TrackScheduler(AudioPlayer player, MusicService musicService, Long guildId) {
    this.player = player;
    this.guildId = guildId;
    this.queue = new LinkedBlockingQueue<>();
    this.musicService = musicService;
  }

  public void setAnnouncementChannel(MessageChannel channel) {
    this.announcementChannel = channel;
  }

  @Override
  public void onTrackStart(AudioPlayer player, AudioTrack track) {
    if (player != null && track != null) {
      musicService.saveOnHistory(track, this.guildId);

      if (announcementChannel != null) {
        announcementChannel.sendMessage("🎶 Reproduciendo ahora: **" + track.getInfo().title + "**").queue();
      }

      System.out.println("💾 [TrackStart] Procesada con éxito: " + track.getInfo().title);
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
    player.startTrack(queue.poll(), false);
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

}
