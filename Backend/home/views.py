from django.shortcuts import render
from django.http import JsonResponse
from users.models import User
from music.services import get_jamendo_related, get_popular_artists, get_events
from music.service.tracks import get_random_songs, get_recent
from playlists.services import get_user_playlists
from drf_yasg.utils import swagger_auto_schema
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status

@api_view(['GET'])
@permission_classes([IsAuthenticated])
@swagger_auto_schema(operation_summary="Get Home Page Data")
def home(request):
    try:
        print(f'request.user: {request.user}')
        print(f'request.user type: {type(request.user)}')
        print(f'request.user.is_authenticated: {request.user.is_authenticated}')
        
        # Check if request.user is actually a User object
        if isinstance(request.user, str):
            # If it's a string, try to get the actual User object
            user = User.objects.get(username=request.user)
            user_id = user.id
        elif hasattr(request.user, 'user_id'):
            # If it's a User object with user_id attribute
            user_id = request.user.user_id
            user = request.user
        elif hasattr(request.user, 'id'):
            # If it's a User object with id attribute
            user_id = request.user.id
            user = request.user
        else:
            return Response({'error': 'Invalid user authentication'}, status=status.HTTP_401_UNAUTHORIZED)
        
        print(f'user_id: {user_id}')
        
        user_playlists = get_user_playlists(user_id, 5) or []
        recommended_songs = get_jamendo_related(user_id, 5) or []
        recently_listened = get_recent(5) or []
        popular_songs = get_random_songs(5) or []
        popular_artists = get_popular_artists(5) or []
        events = get_events(3) or []
        
        # Get user notifications
        event_notifications = []
        playlist_notifications = []
        
        if hasattr(user, 'event_notifications') and user.event_notifications:
            event_notifications = list(user.event_notifications.values())
        
        if hasattr(user, 'playlist_notifications') and user.playlist_notifications:
            playlist_notifications = list(user.playlist_notifications.values())
        
        fullhome = {
            "user_playlists": {"results": user_playlists},
            "recommended_songs": recommended_songs,
            "popular_songs": popular_songs,
            "recently_listened": recently_listened,
            "popular_artists": popular_artists,
            "events": events,
            "notifications": {
                "event_notifications": event_notifications,
                "playlist_notifications": playlist_notifications
            }
        }
        return JsonResponse(fullhome, safe=False)
        
    except User.DoesNotExist:
        return Response({'error': 'User not found'}, status=status.HTTP_404_NOT_FOUND)
    except Exception as e:
        print(f'Error in home view: {str(e)}')
        return Response({'error': 'Internal server error'}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
