package sbrt.preppy.grpc;

import lombok.Getter;

import java.util.Random;

/**
 * The simplest chat client model
 */
@Getter
public class User {
    /* ----------------------------- GETTERS ----------------------------- */
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
