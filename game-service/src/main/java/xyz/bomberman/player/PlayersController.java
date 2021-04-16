package xyz.bomberman.player;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@AllArgsConstructor
class PlayersController {

  private final PlayersRepository playersRepository;

}
