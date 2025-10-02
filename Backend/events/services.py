from .models import Events
import requests
import os
from django.conf import settings
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny

CLIENT_ID = settings.JAMENDO_CLIENT_ID  # Ensure this is set in your settings or .env file
JAMENDO_BASE_URL = settings.JAMENDO_BASE_URL  # Ensure this is set in your settings or .env file

def get_event_songs(event_id):
    """Get songs from event with full details from Jamendo API"""
    try:
        # Get the event from your database
        event = Events.objects.get(id=event_id)
        
        # Get the track IDs from the event
        track_ids = event.songs  # This is your JSON array like ["2257799", "1834483", "1886257"]
        
        if not track_ids:
            return {"results": []}
        
        # Use plus (+) instead of comma for multiple IDs in Jamendo API
        ids_string = "+".join(track_ids)  # "2257799+1834483+1886257"
        
        print(f'Fetching songs for event {event_id} with track IDs: {ids_string}')
        # Fetch all tracks in one API call
        url = f"{JAMENDO_BASE_URL}/tracks"
        params = {
            "client_id": CLIENT_ID,
            "format": "json",
            "id": ids_string,  # Multiple track IDs with + separator
            "include": "musicinfo",  # Include music info for MP3 URLs
        }
        print(f'params: {params}')
        response = requests.get(url, params=params)
        return response.json()
        
    except Events.DoesNotExist:
        print(f"Event {event_id} not found")
        return {"results": []}
    except Exception as e:
        print(f"Error fetching event songs: {e}")
        return {"results": []}