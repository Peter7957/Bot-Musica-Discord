package com.Pixu.DJ.listeners;

import java.util.List;

import org.springframework.stereotype.Component;

import com.Pixu.DJ.exception.NoGuildChannelException;
import com.Pixu.DJ.exception.NoVoiceChannelException;
import com.Pixu.DJ.service.MusicService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

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
      event.deferReply().queue(); // Acepta el comando y da más tiempo para procesar

      try {
        String mixName = "History";

        AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
        MessageChannel textChannel = event.getChannel();

        // 2. Validación rápida
        if (voiceChannel == null) {
          event.getHook().sendMessage("❌ ¡Debes estar en un canal de voz!").setEphemeral(true).queue();
          return;
        }

        // LLAMADA AL SERVICE: El Service hace la magia de H2 -> Player
        musicService.getMixTracks(mixName, guildId, voiceChannel, textChannel);

        event.getHook().sendMessage("📂 Cargando tu mix personalizado: **" + mixName + "**").queue();
      } catch (NoVoiceChannelException e) {
        event.getHook().sendMessage("❌ No hay canales de voz disponibles en este servidor.").setEphemeral(true).queue();
      } catch (NoGuildChannelException e) {
        event.getHook().sendMessage("❌ No se pudo encontrar el canal de texto o voz especificado.").setEphemeral(true)
            .queue();
      } catch (Exception e) {
        event.getHook().sendMessage("❌ Ocurrió un error al procesar tu solicitud.").setEphemeral(true).queue();
      }
    }

    if (event.getName().equals("borrar")) {
      boolean isDeleted = musicService.deleteMix(guildId);
      if (!isDeleted) {
        event.reply("❌ No hay una canción sonando para borrar.").queue();
        return;
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
      event.deferReply().queue(); // Acepta el comando y da más tiempo para procesar

      try {

        OptionMapping option = event.getOption("cancion");
        if (option == null) {
          event.getHook().sendMessage("❌ Debes proporcionar una URL o nombre de canción.").setEphemeral(true).queue();
          return;
        }
        String input = option.getAsString();
        String trackUrl = input.startsWith("http") ? input : "ytsearch: " + input;
        AudioChannel voiceChannel = event.getMember().getVoiceState().getChannel();
        MessageChannel textChannel = event.getChannel();

        // Validación rápida
        if (voiceChannel == null) {
          event.getHook().sendMessage("❌ ¡Debes estar en un canal de voz!").setEphemeral(true).queue();
          return;
        }

        musicService.playFromWeb(trackUrl, guildId, voiceChannel, textChannel,
            (successCallback) -> {
              event.getHook().sendMessage(successCallback).queue();
            },
            (errorCallback) -> {
              event.getHook().sendMessage(errorCallback).setEphemeral(true).queue();
            });

      } catch (NoVoiceChannelException e) {
        event.getHook().sendMessage("❌ No hay canales de voz disponibles en este servidor.").setEphemeral(true).queue();
      } catch (NoGuildChannelException e) {
        event.getHook().sendMessage("❌ No se pudo encontrar el canal de texto o voz especificado.").setEphemeral(true)
            .queue();
      } catch (Exception e) {
        e.printStackTrace();
        event.getHook().sendMessage("❌ Error: " + e.getClass().getSimpleName() + " - " + e.getMessage())
            .setEphemeral(true).queue();
      }
    }

    if (event.getName().equals("queue")) {
      List<AudioTrack> queue = musicService.getQueueListTrack(guildId);
      AudioTrack currentTrack = musicService.getCurrentTrack(guildId);

      if (currentTrack == null && queue.isEmpty()) {
        event.reply("La cola está vacía.").setEphemeral(true).queue();
      } else {
        StringBuilder message = new StringBuilder();
        if (currentTrack != null) {
          message.append("🎶 **Reproduciendo ahora:** ").append(currentTrack.getInfo().title).append("\n\n");
        }

        if (!queue.isEmpty()) {
          message.append("📋 **Próximas en la cola:**\n");
          for (AudioTrack track : queue) {
            message.append("- ").append(track.getInfo().title).append("\n");
          }
        }

        event.reply(message.toString()).setEphemeral(true).queue();
      }
    }

    if (event.getName().equals("volume")) {
      OptionMapping VolumeOption = event.getOption("nivel");

      if (VolumeOption != null) {
        int nivel = VolumeOption.getAsInt();

        if (nivel < 0 || nivel > 100) {
          event.reply("❌ El nivel de volumen debe estar entre 0 y 100.").setEphemeral(true).queue();
          return;
        }

        musicService.setVolume(guildId, nivel);
        event.reply("🔊 Volumen ajustado a: " + nivel + "%").queue();
      } else {
        int currentVolume = musicService.getVolume(guildId);
        event.reply("🔊 Volumen actual: " + currentVolume + "%").queue();
      }
    }

    if (event.getName().equals("shuffle")) {
      boolean isShuffled = musicService.shuffleQueue(guildId);
      if (!isShuffled) {
        event.reply("❌ No hay suficientes canciones en la cola para mezclar.").queue();
        return;
      }
      event.reply("🔀 Cola mezclada.").queue();
    }
  }
}
