package com.example.musicplatform.Data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document("Album")
@Getter
@Setter
public class Album {
    @Id
    private String id;
    private String title;
    private String coverArtUrl;
    private List<String> songIds;
    private String artistId;
    private Date releaseDate;
}
