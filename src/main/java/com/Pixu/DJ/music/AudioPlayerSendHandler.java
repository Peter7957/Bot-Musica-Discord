package com.Pixu.DJ.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.nio.ByteBuffer;

public class AudioPlayerSendHandler implements AudioSendHandler {
  private final AudioPlayer audioPlayer;
  private final ByteBuffer buffer = ByteBuffer.allocate(1024);
  private final MutableAudioFrame frame = new MutableAudioFrame();

  public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
    this.frame.setBuffer(buffer);
  }

  @Override
  public boolean canProvide() {
    return audioPlayer.provide(frame);
  }

  @Override
  public ByteBuffer provide20MsAudio() {
    return (ByteBuffer) buffer.flip();
  }

  @Override
  public boolean isOpus() {
    return true;
  }
}
