import requests
from django.conf import settings
from events.models import Events
from users.models import User

JAMENDO_BASE_URL = "https://api.jamendo.com/v3.0"
CLIENT_ID = settings.JAMENDO_CLIENT_ID  # Store this in your .env

def get_jamendo_track(track_id):
    url = f"{JAMENDO_BASE_URL}/tracks"
    print(f"Fetching track details for ID: {track_id}")
    params = {
        "client_id": CLIENT_ID,
        "format": "json",
        "id": track_id
    }
    response = requests.get(url, params=params)
    return response.json()

def get_recent(user_id, limit=5):
    try:
        
        user = User.objects.get(id=user_id)
        recent_track_ids = user.recently_played or []
        
        # If no recent tracks, return random tracks from Jamendo
        if not recent_track_ids:
            return get_random_songs(limit)
        
        recent_track_ids = recent_track_ids[-limit:] if len(recent_track_ids) > limit else recent_track_ids
        recent_tracks = []
        for track_id in reversed(recent_track_ids):  # Most recent first
            try:
                url = f"{JAMENDO_BASE_URL}/tracks"
                params = {
                    "client_id": CLIENT_ID,
                    "format": "json",
                    "id": track_id,
                    "limit": 1
                }
                response = requests.get(url, params=params)
                track_data = response.json()
                
                if track_data.get('results'):
                    recent_tracks.extend(track_data['results'])
                    
            except Exception as e:
                print(f"Error fetching track {track_id}: {e}")
                continue
        
        return recent_tracks
        
    except User.DoesNotExist:
        print(f"User {user_id} not found")
        return get_random_songs(limit)
    except Exception as e:
        print(f"Error fetching recent tracks: {e}")
        return []

def get_user_related(user_id, limit):
    try:
        user = User.objects.get(id=user_id)
        user_genres = user.genres or [] 
        if not user_genres:
            return get_random_songs(limit)
        
        #import random
        #selected_genre = random.choice(user_genres)  
        
        url = f"{JAMENDO_BASE_URL}/tracks"
        params = {
            "client_id": CLIENT_ID,
            "format": "json",
            "tags": user_genres,  # Use user's genre instead of hardcoded 'rock'
            "limit": limit
        }
        response = requests.get(url, params=params)
        return response.json()
        
    except User.DoesNotExist:
        print(f"User {user_id} not found")
        return get_random_songs(limit)
    except Exception as e:
        print(f"Error fetching related tracks: {e}")
        return get_random_songs(limit)

def get_random_songs(limit):
    url = f"{JAMENDO_BASE_URL}/tracks"
    params = {
        "client_id": CLIENT_ID,
        "format": "json",
        "limit": limit
    }
    response = requests.get(url, params=params)
    return response.json()

def jamendo_track_search(query):
    """Search for tracks by name"""
    url = f"{JAMENDO_BASE_URL}/tracks"
    params = {
        "client_id": CLIENT_ID,
        "format": "json",
        "namesearch": query,
        "limit": 10
    }
    response = requests.get(url, params=params)
    print(f"Search query: {query}, Response status: {response.status_code}")
    return response