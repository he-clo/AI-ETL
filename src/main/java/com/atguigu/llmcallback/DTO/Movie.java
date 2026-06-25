package com.atguigu.llmcallback.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Movie {
    public int movieId;
    public String title;
    public String introduction;
    public String genres;
    public float[] embeddingText;
    public float[] embeddingImage;
    public Double similarity;


    public Movie(int movieId, String title, String introduction, String genres, Double similarity) {
        this.movieId = movieId;
        this.title = title;
        this.introduction = introduction;
        this.genres = genres;
        this.similarity = similarity;
    }

    public Movie(float[] embeddingImage, float[] embeddingText, String introduction, String title) {
        this.embeddingImage = embeddingImage;
        this.embeddingText = embeddingText;
        this.introduction = introduction;
        this.title = title;
    }

    public Movie(int movieId, String title, float[] embeddingText, float[] embeddingImage) {
        this.movieId = movieId;
        this.title = title;
        this.embeddingText = embeddingText;
        this.embeddingImage = embeddingImage;
    }
}













