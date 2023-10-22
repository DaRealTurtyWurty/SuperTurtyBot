package dev.darealturtywurty.superturtybot.core.api.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pornstar {
    private String careerStatus;
    private String country;
    private int dayOfBirth;
    private String monthOfBirth;
    private int yearOfBirth;
    private String gender;
    private String id;
    private String name;
    private String profession;
    private List<String> photos = new ArrayList<>();
    private List<String> nicknames = new ArrayList<>();
}
