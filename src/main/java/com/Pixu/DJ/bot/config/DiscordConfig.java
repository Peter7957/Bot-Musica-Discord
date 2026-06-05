package com.Pixu.DJ.bot.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.Pixu.DJ.listeners.GuildLeaveListener;
import com.Pixu.DJ.listeners.SlashCommandListener;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Configuration
public class DiscordConfig {

  @Value("${discord.token}")
  private String token;

  @Value("${discord.guild-id-test}")
  private String guildIdTest;

  private final GuildLeaveListener guildLeaveListener;
  private final SlashCommandListener slashCommandListener;
  private JDA jda;

  public DiscordConfig(SlashCommandListener slashCommandListener, GuildLeaveListener guildLeaveListener) {
    this.slashCommandListener = slashCommandListener;
    this.guildLeaveListener = guildLeaveListener;
  }

  @Bean
  public JDA jda() throws InterruptedException {
    this.jda = JDABuilder.createLight(token)
        .enableIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.MESSAGE_CONTENT)
        .setMemberCachePolicy(MemberCachePolicy.VOICE) // Mantiene en caché a quien esté en voz
        .enableCache(CacheFlag.VOICE_STATE) // Habilita explícitamente el estado de voz
        .addEventListeners(slashCommandListener, guildLeaveListener)
        .setAudioModuleConfig(
            new AudioModuleConfig()
                .withDaveSessionFactory(new JDaveSessionFactory()))
        .build();

    jda.awaitReady();

    // Comandos globales (funcionan en todos los servidores)
    // Para desarrollo local descomentar
    // guild.updateCommands() // y comentar jda.updateCommands()
    jda.updateCommands()
        .addCommands(

            Commands.slash("ping", "Calcula la latencia"),
            Commands.slash("play", "Reproduce una canción")
                .addOption(OptionType.STRING, "cancion", "Link de YouTube o nombre de la canción", true),
            Commands.slash("escuchando", "Muestra la canción actual"),
            Commands.slash("skip", "Salta a la siguiente canción"),
            Commands.slash("anterior", "Vuelve a la canción anterior"),
            Commands.slash("pause", "Pausa la música actual"),
            Commands.slash("reanudar", "Reanuda la música pausada"),
            Commands.slash("stop", "Detiene todo y limpia la cola"),
            Commands.slash("queue", "Muestra la cola de reproducción"),
            Commands.slash("shuffle", "Mezcla la cola de reproducción"),
            Commands.slash("volume", "Ajusta el volumen (0-100)")
                .addOption(OptionType.INTEGER, "nivel", "Nivel de volumen entre 0 y 100", false),

            Commands.slash("mix", "Reproduce una mezcla personalizada"),
            Commands.slash("borrar", "Elimina una mezcla personalizada"))
        .queue();

    return this.jda;
  }

  @PreDestroy
  public void shutdown() {
    if (jda != null) {
      jda.shutdown();
      try {
        if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
          jda.shutdownNow();
          jda.awaitShutdown();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
