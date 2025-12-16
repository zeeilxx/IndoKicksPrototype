package com.example.indokicksprototype.model;

public class PlayerSimple {

    private int id;
    private String name;
    private String position;
    private Integer number;
    private Integer age;
    private String nationality;
    private String photo;

    public PlayerSimple(int id, String name, String position, Integer number,
                        Integer age, String nationality, String photo) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.number = number;
        this.age = age;
        this.nationality = nationality;
        this.photo = photo;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public Integer getNumber() { return number; }
    public Integer getAge() { return age; }
    public String getNationality() { return nationality; }
    public String getPhoto() { return photo; }
}
