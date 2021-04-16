package xyz.bomberman.room;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
class RoomsController {

  private final RoomsRepository roomsRepository;
}
