package com.example.musicplatform.Data;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("Playlist")
@Getter
@Setter
public class Playlist {
    @Id
    private String id;
    private String title;
    private String description;
    private String coverArtUrl;
    private List<String> songIds;
    private String ownerId;
}
