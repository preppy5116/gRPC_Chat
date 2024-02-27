package sbrt.preppy.userchat;

import java.util.Random;

public class User {
    private String name;
    private final String idUser;

    /* ----------------------------- CONSTRUCTOR ----------------------------- */
    public User() {
        this.idUser = "u" + new Random().nextLong();
    }

    public User(String name) {
        this.name = name;
        this.idUser = "u" + new Random().nextLong();
    }

    /* ----------------------------- GETTERS ----------------------------- */
    public String getName() {
        return name;
    }

    public String getIdUser() {
        return idUser;
    }

    /* ----------------------------- SETTERS ----------------------------- */
    public void setName(String name) {
        this.name = name;
    }

    /* ----------------------------- OVERRIDE ----------------------------- */
    @Override
    public String toString() {
        return name;
    }
}
