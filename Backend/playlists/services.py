from .models import Playlist
from users.models import User

def get_user_playlist_stats(user):
    """Get user's playlist statistics"""
    try:
        owned_playlists = Playlist.objects.filter(owner=user).count()
        followed_playlists = Playlist.objects.filter(followers=user).count()
        total_tracks = sum(len(p.tracks) for p in Playlist.objects.filter(owner=user))
        
        return {
            'owned_playlists': owned_playlists,
            'followed_playlists': followed_playlists,
            'total_tracks': total_tracks,
        }
    except Exception as e:
        return {'error': str(e)}

def get_popular_playlists(limit=10):
    """Get most followed user playlists (no Jamendo)"""
    try:
        playlists = Playlist.objects.filter(
            is_public=True
        ).order_by('-followers__count')[:limit]
        
        return playlists
    except Exception as e:
        return []

def search_playlists(query, user=None):
    """Search user playlists by name (no Jamendo)"""
    try:
        playlists = Playlist.objects.filter(
            name__icontains=query,
            is_public=True
        )
        
        if user and user.is_authenticated:
            # Also include user's private playlists
            user_playlists = Playlist.objects.filter(
                name__icontains=query,
                owner=user
            )
            playlists = playlists.union(user_playlists)
        
        return playlists[:20]
    except Exception as e:
        return []
    
def get_user_playlists(user_id, limit):
    """Get user's own playlists"""
    try:
        user = User.objects.get(id=user_id)
        playlists = Playlist.objects.filter(owner=user)[:limit]
        
        playlists_data = []
        for playlist in playlists:
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'track_count': len(playlist.tracks),
                'is_public': playlist.is_public,
                'followers_count': playlist.followers.count(),
                'created_at': playlist.date.isoformat(),
            })
        
        return playlists_data
    except User.DoesNotExist:
        return []
    except Exception as e:
        return []