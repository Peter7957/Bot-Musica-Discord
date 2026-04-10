package com.Pixu.DJ.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Pixu.DJ.service.MusicService;

@RestController
@RequestMapping("api/music")
public class MusicRestController {

  @Autowired
  private MusicService musicService;

  @PostMapping("/play")
  public ResponseEntity<Map<String, String>> play(@RequestBody Map<String, String> request) {
    try {
      Long guildId = Long.parseLong(request.get("guildId"));
      String trackUrl = request.get("trackUrl");

      musicService.playFromWeb(trackUrl, guildId, null, null, null, null);

      return ResponseEntity.ok(Map.of("message", "Reproduciendo la pista"));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("error", "Error al reproducir la pista: " + e.getMessage()));
    }

  }

}
