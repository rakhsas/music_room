from django.shortcuts import render, get_object_or_404
from django.http import JsonResponse
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi

from .models import Playlist
from users.models import User
from music.services import get_playlist_songs

def check_playlist_permission(request, playlist, action='view'):
    """Check playlist permissions"""
    if not request.user.is_authenticated:
        return False
    
    if action == 'view':
        return (playlist.is_public or 
                playlist.owner == request.user or 
                request.user in playlist.collaborators.all() or
                request.user in playlist.followers.all())
    
    elif action == 'edit':
        # Use the new can_edit method that checks all edit permissions
        return playlist.can_edit(request.user)
    
    elif action == 'delete':
        return playlist.owner == request.user
    
    return False

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get public playlists")
def playlist_list(request):
    """Get list of public playlists"""
    try:
        playlists = Playlist.objects.filter(is_public=True)[:20]
        playlists_data = []
        
        for playlist in playlists:
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'owner': playlist.owner.name,
                'track_count': len(playlist.tracks),
                'followers_count': playlist.followers.count(),
                'created_at': playlist.date.isoformat(),
            })
        
        return Response(playlists_data)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get user's accessible playlists")
def user_playlists(request):
    """Get all playlists user has access to (owned, following, collaborating)"""
    try:
        # Get playlists where user is owner, follower, or collaborator
        owned_playlists = Playlist.objects.filter(owner=request.user)
        followed_playlists = Playlist.objects.filter(followers=request.user)
        collaborated_playlists = Playlist.objects.filter(collaborators=request.user)
        
        # Combine all playlists and remove duplicates
        all_playlists = (owned_playlists | followed_playlists | collaborated_playlists).distinct()
        
        playlists_data = []
        
        for playlist in all_playlists:
            # Determine user's relationship to this playlist
            user_role = []
            if playlist.owner == request.user:
                user_role.append('owner')
            if request.user in playlist.collaborators.all():
                user_role.append('collaborator')
            if request.user in playlist.followers.all():
                user_role.append('follower')
            
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'owner': {
                    'id': playlist.owner.id,
                    'name': playlist.owner.name,
                    'avatar': playlist.owner.avatar
                },
                'track_count': len(playlist.tracks),
                'followers_count': playlist.followers.count(),
                'is_public': playlist.is_public,
                'user_role': user_role,  # Shows relationship: ['owner'], ['follower'], ['collaborator'], etc.
                'can_edit': playlist.can_edit(request.user),
                'created_at': playlist.date.isoformat(),
            })
        
        # Sort by creation date (newest first)
        playlists_data.sort(key=lambda x: x['created_at'], reverse=True)
        
        return Response({
            'playlists': playlists_data,
            'count': len(playlists_data)
        })
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get playlist details")
def playlist_detail(request, playlist_id):
    """Get specific playlist details"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'view'):
            print(1)
            #return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get collaborators info
        collaborators = []
        for collaborator in playlist.collaborators.all():
            collaborators.append({
                'id': collaborator.id,
                'name': collaborator.name,
                'avatar': collaborator.avatar
            })
        
        playlist_data = {
            'id': playlist.id,
            'name': playlist.name,
            'owner': {
                'id': playlist.owner.id,
                'name': playlist.owner.name,
                'avatar': playlist.owner.avatar
            },
            'tracks': playlist.tracks,
            'track_count': len(playlist.tracks),
            'is_public': playlist.is_public,
            'followers_count': playlist.followers.count(),
            'collaborators': collaborators,
            'could_edit': playlist.could_edit,
            'user_permissions': {
                'can_edit': playlist.can_edit(request.user) if request.user.is_authenticated else False,
                'can_delete': playlist.owner == request.user if request.user.is_authenticated else False,
                'is_following': request.user in playlist.followers.all() if request.user.is_authenticated else False,
                'is_collaborator': request.user in playlist.collaborators.all() if request.user.is_authenticated else False,
                'is_owner': playlist.owner == request.user if request.user.is_authenticated else False
            },
            'created_at': playlist.date.isoformat(),
        }
        
        return Response(playlist_data)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Create new playlist",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'name': openapi.Schema(type=openapi.TYPE_STRING),
            'is_public': openapi.Schema(type=openapi.TYPE_BOOLEAN),
        }
    )
)
@api_view(['POST'])
def create_playlist(request):
    """Create a new playlist"""
    try:
        name = request.data.get('name')
        is_public = request.data.get('is_public', True)
        
        if not name:
            return Response({'error': 'Playlist name is required'}, status=status.HTTP_400_BAD_REQUEST)
        playlist = Playlist.objects.create(
            name=name,
            owner=request.user,
            is_public=is_public
        )
        
        return Response({
            'id': playlist.id,
            'name': playlist.name,
            'message': 'Playlist created successfully'
        }, status=status.HTTP_201_CREATED)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='put',
    operation_summary="Update playlist",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'name': openapi.Schema(type=openapi.TYPE_STRING),
            'is_public': openapi.Schema(type=openapi.TYPE_BOOLEAN),
        }
    )
)
@api_view(['PUT'])
def update_playlist(request, playlist_id):
    """Update playlist details"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'edit'):
            return Response({'error': 'Permission denied'}, status=status.HTTP_403_FORBIDDEN)
        
        name = request.data.get('name')
        is_public = request.data.get('is_public')
        
        if name:
            playlist.name = name
        if is_public is not None:
            playlist.is_public = is_public
        
        playlist.save()
        
        return Response({'message': 'Playlist updated successfully'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Delete playlist")
def delete_playlist(request, playlist_id):
    """Delete a playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'delete'):
            return Response({'error': 'Only playlist owner can delete'}, status=status.HTTP_403_FORBIDDEN)
        
        playlist.delete()
        return Response({'message': 'Playlist deleted successfully'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Follow playlist")
def follow_playlist(request, playlist_id):
    """Follow a playlist (automatically grants edit rights)"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not playlist.is_public and playlist.owner != request.user:
            return Response({'error': 'Cannot follow private playlist'}, status=status.HTTP_403_FORBIDDEN)
        
        if playlist.owner == request.user:
            return Response({'error': 'Cannot follow your own playlist'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Check if already following
        if request.user in playlist.followers.all():
            return Response({'message': 'Already following this playlist'}, status=status.HTTP_200_OK)
        
        # Add as follower (this will also add as collaborator automatically)
        playlist.add_follower(request.user)
        return Response({
            'message': 'Playlist followed successfully',
            'note': 'You now have edit permissions for this playlist'
        })
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Unfollow playlist")
def unfollow_playlist(request, playlist_id):
    """Unfollow a playlist (removes edit rights)"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if request.user not in playlist.followers.all():
            return Response({'message': 'Not following this playlist'}, status=status.HTTP_200_OK)
        
        # Remove as follower (this will also remove collaborator status)
        playlist.remove_follower(request.user)
        return Response({
            'message': 'Playlist unfollowed successfully',
            'note': 'Edit permissions have been removed'
        })
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
@swagger_auto_schema(operation_summary="Add collaborator to playlist")
def add_collaborator(request, playlist_id, user_id):
    """Add a collaborator to playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        user_to_add = get_object_or_404(User, id=user_id)
        
        if playlist.owner != request.user:
            return Response({'error': 'Only playlist owner can add collaborators'}, status=status.HTTP_403_FORBIDDEN)
        
        if user_to_add == playlist.owner:
            return Response({'error': 'Owner is already a collaborator'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Check if user is already a collaborator
        if user_to_add in playlist.collaborators.all():
            return Response({'error': f'User {user_to_add.name} is already a collaborator'}, status=status.HTTP_400_BAD_REQUEST)
        
        playlist.add_collaborator(user_to_add)
        return Response({'message': f'User {user_to_add.name} added as collaborator'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Remove collaborator from playlist")
def remove_collaborator(request, playlist_id, user_id):
    """Remove a collaborator from playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        user_to_remove = get_object_or_404(User, id=user_id)
        
        if playlist.owner != request.user:
            return Response({'error': 'Only playlist owner can remove collaborators'}, status=status.HTTP_403_FORBIDDEN)
        
        if user_to_remove in playlist.collaborators.all():
            playlist.collaborators.remove(user_to_remove)
            return Response({'message': f'User {user_to_remove.name} removed as collaborator'})
        else:
            return Response({'message': 'User is not a collaborator'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Add track to playlist")
def add_track_to_playlist(request, playlist_id, track_id):
    """Add a track to playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'edit'):
            return Response({'error': 'Permission denied'}, status=status.HTTP_403_FORBIDDEN)
        
        if str(track_id) not in playlist.tracks:
            playlist.add_track(str(track_id))
            return Response({'message': 'Track added successfully'})
        else:
            return Response({'message': 'Track already in playlist'})
            
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Remove track from playlist")
def remove_track_from_playlist(request, playlist_id, track_id):
    """Remove a track from playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'edit'):
            return Response({'error': 'Permission denied'}, status=status.HTTP_403_FORBIDDEN)
        
        if str(track_id) in playlist.tracks:
            playlist.remove_track(str(track_id))
            return Response({'message': 'Track removed successfully'})
        else:
            return Response({'message': 'Track not in playlist'})
            
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get playlist tracks with details")
def get_playlist_tracks(request, playlist_id):
    """Get tracks in a playlist with full details from Jamendo"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get track details from Jamendo API
        tracks_data = get_playlist_songs(playlist_id)
        
        # Return playlist info along with track details
        response_data = {
            'playlist_info': {
                'id': playlist.id,
                'name': playlist.name,
                'owner': playlist.owner.name,
                'track_count': len(playlist.tracks),
                'is_public': playlist.is_public,
                'followers_count': playlist.followers.count(),
            },
            'tracks': tracks_data.get('results', []) if tracks_data else []
        }
        return Response(response_data)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['PUT'])
@swagger_auto_schema(
    operation_summary="Change playlist visibility",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'is_public': openapi.Schema(type=openapi.TYPE_BOOLEAN),
        }
    )
)
def change_playlist_visibility(request, playlist_id):
    """Change playlist visibility (public/private)"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if playlist.owner != request.user:
            return Response({'error': 'Only playlist owner can change visibility'}, status=status.HTTP_403_FORBIDDEN)
        
        is_public = request.data.get('is_public')
        if is_public is not None:
            playlist.set_status(is_public)
            visibility = "public" if is_public else "private"
            return Response({'message': f'Playlist visibility changed to {visibility}'})
        else:
            return Response({'error': 'is_public field is required'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get playlist followers")
def playlist_followers(request, playlist_id):
    """Get list of playlist followers"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        followers = []
        for follower in playlist.followers.all():
            followers.append({
                'id': follower.id,
                'name': follower.name,
                'avatar': follower.avatar,
            })
        
        return Response(followers)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get followed playlists")
def followed_playlists(request):
    """Get playlists that user follows"""
    try:
        followed = Playlist.objects.filter(followers=request.user)
        playlists_data = []
        
        for playlist in followed:
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'owner': playlist.owner.name,
                'track_count': len(playlist.tracks),
                'followers_count': playlist.followers.count(),
                'created_at': playlist.date.isoformat(),
            })
        
        return Response(playlists_data)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)


@swagger_auto_schema(
    method='post',
    operation_summary="Invite user to playlist",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['user_id'],
        properties={
            'user_id': openapi.Schema(type=openapi.TYPE_INTEGER, description="ID of user to invite"),
        }
    ),
    responses={
        200: openapi.Response(description="Invitation sent successfully"),
        400: openapi.Response(description="Bad request"),
        403: openapi.Response(description="Permission denied"),
        404: openapi.Response(description="Playlist not found")
    }
)
@api_view(['POST'])
def invite_user_to_playlist(request, playlist_id):
    """Invite a user to a private playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        user_id = request.data.get('user_id')
        
        if not user_id:
            return Response({'error': 'user_id is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Check permissions - only owner and collaborators can invite
        if not check_playlist_permission(request, playlist, 'edit'):
            return Response({'error': 'Only owner or collaborators can invite users'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        # Invite user
        success, message = playlist.invite_user(user_id, current_user_id)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Accept playlist invitation",
    responses={
        200: openapi.Response(description="Invitation accepted successfully"),
        400: openapi.Response(description="No pending invite found"),
        404: openapi.Response(description="Playlist not found")
    }
)
@api_view(['POST'])
def accept_playlist_invitation(request, playlist_id):
    """Accept playlist invitation"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        success, message = playlist.accept_invite(current_user_id)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Decline playlist invitation",
    responses={
        200: openapi.Response(description="Invitation declined successfully"),
        400: openapi.Response(description="No pending invite found"),
        404: openapi.Response(description="Playlist not found")
    }
)
@api_view(['POST'])
def decline_playlist_invitation(request, playlist_id):
    """Decline playlist invitation"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        success, message = playlist.decline_invite(current_user_id)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Reorder tracks in playlist")
def reorder_tracks_in_playlist(request, playlist_id):
    """Reorder tracks in playlist"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        if not check_playlist_permission(request, playlist, 'edit'):
            return Response({'error': 'Permission denied'}, status=status.HTTP_403_FORBIDDEN)
        
        new_order = request.data.get('track_order', [])
        
        # Validate that all tracks in new order exist in playlist
        if set(new_order) == set(playlist.tracks):
            playlist.tracks = new_order
            playlist.save()
            return Response({'message': 'Playlist tracks reordered successfully'})
        else:
            return Response({'error': 'Invalid track order'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Grant edit permission to user",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['user_id'],
        properties={
            'user_id': openapi.Schema(type=openapi.TYPE_INTEGER, description="ID of user to grant edit permission"),
        }
    ),
    responses={
        200: openapi.Response(description="Edit permission granted successfully"),
        400: openapi.Response(description="Bad request"),
        403: openapi.Response(description="Permission denied"),
        404: openapi.Response(description="Playlist not found")
    }
)
@api_view(['POST'])
def grant_edit_permission(request, playlist_id):
    """Grant edit permission to a user"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        user_id = request.data.get('user_id')
        
        if not user_id:
            return Response({'error': 'user_id is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Only owner can grant edit permissions
        if playlist.owner != request.user:
            return Response({'error': 'Only playlist owner can grant edit permissions'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get user to grant permission to
        try:
            user_to_grant = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found'}, status=status.HTTP_404_NOT_FOUND)
        
        # Add edit permission
        success = playlist.add_edit_permission(user_id)
        
        if success:
            return Response({'message': f'Edit permission granted to {user_to_grant.name}'})
        else:
            return Response({'message': 'User already has edit permission'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='delete',
    operation_summary="Revoke edit permission from user",
    responses={
        200: openapi.Response(description="Edit permission revoked successfully"),
        403: openapi.Response(description="Permission denied"),
        404: openapi.Response(description="User not found")
    }
)
@api_view(['DELETE'])
def revoke_edit_permission(request, playlist_id, user_id):
    """Revoke edit permission from a user"""
    try:
        playlist = get_object_or_404(Playlist, id=playlist_id)
        
        # Only owner can revoke edit permissions
        if playlist.owner != request.user:
            return Response({'error': 'Only playlist owner can revoke edit permissions'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get user to revoke permission from
        try:
            user_to_revoke = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found'}, status=status.HTTP_404_NOT_FOUND)
        
        # Remove edit permission
        success = playlist.remove_edit_permission(user_id)
        
        if success:
            return Response({'message': f'Edit permission revoked from {user_to_revoke.name}'})
        else:
            return Response({'message': 'User does not have edit permission'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get user's owned playlists only")
def owned_playlists(request):
    """Get playlists that user owns"""
    try:
        playlists = Playlist.objects.filter(owner=request.user)
        playlists_data = []
        
        for playlist in playlists:
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'track_count': len(playlist.tracks),
                'followers_count': playlist.followers.count(),
                'collaborators_count': playlist.collaborators.count(),
                'is_public': playlist.is_public,
                'created_at': playlist.date.isoformat(),
            })
        
        return Response({
            'playlists': playlists_data,
            'count': len(playlists_data)
        })
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get collaborative playlists")
def collaborative_playlists(request):
    """Get playlists where user is a collaborator (but not owner)"""
    try:
        playlists = Playlist.objects.filter(collaborators=request.user).exclude(owner=request.user)
        playlists_data = []
        
        for playlist in playlists:
            playlists_data.append({
                'id': playlist.id,
                'name': playlist.name,
                'owner': {
                    'id': playlist.owner.id,
                    'name': playlist.owner.name,
                    'avatar': playlist.owner.avatar
                },
                'track_count': len(playlist.tracks),
                'followers_count': playlist.followers.count(),
                'is_public': playlist.is_public,
                'can_edit': playlist.can_edit(request.user),
                'created_at': playlist.date.isoformat(),
            })
        
        return Response({
            'playlists': playlists_data,
            'count': len(playlists_data)
        })
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)