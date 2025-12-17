package com.example.indokicksprototype.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class PlayerDetail implements Parcelable {

    private int id;
    private String name;
    private String photo;

    private Integer age;
    private String nationality;

    // dari statistics[0].games
    private String position;
    private Integer number;
    private Integer appearences;
    private Integer lineups;
    private Integer minutes;
    private String rating;

    // tambahan (kalau ada)
    private String birthDate;
    private String birthPlace;
    private String height;
    private String weight;

    public PlayerDetail(int id, String name, String photo,
                        Integer age, String nationality,
                        String position, Integer number,
                        Integer appearences, Integer lineups, Integer minutes, String rating,
                        String birthDate, String birthPlace,
                        String height, String weight) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.age = age;
        this.nationality = nationality;
        this.position = position;
        this.number = number;
        this.appearences = appearences;
        this.lineups = lineups;
        this.minutes = minutes;
        this.rating = rating;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.height = height;
        this.weight = weight;
    }

    protected PlayerDetail(Parcel in) {
        id = in.readInt();
        name = in.readString();
        photo = in.readString();

        age = (Integer) in.readValue(Integer.class.getClassLoader());
        nationality = in.readString();

        position = in.readString();
        number = (Integer) in.readValue(Integer.class.getClassLoader());
        appearences = (Integer) in.readValue(Integer.class.getClassLoader());
        lineups = (Integer) in.readValue(Integer.class.getClassLoader());
        minutes = (Integer) in.readValue(Integer.class.getClassLoader());
        rating = in.readString();

        birthDate = in.readString();
        birthPlace = in.readString();
        height = in.readString();
        weight = in.readString();
    }

    public static final Creator<PlayerDetail> CREATOR = new Creator<PlayerDetail>() {
        @Override
        public PlayerDetail createFromParcel(Parcel in) {
            return new PlayerDetail(in);
        }

        @Override
        public PlayerDetail[] newArray(int size) {
            return new PlayerDetail[size];
        }
    };

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhoto() { return photo; }
    public Integer getAge() { return age; }
    public String getNationality() { return nationality; }

    public String getPosition() { return position; }
    public Integer getNumber() { return number; }
    public Integer getAppearences() { return appearences; }
    public Integer getLineups() { return lineups; }
    public Integer getMinutes() { return minutes; }
    public String getRating() { return rating; }

    public String getBirthDate() { return birthDate; }
    public String getBirthPlace() { return birthPlace; }
    public String getHeight() { return height; }
    public String getWeight() { return weight; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(photo);

        dest.writeValue(age);
        dest.writeString(nationality);

        dest.writeString(position);
        dest.writeValue(number);
        dest.writeValue(appearences);
        dest.writeValue(lineups);
        dest.writeValue(minutes);
        dest.writeString(rating);

        dest.writeString(birthDate);
        dest.writeString(birthPlace);
        dest.writeString(height);
        dest.writeString(weight);
    }
}
