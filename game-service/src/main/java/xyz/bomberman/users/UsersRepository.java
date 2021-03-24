package xyz.bomberman.users;

import java.util.UUID;

interface UsersRepository {

  void save(User user);

  User find(UUID uuid);
}
