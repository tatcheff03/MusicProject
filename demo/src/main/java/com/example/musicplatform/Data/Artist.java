package com.example.musicplatform.Data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("Artist")
@Getter
@Setter
public class Artist {
    @Id
    private String id;
    private String name;
    private Integer age;
    private String genre;
    private String profilePhotoUrl;
    private String socialMediaLink;
    private List<String> songIds;
}
