package com.Pixu.DJ.listeners;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.Pixu.DJ.service.MusicStateService;

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
public class GuildLeaveListener extends ListenerAdapter {

  @Autowired
  private MusicStateService musicStateService;

  @Override
  public void onGuildLeave(GuildLeaveEvent event) {
    Long guildId = event.getGuild().getIdLong();
    musicStateService.removeGuildMusicManager(guildId);
  }

}
