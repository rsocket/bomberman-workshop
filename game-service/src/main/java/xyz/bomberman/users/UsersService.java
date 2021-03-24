package xyz.bomberman.users;

import java.util.UUID;

public interface UsersService {

  void register(UUID uuid, String name);

  void disconnect(UUID uuid);

  User find(UUID uuid);
}
