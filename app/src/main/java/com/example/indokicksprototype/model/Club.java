package com.example.indokicksprototype.model;

public class Club {

    private int id;
    private String name;
    private String slug;      // ✅ TAMBAH
    private String logo;
    private String country;
    private Integer founded;
    private String stadiumName;
    private String stadiumCity;
    private String stadiumAddress;
    private Integer stadiumCapacity;

    public Club(
            int id,
            String name,
            String slug,       // ✅ TAMBAH
            String logo,
            String country,
            Integer founded,
            String stadiumName,
            String stadiumCity,
            String stadiumAddress,
            Integer stadiumCapacity
    ) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.logo = logo;
        this.country = country;
        this.founded = founded;
        this.stadiumName = stadiumName;
        this.stadiumCity = stadiumCity;
        this.stadiumAddress = stadiumAddress;
        this.stadiumCapacity = stadiumCapacity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }   // ✅ GETTER
    public String getLogo() { return logo; }
    public String getCountry() { return country; }
    public Integer getFounded() { return founded; }
    public String getStadiumName() { return stadiumName; }
    public String getStadiumCity() { return stadiumCity; }
    public String getStadiumAddress() { return stadiumAddress; }
    public Integer getStadiumCapacity() { return stadiumCapacity; }
}
