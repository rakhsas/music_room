from django.urls import path
from . import views
from music.view import  tracks, artists

urlpatterns = [

    #artist-related URLs
    path('songs/', tracks.song_list, name='song-list'),
    path('songs/<int:track_id>/', tracks.song_file, name='song-detail'),
    path('related/', tracks.related_list, name='related-list'),
    path('random-songs/', tracks.random_songs, name='random-songs'),
    path('tracks/search-name/<str:query>/', tracks.track_search, name='track-search-query'),
    
    path('artists/', artists.artist_list, name='artist-list'),
    path('artists/<int:artist_id>/', artists.artist_detail, name='artist-detail'),
    path('artists/search/<str:query>/', artists.artist_search, name='artist-search-query'),
    path('artists/popular/', artists.popular_artists, name='popular-artists'),
    #path('events/', tracks.event_list, name='event-list'),
    #path('search/', tracks.search, name='search'),
    #path('search/<str:query>/', tracks.search, name='search-query'),
    #path('search/<str:query>/<int:limit>/', tracks.search, name='search-query-limit'),
    #path('search/<str:query>/<int:limit>/<str:sort_by>/', tracks.search, name='search-query-limit-sort'),
    #path('artists/<int:artist_id>/', tracks.artist_detail, name='artist-detail'),

]