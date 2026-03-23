package com.Pixu.DJ.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.Pixu.DJ.music.GuildMusicManager;
import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class SlashCommandListener extends ListenerAdapter {
  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();
  private final MusicService musicService;

  public SlashCommandListener(AudioPlayerManager playerManager, MusicService musicService) {
    this.playerManager = playerManager;
    this.musicService = musicService;
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
    Long guildId = guild.getIdLong();
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager, musicService, guildId);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

    return musicManager;
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    String command = event.getName();
    GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());

    // Dentro del método onSlashCommandInteraction de SlashCommandListener

    if (command.equals("pause")) {
      musicManager.player.setPaused(true);
      event.reply("⏸️ Música pausada.").queue();
    }

    if (command.equals("reanudar")) {
      musicManager.player.setPaused(false);
      event.reply("▶️ Música reanudada.").queue();
    }

    if (command.equals("stop")) {
      musicManager.scheduler.stop();
      // Opcional: Desconectar al bot del canal al detener
      event.getGuild().getAudioManager().closeAudioConnection();
      event.reply("⏹️ Reproducción detenida y cola limpiada.").queue();
    }

    if (command.equals("escuchando")) {
      AudioTrack track = musicManager.player.getPlayingTrack();
      if (track == null) {
        event.reply("No hay nada sonando.").queue();
        return;
      }
      event.reply("📻 Escuchando ahora: **" + track.getInfo().title + "**\n" +
          "👤 Autor: `" + track.getInfo().author + "`").queue();
    }

    if (command.equals("skip")) {
      musicManager.scheduler.nextTrack();
      event.reply("⏭️ Saltando canción...").queue();
    }

    if (command.equals("anterior")) {
      if (musicManager.scheduler.getLastTrack() == null) {
        event.reply("❌ No hay una canción anterior registrada.").queue();
      } else {
        musicManager.scheduler.previousTrack();
        event.reply("⏪ Volviendo a la canción anterior...").queue();
      }
    }

    if (event.getName().equals("mix")) {

      String mixName = "History";

      // Conectar al canal de voz si no está
      event.getGuild().getAudioManager().openAudioConnection(
          event.getMember().getVoiceState().getChannel());

      // GUARDAR EL CANAL: Aquí le decimos al scheduler dónde hablar
      musicManager.scheduler.setAnnouncementChannel(event.getChannel());

      // LLAMADA AL SERVICE: El Service hace la magia de H2 -> Player
      musicService.getMixTracks(mixName, event.getGuild().getIdLong(), musicManager);

      event.reply("📂 Cargando tu mix personalizado: **" + mixName + "**").queue();
    }

    if (event.getName().equals("borrar")) {
      AudioTrack track = musicManager.player.getPlayingTrack();

      if (track != null) {
        String identifier = track.getIdentifier();
        Long guildId = event.getGuild().getIdLong();

        musicService.deleteFromMix(identifier, guildId);
        event.reply("🗑️ Canción actual eliminada de la base de datos.").queue();
      } else {
        event.reply("❌ No hay una canción sonando para borrar.").queue();
      }

    }

    // filatramos por el nomber del comando

    if (event.getName().equals("ping")) {
      long time = System.currentTimeMillis();

      event.reply("Pong").setEphemeral(true)
          .flatMap(v -> event.getHook().editOriginalFormat("Pong!: %d ms", System.currentTimeMillis() - time))
          .queue();
    }

    if (event.getName().equals("play")) {
      String trackUrl = event.getOption("url").getAsString();

      // Conectar al canal de voz si no está
      event.getGuild().getAudioManager().openAudioConnection(
          event.getMember().getVoiceState().getChannel());

      // GUARDAR EL CANAL: Aquí le decimos al scheduler dónde hablar
      musicManager.scheduler.setAnnouncementChannel(event.getChannel());

      playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
        @Override
        public void trackLoaded(AudioTrack track) {
          event.reply("✅ Añadido a la cola: **" + track.getInfo().title + "**").queue();
          musicManager.scheduler.queue(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
          AudioTrack selectedTrack = playlist.getSelectedTrack();

          // 1. Definimos el límite (luego este número vendrá desde Angular)
          int limiteMaximo = 15;

          if (selectedTrack != null) {
            event.reply("✨ Mix detectado. Cargando las primeras " + limiteMaximo + " canciones...").queue();
            musicManager.scheduler.queue(selectedTrack);
          } else {
            event.reply("🎶 Añadiendo playlist: **" + playlist.getName() + "**").queue();
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
          event.reply("❌ No encontré nada para: " + trackUrl).setEphemeral(true).queue();
        }

        @Override
        public void loadFailed(FriendlyException exception) {
          event.reply("💥 Error al cargar la canción: " + exception.getMessage()).setEphemeral(true).queue();
        }
      });
    }

  }

}
