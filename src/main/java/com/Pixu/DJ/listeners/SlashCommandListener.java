package com.Pixu.DJ.listeners;

import org.springframework.stereotype.Component;

import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class SlashCommandListener extends ListenerAdapter {
  private final MusicService musicService;

  public SlashCommandListener(
      MusicService musicService) {
    this.musicService = musicService;
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    String command = event.getName();
    Long guildId = event.getGuild().getIdLong();

    // Dentro del método onSlashCommandInteraction de SlashCommandListener

    if (command.equals("pause")) {
      boolean isPaused = musicService.pauseTrack(guildId);
      if (!isPaused) {
        event.reply("❌ No hay una canción sonando para pausar.").queue();
        return;
      }
      event.reply("⏸️ Música pausada.").queue();
    }

    if (command.equals("reanudar")) {
      boolean isResumed = musicService.resumeTrack(guildId);
      if (!isResumed) {
        event.reply("❌ No hay una canción pausada para reanudar.").queue();
        return;
      }
      event.reply("▶️ Música reanudada.").queue();
    }

    if (command.equals("stop")) {
      boolean isStop = musicService.stopTrack(guildId);
      if (!isStop) {
        event.reply("❌ No hay una canción sonando para detener.").queue();
        return;
      }
      // Opcional: Desconectar al bot del canal al detener
      event.getGuild().getAudioManager().closeAudioConnection();
      event.reply("⏹️ Reproducción detenida y cola limpiada.").queue();
    }

    if (command.equals("escuchando")) {
      AudioTrack track = musicService.getCurrentTrack(guildId);
      if (track == null) {
        event.reply("No hay nada sonando.").queue();
        return;
      }
      event.reply("📻 Escuchando ahora: **" + track.getInfo().title + "**\n" +
          "👤 Autor: `" + track.getInfo().author + "`").queue();
    }

    if (command.equals("skip")) {
      boolean isSkipped = musicService.skipTrack(guildId);
      if (!isSkipped) {
        event.reply("❌ No hay una canción sonando para saltar.").queue();
        return;
      }
      event.reply("⏭️ Saltando canción...").queue();
    }

    if (command.equals("anterior")) {
      boolean hasPrevious = musicService.previousTrack(guildId);
      if (!hasPrevious) {
        event.reply("❌ No hay una canción anterior registrada.").queue();
        return;
      }
      event.reply("⏪ Volviendo a la canción anterior...").queue();
    }

    if (event.getName().equals("mix")) {

      String mixName = "History";

      AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
      MessageChannel textChannel = event.getChannel();

      // 2. Validación rápida
      if (voiceChannel == null) {
        event.reply("❌ ¡Debes estar en un canal de voz!").setEphemeral(true).queue();
        return;
      }

      // LLAMADA AL SERVICE: El Service hace la magia de H2 -> Player
      musicService.getMixTracks(mixName, guildId, voiceChannel, textChannel);

      event.reply("📂 Cargando tu mix personalizado: **" + mixName + "**").queue();
    }

    if (event.getName().equals("borrar")) {
      boolean isDeleted = musicService.deleteMix(guildId);
      if (!isDeleted) {
        event.reply("❌ No hay una canción sonando para borrar.").queue();
      }
      event.reply("🗑️ Canción actual eliminada de la base de datos.").queue();
    }

    // filatramos por el nomber del comando

    if (event.getName().equals("ping")) {
      long time = System.currentTimeMillis();

      event.reply("Pong").setEphemeral(true)
          .flatMap(v -> event.getHook().editOriginalFormat("Pong!: %d ms",
              System.currentTimeMillis() - time))
          .queue();
    }

    if (event.getName().equals("play")) {
      String trackUrl = event.getOption("url").getAsString();
      AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
      MessageChannel textChannel = event.getChannel();

      // Validación rápida
      if (voiceChannel == null) {
        event.reply("❌ ¡Debes estar en un canal de voz!").setEphemeral(true).queue();
        return;
      }

      musicService.playFromWeb(trackUrl, guildId, voiceChannel, textChannel,
          (successCallback) -> {
            event.reply(successCallback).queue();
          },
          (errorCallback) -> {
            event.reply(errorCallback).setEphemeral(true).queue();
          });
    }
  }
}
