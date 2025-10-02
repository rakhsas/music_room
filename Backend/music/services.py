import requests
from django.conf import settings
from events.models import Events


JAMENDO_BASE_URL = settings.JAMENDO_BASE_URL
CLIENT_ID = settings.JAMENDO_CLIENT_ID


def get_jamendo_related(user_id, limit):
    try:
        from users.models import User
        
        # Get the user
        user = User.objects.get(id=user_id)
        
        # Get user's preferred genre from database
        # Assuming you have a genre field or preferred_genres field in User model
        user_genre = getattr(user, 'preferred_genre', None) or getattr(user, 'genre', None)
        
        # If no genre preference, default to 'rock'
        if not user_genre:
            user_genre = 'rock'
        
        url = f"{JAMENDO_BASE_URL}/tracks"
        params = {
            "client_id": CLIENT_ID,
            "format": "json",
            "tags": user_genre,
            "limit": limit,
        }
        response = requests.get(url, params=params)
        return response.json()
        
    except User.DoesNotExist:
        print(f"User {user_id} not found, using default genre")
        # Fallback to default genre if user not found
        url = f"{JAMENDO_BASE_URL}/tracks"
        params = {
            "client_id": CLIENT_ID,
            "format": "json",
            "tags": 'rock',
            "limit": limit,
        }
        response = requests.get(url, params=params)
        return response.json()
    except Exception as e:
        print(f"Error fetching user related tracks: {e}")
        return {"results": []}

def get_playlist_songs(playlist_id):
    """Get songs from user playlist (not Jamendo)"""
    try:
        from playlists.models import Playlist
        # Get the playlist from your database
        playlist = Playlist.objects.get(id=playlist_id)
        
        # Get the track IDs from the playlist
        track_ids = playlist.tracks  # This is your JSON array like ["2257799", "1834483", "1886257"]
        
        if not track_ids:
            return {"results": []}
        
        # Use plus (+) instead of comma for multiple IDs in Jamendo API
        ids_string = "+".join(track_ids)  # "2257799+1834483+1886257"
        
        print(f'Fetching songs for playlist {playlist_id} with track IDs: {ids_string}')
        # Fetch all tracks in one API call
        url = f"{JAMENDO_BASE_URL}/tracks"
        params = {
            "client_id": CLIENT_ID,
            "format": "json",
            "id": ids_string,  # Multiple track IDs with + separator
        }
        print(f'params :{params}')
        response = requests.get(url, params=params)
        return response.json()
        
    except Playlist.DoesNotExist:
        print(f"Playlist {playlist_id} not found")
        return {"results": []}
    except Exception as e:
        print(f"Error fetching playlist songs: {e}")
        return {"results": []}

def get_playlist_songs_individual(playlist_id):
    """Alternative method: fetch tracks individually if batch doesn't work"""
    try:
        from playlists.models import Playlist
        playlist = Playlist.objects.get(id=playlist_id)
        
        track_ids = playlist.tracks
        if not track_ids:
            return {"results": []}
        
        all_tracks = []
        for track_id in track_ids:
            try:
                url = f"{JAMENDO_BASE_URL}/tracks"
                params = {
                    "client_id": CLIENT_ID,
                    "format": "json",
                    "id": track_id,  # Single track ID
                }
                response = requests.get(url, params=params)
                track_data = response.json()
                
                if track_data.get('results'):
                    all_tracks.extend(track_data['results'])
                    
            except Exception as e:
                print(f"Error fetching track {track_id}: {e}")
                continue
        
        return {"results": all_tracks}
        
    except Exception as e:
        print(f"Error: {e}")
        return {"results": []}

def get_popular_artists(limit):
    url = f"{JAMENDO_BASE_URL}/artists"
    params = {
        "client_id": CLIENT_ID,
        "format": "json",
        "limit": limit
    }
    response = requests.get(url, params=params)
    return response.json()

def get_events(limit):
    try:
        # Get only public events from database with limit
        events = Events.objects.filter(is_public=True).select_related('organizer')[:limit]
        
        events_list = []
        for event in events:
            event_data = {
                'id': event.id,
                'name': event.name,
                'event_start_time': event.event_start_time.isoformat(),
                'event_end_time': event.event_end_time.isoformat(),
                'date': event.date.isoformat(),
                'location': event.location,
                'organizer_name': event.organizer.name,
                'attendees_count': event.attendees.count(),
                'is_public': event.is_public,
                'tracks': event.tracks,
            }
            events_list.append(event_data)
        
        return events_list
        
    except Exception as e:
        print(f"Error fetching public events: {e}")
        return []
