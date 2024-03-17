package sbrt.preppy.server.users;

import lombok.Getter;

import java.util.Random;

@Getter
public class User {
    private final String name;
    private final String idUser;
    public User(String name) {
        this.name = name;
        this.idUser = "u" + new Random().nextLong();
    }
    @Override
    public String toString() {
        return name;
    }
}
