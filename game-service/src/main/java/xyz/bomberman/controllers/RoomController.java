package xyz.bomberman.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.RedirectView;

@Controller
public class RoomController {
  @GetMapping("/")
  RedirectView game() {
    // TODO: serve an HTML with rooms
    return new RedirectView("/game");
  }

  @GetMapping("/rooms")
  ResponseEntity<Resource> game(@Value("classpath:/static/rooms.html") Resource page) {
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
  }
}
