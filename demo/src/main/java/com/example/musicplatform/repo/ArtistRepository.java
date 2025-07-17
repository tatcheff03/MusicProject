package com.example.musicplatform.repo;

import com.example.musicplatform.Data.Artist;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtistRepository extends MongoRepository<Artist, String>{
}
