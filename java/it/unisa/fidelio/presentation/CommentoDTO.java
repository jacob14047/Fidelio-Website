package it.unisa.fidelio.presentation;

public record CommentoDTO(
        int id,
        String testo,
        String username,
        String date,
        String avatarInitial,
        String dtype
) {


    public int getId() {
        return id;
    }

    public String getTesto() {
        return testo;
    }

    public String getUsername() {
        return username;
    }

    public String getDate() {
        return date;
    }

    public String getAvatarInitial() {
        return avatarInitial;
    }

    public String getDtype(){
        return dtype;
    }
}