package com.Pixu.DJ.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.Pixu.DJ.listeners.SlashCommandListener;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Configuration
public class DiscordConfig {

  @Value("${discord.token}")
  private String token;

  @Value("${discord.guild-id-test}")
  private String guildIdTest;

  private final SlashCommandListener slashCommandListener;

  public DiscordConfig(SlashCommandListener slashCommandListener) {
    this.slashCommandListener = slashCommandListener;
  }

  @Bean
  public JDA jda() throws InterruptedException {
    JDA jda = JDABuilder.createLight(token)
        .enableIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.MESSAGE_CONTENT)
        .setMemberCachePolicy(MemberCachePolicy.VOICE) // Mantiene en caché a quien esté en voz
        .enableCache(CacheFlag.VOICE_STATE) // Habilita explícitamente el estado de voz
        .addEventListeners(slashCommandListener)
        .setAudioModuleConfig(
            new AudioModuleConfig()
                .withDaveSessionFactory(new JDaveSessionFactory()))
        .build();

    jda.awaitReady();

    Guild guild = jda.getGuildById(guildIdTest);
    if (guild != null) {
      guild.upsertCommand("ping", "Calcula la latencia").queue();
      guild.upsertCommand("play", "Reproduce una canción")
          .addOption(OptionType.STRING, "url", "Link de YouTube", true)
          .queue();
      guild.upsertCommand("escuchando", "Muestra la canción actual").queue();
      guild.upsertCommand("skip", "Salta a la siguiente canción").queue();
      guild.upsertCommand("anterior", "Vuelve a la canción anterior").queue();
      guild.upsertCommand("pause", "Pausa la música actual").queue();
      guild.upsertCommand("reanudar", "Reanuda la música pausada").queue();
      guild.upsertCommand("stop", "Detiene todo y limpia la cola").queue();

      System.out.println("✅ Comandos registrados en el servidor de prueba.");
    }

    return jda;
  }
}
