import requests
from django.conf import settings
from events.models import Events
from users.models import User

JAMENDO_BASE_URL = "https://api.jamendo.com/v3.0"
CLIENT_ID = settings.JAMENDO_CLIENT_ID  # Store this in your .env


def jemendo_artist_search(artist_name):
    """Search for artists by name"""
    url = f"{JAMENDO_BASE_URL}/artists/"
    params = {
        "client_id": CLIENT_ID,
        "namesearch": artist_name,
        "format": "json",
    }
    response = requests.get(url, params=params)
    return response.json()
