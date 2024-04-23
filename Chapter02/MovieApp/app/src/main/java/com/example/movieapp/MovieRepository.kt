package com.example.movieapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.movieapp.api.MovieService
import com.example.movieapp.model.Movie

class MovieRepository(private val movieService: MovieService) {
    private val apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhODAwNWVjMDJmYzYzZDQxOGNhNmE4OWJjMGQwMzg1NSIsInN1YiI6IjY2Mjc3ZTZhZTU0ZDVkMDE3ZWVlZTZhZSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.L20ByBvsINDIpVPdAyhlco6syGizFFkPCBWp2ncWHHQ"
    private val apiKey2 = "a8005ec02fc63d418ca6a89bc0d03855"
    private val movieLiveData = MutableLiveData<List<Movie>>()
    private val errorLiveData = MutableLiveData<String>()

    val movies: LiveData<List<Movie>>
        get() = movieLiveData

    val error: LiveData<String>
        get() = errorLiveData

    suspend fun fetchMovies() {
        try {
            val movies = movieService.getMovies(apiKey2)
            movieLiveData.postValue(movies.results)
        } catch (exception: Exception) {
            errorLiveData.postValue("An error occurred: ${exception.message}")
        }
    }
}