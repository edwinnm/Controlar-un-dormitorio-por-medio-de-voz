package com.example.edwinnm.usandowit;

import java.io.File;

/**
 * Created by Usuario on 20/05/2018.
 */

public class Musica {

    private String artista;
    private String nombre;
    private String genero;
    private String direccion;
    private File archivo;


    public Musica(String n, String a, String g, String d, File f){

        nombre = n;
        artista = a;
        genero = g;
        direccion = d;
        archivo = f;

    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setArchivo(File archivo) {
        this.archivo = archivo;
    }

    public String getArtista() {
        return artista;
    }

    public String getNombre() {
        return nombre;
    }

    public String getGenero() {
        return genero;
    }

     public String getDireccion() {
        return direccion;
    }

    public File getArchivo() {
        return archivo;
    }
}
