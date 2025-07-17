package com.example.musicplatform.Services;

import com.example.musicplatform.Data.Artist;
import com.example.musicplatform.repo.ArtistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ArtistService {
    private final ArtistRepository artistRepository;
    private final S3Service s3Service;

    @Autowired
    public ArtistService(ArtistRepository artistRepository, S3Service s3Service) {
        this.artistRepository = artistRepository;
        this.s3Service = s3Service;
    }

    public Artist createArtist(Artist artist, MultipartFile profilePhoto) throws IOException {
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String photoKey = s3Service.uploadArtistProfilePhoto(profilePhoto);
            artist.setProfilePhotoUrl(photoKey);
        }
        Artist saved = artistRepository.save(artist);

        // Upload metadata JSON to S3 after saving to DB (so we have the ID)
        s3Service.uploadArtistMetadataJson(saved);
        return saved;
    }


    public Optional<Artist> updateArtist(String id, Artist update, MultipartFile profilePhoto) throws IOException {
        return artistRepository.findById(id).map(existing -> {
            if (update.getName() != null) existing.setName(update.getName());
            if (update.getAge() != null) existing.setAge(update.getAge());
            if (update.getGenre() != null) existing.setGenre(update.getGenre());
            if (update.getSocialMediaLink() != null) existing.setSocialMediaLink(update.getSocialMediaLink());
            if (update.getSongIds() != null) existing.setSongIds(update.getSongIds());

            // Update profile photo if provided
            if (profilePhoto != null && !profilePhoto.isEmpty()) {
                String oldPhoto = existing.getProfilePhotoUrl();
                if (oldPhoto != null && !oldPhoto.isEmpty()) {
                    s3Service.deleteFile(oldPhoto);
                }
                String newPhotoKey = null;
                try {
                    newPhotoKey = s3Service.uploadArtistProfilePhoto(profilePhoto);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                existing.setProfilePhotoUrl(newPhotoKey);
            }
            Artist saved = artistRepository.save(existing);

            // Save updated metadata to S3 as JSON
            try {
                s3Service.uploadArtistMetadataJson(saved);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return saved;
        });
    }

    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    public Optional<Artist> getArtistById(String id) {
        return artistRepository.findById(id);
    }

    public void deleteArtist(String id) {
        artistRepository.findById(id).ifPresent(artist -> {
            // Delete profile photo from S3 if exists
            if (artist.getProfilePhotoUrl() != null && !artist.getProfilePhotoUrl().isEmpty()) {
                s3Service.deleteFile(artist.getProfilePhotoUrl());
            }
            // Delete artist JSON from S3
            String jsonKey = "artist-profiles/" + artist.getId() + ".json";
            s3Service.deleteFile(jsonKey);
            // Delete artist from DB
            artistRepository.deleteById(id);
        });
    }


}
