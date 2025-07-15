package com.example.musicplatform.Data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document("Song")
@Getter
@Setter

public class Song {
    @Id
    private String id;
    private String title;
    private String artist;
    private String genre;
    private String s3Url;
    private Date releaseDate;

    private Integer duration;
    private String album;
    private String coverArtUrl;
}
