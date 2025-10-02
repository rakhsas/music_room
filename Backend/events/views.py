from django.shortcuts import render, get_object_or_404
from django.http import JsonResponse
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from events.services import get_event_songs
from .models import Events
from users.models import User


def check_event_permission(request, events, action='view'):
    """Check event permissions"""
    if not request.user.is_authenticated:
        return False
    
    if action == 'view':
        return (events.is_public or
                events.organizer == request.user or
                request.user in events.attendees.all())

    elif action == 'edit':
        return (events.organizer == request.user or
                str(request.user.id) in events.managers)

    elif action == 'delete':
        return events.organizer == request.user

    return False

@swagger_auto_schema(
    method='get',
    operation_summary="Get public events",
    operation_description="Get list of public events with optional location filtering",
    manual_parameters=[
        openapi.Parameter(
            'location',
            openapi.IN_QUERY,
            description="Filter events by location",
            type=openapi.TYPE_STRING,
            required=False
        ),
    ]
)
@api_view(['GET'])
def event_list(request):
    """Get list of public events with optional location filtering"""
    try:
        location_filter = request.GET.get('location')
        events = Events.objects.filter(is_public=True)
        
        # Filter by location if provided
        if location_filter:
            # Validate location is in the predefined choices
            valid_locations = [choice[0] for choice in Events.get_location_choices()]
            if location_filter not in valid_locations:
                return Response({
                    'error': f'Invalid location filter. Must be one of: {", ".join(valid_locations)}'
                }, status=status.HTTP_400_BAD_REQUEST)
            events = events.filter(location=location_filter)
        
        events = events[:20]
        events_data = []
        
        for event in events:
            events_data.append({
                'id': event.id,
                'title': event.title,
                'organizer': event.organizer.name,
                'location': event.location,
                'attendee_count': event.attendee_count,
                'event_start_time': event.event_start_time.isoformat(),
                'is_public': event.is_public,
            })
        
        return Response(events_data)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='POST',
    operation_summary="Create new event",
    operation_description="Create a new event with required location selection from predefined list",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['title', 'location', 'event_start_time'],
        properties={
            'title': openapi.Schema(type=openapi.TYPE_STRING, description="Event title"),
            'description': openapi.Schema(type=openapi.TYPE_STRING, description="Event description (optional)"),
            'location': openapi.Schema(
                type=openapi.TYPE_STRING, 
                description="Event location (required). Must be one of: E1, E2, P1, P2, C3, C4, Agora, E3, C3-Room, C3-Relax, C4-rooms, Elevator-room",
                enum=['E1', 'E2', 'P1', 'P2', 'C3', 'C4', 'Agora', 'E3', 'C3-Room', 'C3-Relax', 'C4-rooms', 'Elevator-room']
            ),
            'event_start_time': openapi.Schema(type=openapi.TYPE_STRING, format='datetime', description="Event start time (required)"),
            'event_end_time': openapi.Schema(type=openapi.TYPE_STRING, format='datetime', description="Event end time (optional)"),
            'is_public': openapi.Schema(type=openapi.TYPE_BOOLEAN, description="Whether event is public (default: true)"),
        }
    ),
    responses={
        201: openapi.Response(
            description="Event created successfully",
            examples={
                'application/json': {
                    'id': 1,
                    'title': 'Music Night',
                    'message': 'Event created successfully'
                }
            }
        ),
        400: openapi.Response(
            description="Bad request - missing required fields or invalid location",
            examples={
                'application/json': {
                    'error': 'Location is required'
                }
            }
        )
    }
)
@api_view(['POST'])
def create_event(request):
    """Create a new event"""
    try:
        title = request.data.get('title')
        description = request.data.get('description', '')
        location = request.data.get('location')
        event_start_time = request.data.get('event_start_time')
        event_end_time = request.data.get('event_end_time')
        is_public = request.data.get('is_public', True)
        
        if not title or not event_start_time:
            return Response({'error': 'Title and start time are required'}, status=status.HTTP_400_BAD_REQUEST)
        
        if not location:
            return Response({'error': 'Location is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Validate location is in the predefined choices
        valid_locations = [choice[0] for choice in Events.get_location_choices()]
        if location not in valid_locations:
            return Response({
                'error': f'Invalid location. Must be one of: {", ".join(valid_locations)}'
            }, status=status.HTTP_400_BAD_REQUEST)
        event = Events.objects.create(
            title=title,
            description=description,
            location=location,
            event_start_time=event_start_time,
            event_end_time=event_end_time,
            organizer=request.user,
            is_public=is_public
        )
        
        # Assign creator as owner in user_roles
        event.assign_role(request.user.id, 'owner')
        
        return Response({
            'id': event.id,
            'title': event.title,
            'message': 'Event created successfully'
        }, status=status.HTTP_201_CREATED)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='get',
    operation_summary="Get event details",
    operation_description="Get detailed information about a specific event including user roles and track vote counts",
    manual_parameters=[
        openapi.Parameter(
            'event_id',
            openapi.IN_PATH,
            description="Event ID",
            type=openapi.TYPE_INTEGER,
            required=True
        )
    ],
    responses={
        200: openapi.Response(
            description="Event details retrieved successfully",
            examples={
                'application/json': {
                    'id': 1,
                    'title': 'Summer Music Festival',
                    'description': 'A great music event',
                    'location': 'Central Park',
                    'event_start_time': '2024-07-15T20:00:00Z',
                    'event_end_time': '2024-07-15T23:00:00Z',
                    'organizer': {
                        'id': 1,
                        'name': 'John Doe',
                        'avatar': 'https://example.com/avatar.jpg'
                    },
                    'attendee_count': 25,
                    'track_count': 10,
                    'is_public': True,
                    'songs': [
                        {'track_id': 'song1', 'vote_count': 5},
                        {'track_id': 'song2', 'vote_count': 3}
                    ],
                    'user_roles': {
                        '1': 'owner',
                        '2': 'editor',
                        '3': 'listener'
                    },
                    'current_user_role': 'owner'
                }
            }
        ),
        403: openapi.Response(
            description="Access denied",
            examples={
                'application/json': {
                    'error': 'Access denied'
                }
            }
        ),
        404: openapi.Response(
            description="Event not found",
            examples={
                'application/json': {
                    'error': 'Event not found'
                }
            }
        )
    }
)
@api_view(['GET'])
def event_detail(request, event_id):
    """Get specific event details"""
    try:
        event = get_object_or_404(Events, id=event_id)

        if not check_event_permission(request, event, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get current user's role if authenticated
        current_user_role = None
        if request.user.is_authenticated:
            current_user_role = event.get_user_role(request.user.id)
        
        # Build songs list with vote counts
        songs_with_votes = []
        for track_id in event.songs:
            # Get vote count for this track using the model method
            vote_count = event.get_track_votes(track_id)
            songs_with_votes.append({
                'track_id': track_id,
                'vote_count': vote_count
            })
        
        event_data = {
            'id': event.id,
            'title': event.title,
            'description': event.description,
            'location': event.location,
            'event_start_time': event.event_start_time.isoformat(),
            'event_end_time': event.event_end_time.isoformat() if event.event_end_time else None,
            'organizer': {
                'id': event.organizer.id,
                'name': event.organizer.name,
                'avatar': event.organizer.avatar
            },
            'attendee_count': event.attendee_count,
            'track_count': event.track_count,
            'is_public': event.is_public,
            'songs': songs_with_votes,
            'user_roles': event.get_all_roles(),
            'current_user_role': current_user_role,
        }
        
        return Response(event_data)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@permission_classes([IsAuthenticated])
@swagger_auto_schema(operation_summary="Join event")
def join_event(request, event_id):
    """Join an event"""
    try:
        event = get_object_or_404(Events, id=event_id)

        if not event.is_public and event.organizer != request.user:
            return Response({'error': 'Cannot join private event'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.add_attendee(request.user):
            return Response({'message': 'Successfully joined event'})
        else:
            return Response({'message': 'Already attending this event'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
@swagger_auto_schema(operation_summary="Leave event")
def leave_event(request, event_id):
    """Leave an event"""
    try:
        event = get_object_or_404(Events, id=event_id)

        if event.organizer == request.user:
            return Response({'error': 'Organizer cannot leave their own event'}, status=status.HTTP_400_BAD_REQUEST)
        
        if event.remove_attendee(request.user):
            return Response({'message': 'Successfully left event'})
        else:
            return Response({'message': 'Not attending this event'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Add user to event")
def add_user_to_event(request, event_id, user_id):
    """Add a user to an event (invite)"""
    try:
        event = get_object_or_404(Events, id=event_id)
        user_to_add = get_object_or_404(User, id=user_id)
        
        if not check_event_permission(request, event, 'edit'):
            return Response({'error': 'Only organizer or managers can add users'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.add_attendee(user_to_add):
            return Response({'message': f'User {user_to_add.name} added successfully'})
        else:
            return Response({'message': 'User already attending this event'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Remove user from event")
def remove_user_from_event(request, event_id, user_id):
    """Remove a user from an event"""
    try:
        event = get_object_or_404(Events, id=event_id)
        user_to_remove = get_object_or_404(User, id=user_id)
        
        if not check_event_permission(request, event, 'edit'):
            return Response({'error': 'Only organizer or managers can remove users'}, status=status.HTTP_403_FORBIDDEN)
        
        if user_to_remove == event.organizer:
            return Response({'error': 'Cannot remove organizer from event'}, status=status.HTTP_400_BAD_REQUEST)
        
        if event.remove_attendee(user_to_remove):
            return Response({'message': f'User {user_to_remove.name} removed successfully'})
        else:
            return Response({'message': 'User not attending this event'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Add track to event")
def add_track_to_event(request, event_id, track_id):
    """Add a track to event"""
    try:
        event = get_object_or_404(Events, id=event_id)

        # Check if user is attendee or organizer
        if not (event.organizer == request.user or request.user in event.attendees.all()):
            return Response({'error': 'Only attendees can add tracks'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.add_track(track_id):
            return Response({'message': 'Track added successfully'})
        else:
            return Response({'message': 'Track already in event'})
            
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Remove track from event")
def remove_track_from_event(request, event_id, track_id):
    """Remove a track from event"""
    try:
        event = get_object_or_404(Events, id=event_id)

        # Only organizer can remove tracks
        if not check_event_permission(request, event, 'edit'):
            return Response({'error': 'Only organizer or managers can remove tracks'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.remove_track(track_id):
            return Response({'message': 'Track removed successfully'})
        else:
            return Response({'message': 'Track not in event'})
            
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get event tracks")
def get_event_tracks(request, event_id):
    """Get tracks in an event with vote counts and full details from Jamendo API"""
    try:
        event = get_object_or_404(Events, id=event_id)

        if not check_event_permission(request, event, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get full track details from Jamendo API
        tracks_data = get_event_songs(event_id)
        
        # Add vote counts to each track
        tracks_with_votes = []
        track_results = tracks_data.get('results', []) if tracks_data else []
        
        for track in track_results:
            track_id = str(track.get('id', ''))
            vote_count = event.get_track_votes(track_id)
            
            # Add vote count to track data
            track['votes'] = vote_count
            tracks_with_votes.append(track)
        
        # Sort tracks by votes in descending order (most voted first)
        tracks_with_votes.sort(key=lambda track: track.get('votes', 0), reverse=True)
        
        # Return event info along with track details
        response_data = {
            'event_info': {
                'id': event.id,
                'title': event.title,
                'organizer': event.organizer.name,
                'track_count': len(event.songs),
                'attendee_count': event.attendee_count,
                'is_public': event.is_public,
            },
            'tracks': tracks_with_votes
        }
        
        return Response(response_data)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['POST'])
@swagger_auto_schema(operation_summary="Vote for track in event")
def vote_track_in_event(request, event_id, track_id):
    """Vote for a track in event"""
    try:
        event = get_object_or_404(Events, id=event_id)

        # Only attendees can vote
        if not (event.organizer == request.user or request.user in event.attendees.all()):
            return Response({'error': 'Only attendees can vote'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.vote_track(request.user.id, track_id):
            return Response({'message': 'Vote added successfully'})
        else:
            return Response({'message': 'Already voted for this track or track not in event'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['DELETE'])
@swagger_auto_schema(operation_summary="Remove vote for track in event")
def unvote_track_in_event(request, event_id, track_id):
    """Remove vote for a track in event"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        # Only attendees can unvote
        if not (event.organizer == request.user or request.user in event.attendees.all()):
            return Response({'error': 'Only attendees can unvote'}, status=status.HTTP_403_FORBIDDEN)
        
        if event.unvote_track(request.user.id, track_id):
            return Response({'message': 'Vote removed successfully'})
        else:
            return Response({'message': 'Vote not found'})
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='put',
    operation_summary="Change event visibility",
    request_body=openapi.Schema(
        type= openapi.TYPE_OBJECT,
        required=['is_public'],
        properties={
            'is_public': openapi.Schema(
                type=openapi.TYPE_BOOLEAN,
                description="Set to true for public event, false for private"
            )
        }
    ),
    responses={
        201: openapi.Response(
            description="Event visibility changed successfully",
        ),
        403: openapi.Response(
            description="Permission denied - only organizer or managers can change visibility",
        ),
        400: openapi.Response(
            description="Bad request - is_public field is required",
        )
    }
)
@api_view(['PUT'])
def change_event_visibility(request, event_id):
    """Change event visibility (public/private)"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        if not check_event_permission(request, event, 'edit'):
            return Response({'error': 'Only organizer or managers can change visibility'}, status=status.HTTP_403_FORBIDDEN)
        
        is_public = request.data.get('is_public')
        if is_public is not None:
            event.is_public = is_public
            event.save()
            visibility = "public" if is_public else "private"
            return Response({'message': f'Event visibility changed to {visibility}'})
        else:
            return Response({'error': 'is_public field is required'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get event attendees")
def event_attendees(request, event_id):
    """Get list of event attendees"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        if not check_event_permission(request, event, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        attendees = []
        for attendee in event.attendees.all():
            attendees.append({
                'id': attendee.id,
                'name': attendee.name,
                'avatar': attendee.avatar,
            })
        
        return Response(attendees)
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Assign editor role to user",
    operation_description="Assign 'editor' role to a user in the event. Only owners can assign editor roles.",
    manual_parameters=[
        openapi.Parameter(
            'event_id',
            openapi.IN_PATH,
            description="Event ID",
            type=openapi.TYPE_INTEGER,
            required=True
        ),
        openapi.Parameter(
            'user_id',
            openapi.IN_PATH,
            description="User ID to assign editor role",
            type=openapi.TYPE_INTEGER,
            required=True
        )
    ],
    responses={
        200: openapi.Response(
            description="Editor role assigned successfully",
            examples={
                'application/json': {
                    'message': 'User assigned as editor successfully'
                }
            }
        ),
        403: openapi.Response(
            description="Permission denied - only owners can assign editor roles",
            examples={
                'application/json': {
                    'error': 'Only event owner can assign editor roles'
                }
            }
        ),
        404: openapi.Response(
            description="Event or user not found",
            examples={
                'application/json': {
                    'error': 'Event not found'
                }
            }
        )
    }
)
@api_view(['POST'])
def assign_editor_role(request, event_id, user_id):
    """Assign editor role to a user"""
    try:
        event = get_object_or_404(Events, id=event_id)
        user_to_assign = get_object_or_404(User, id=user_id)
        
        # Check if current user is the owner
        if event.get_user_role(request.user.id) != 'owner':
            return Response({'error': 'Only event owner can assign editor roles'}, status=status.HTTP_403_FORBIDDEN)
        
        # Assign editor role
        if event.assign_editor_role(user_id):
            return Response({'message': f'User {user_to_assign.name} assigned as editor successfully'})
        else:
            return Response({'error': 'Failed to assign editor role'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Transfer event ownership",
    operation_description="Transfer ownership of the event to another user. Current owner becomes an editor.",
    manual_parameters=[
        openapi.Parameter(
            'event_id',
            openapi.IN_PATH,
            description="Event ID",
            type=openapi.TYPE_INTEGER,
            required=True
        ),
        openapi.Parameter(
            'user_id',
            openapi.IN_PATH,
            description="User ID to transfer ownership to",
            type=openapi.TYPE_INTEGER,
            required=True
        )
    ],
    responses={
        200: openapi.Response(
            description="Ownership transferred successfully",
            examples={
                'application/json': {
                    'message': 'Ownership transferred successfully'
                }
            }
        ),
        403: openapi.Response(
            description="Permission denied - only current owner can transfer ownership",
            examples={
                'application/json': {
                    'error': 'Only current owner can transfer ownership'
                }
            }
        ),
        404: openapi.Response(
            description="Event or user not found",
            examples={
                'application/json': {
                    'error': 'User not found'
                }
            }
        )
    }
)
@api_view(['POST'])
def transfer_event_ownership(request, event_id, user_id):
    """Transfer ownership to another user"""
    try:
        event = get_object_or_404(Events, id=event_id)
        new_owner = get_object_or_404(User, id=user_id)
        
        # Check if current user is the owner
        if event.get_user_role(request.user.id) != 'owner':
            return Response({'error': 'Only current owner can transfer ownership'}, status=status.HTTP_403_FORBIDDEN)
        
        # Transfer ownership
        if event.transfer_ownership(user_id, request.user.id):
            return Response({'message': f'Ownership transferred to {new_owner.name} successfully'})
        else:
            return Response({'error': 'Failed to transfer ownership'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='get',
    operation_summary="Get event user roles",
    operation_description="Get all users and their roles in the event",
    manual_parameters=[
        openapi.Parameter(
            'event_id',
            openapi.IN_PATH,
            description="Event ID",
            type=openapi.TYPE_INTEGER,
            required=True
        )
    ],
    responses={
        200: openapi.Response(
            description="User roles retrieved successfully",
            examples={
                'application/json': {
                    'user_roles': {
                        '1': 'owner',
                        '2': 'editor',
                        '3': 'listener'
                    },
                    'users_by_role': {
                        'owner': [{'id': 1, 'name': 'John Doe', 'role': 'owner'}],
                        'editor': [{'id': 2, 'name': 'Jane Smith', 'role': 'editor'}],
                        'listener': [{'id': 3, 'name': 'Bob Wilson', 'role': 'listener'}]
                    }
                }
            }
        ),
        403: openapi.Response(
            description="Access denied",
            examples={
                'application/json': {
                    'error': 'Access denied'
                }
            }
        ),
        404: openapi.Response(
            description="Event not found",
            examples={
                'application/json': {
                    'error': 'Event not found'
                }
            }
        )
    }
)
@api_view(['GET'])
def get_event_user_roles(request, event_id):
    """Get all user roles in the event"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        if not check_event_permission(request, event, 'view'):
            return Response({'error': 'Access denied'}, status=status.HTTP_403_FORBIDDEN)
        
        # Get all user roles
        user_roles = event.get_all_roles()
        
        # Organize users by role with user details
        users_by_role = {'owner': [], 'editor': [], 'listener': []}
        
        for user_id, role in user_roles.items():
            try:
                user = User.objects.get(id=int(user_id))
                user_data = {
                    'id': user.id,
                    'name': user.name,
                    'avatar': user.avatar,
                    'role': role
                }
                users_by_role[role].append(user_data)
            except User.DoesNotExist:
                continue
        
        return Response({
            'user_roles': user_roles,
            'users_by_role': users_by_role
        })
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='delete',
    operation_summary="Remove user role from event",
    operation_description="Remove a user's role from the event (effectively removing them from the event)",
    manual_parameters=[
        openapi.Parameter(
            'event_id',
            openapi.IN_PATH,
            description="Event ID",
            type=openapi.TYPE_INTEGER,
            required=True
        ),
        openapi.Parameter(
            'user_id',
            openapi.IN_PATH,
            description="User ID to remove from event",
            type=openapi.TYPE_INTEGER,
            required=True
        )
    ],
    responses={
        200: openapi.Response(
            description="User removed successfully",
            examples={
                'application/json': {
                    'message': 'User removed from event successfully'
                }
            }
        ),
        403: openapi.Response(
            description="Permission denied",
            examples={
                'application/json': {
                    'error': 'Only owner or the user themselves can remove from event'
                }
            }
        ),
        404: openapi.Response(
            description="Event or user not found",
            examples={
                'application/json': {
                    'error': 'Event not found'
                }
            }
        )
    }
)
@api_view(['DELETE'])
def remove_user_role_from_event(request, event_id, user_id):
    """Remove user role from event"""
    try:
        event = get_object_or_404(Events, id=event_id)
        user_to_remove = get_object_or_404(User, id=user_id)
        
        # Check permissions: only owner can remove others, or user can remove themselves
        current_user_role = event.get_user_role(request.user.id)
        if current_user_role != 'owner' and request.user.id != int(user_id):
            return Response({'error': 'Only owner or the user themselves can remove from event'}, status=status.HTTP_403_FORBIDDEN)
        
        # Cannot remove the owner unless they're removing themselves
        if event.get_user_role(user_id) == 'owner' and request.user.id != int(user_id):
            return Response({'error': 'Cannot remove the event owner'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Remove user from attendees and role
        if event.remove_attendee(user_to_remove):
            return Response({'message': f'User {user_to_remove.name} removed from event successfully'})
        else:
            return Response({'error': 'User is not part of this event'}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='get',
    operation_summary="Get available event locations",
    operation_description="Get list of all predefined locations available for event creation",
    responses={
        200: openapi.Response(
            description="List of available locations",
            examples={
                'application/json': {
                    'locations': [
                        {'value': 'E1', 'label': 'E1'},
                        {'value': 'E2', 'label': 'E2'},
                        {'value': 'P1', 'label': 'P1'},
                        {'value': 'P2', 'label': 'P2'},
                        {'value': 'C3', 'label': 'C3'},
                        {'value': 'C4', 'label': 'C4'},
                        {'value': 'Agora', 'label': 'Agora'},
                        {'value': 'E3', 'label': 'E3'},
                        {'value': 'C3-Room', 'label': 'C3-Room'},
                        {'value': 'C3-Relax', 'label': 'C3-Relax'},
                        {'value': 'C4-rooms', 'label': 'C4-rooms'},
                        {'value': 'Elevator-room', 'label': 'Elevator-room'}
                    ]
                }
            }
        )
    }
)
@api_view(['GET'])
def get_available_locations(request):
    """Get list of all available locations for events"""
    try:
        location_choices = Events.get_location_choices()
        location_data = [{'value': loc[0], 'label': loc[1]} for loc in location_choices]
        
        return Response({
            'locations': location_data
        }, status=status.HTTP_200_OK)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

# Invitation Management Views

@swagger_auto_schema(
    method='post',
    operation_summary="Invite user to event with role",
    operation_description="Send invitation to a user to join the event with a specific role (organizer, manager, or attendee)",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        required=['user_id'],
        properties={
            'user_id': openapi.Schema(type=openapi.TYPE_INTEGER, description="ID of user to invite"),
            'role': openapi.Schema(
                type=openapi.TYPE_STRING, 
                description="Role to assign to the invited user",
                enum=['organizer', 'manager', 'attendee'],
                default='attendee'
            ),
        }
    ),
    responses={
        200: openapi.Response(
            description="Invitation sent successfully",
            examples={
                'application/json': {
                    'message': 'Invitation sent successfully for attendee role'
                }
            }
        ),
        400: openapi.Response(
            description="Bad request - user already invited or attending, or invalid role",
            examples={
                'application/json': {
                    'error': 'User is already attending this event'
                }
            }
        ),
        403: openapi.Response(
            description="Permission denied - insufficient permissions for the requested role",
            examples={
                'application/json': {
                    'error': 'You don\'t have permission to invite users as organizer'
                }
            }
        )
    }
)
@api_view(['POST'])
def invite_user_to_event(request, event_id):
    """Invite a user to an event with a specific role"""
    try:
        event = get_object_or_404(Events, id=event_id)
        user_id = request.data.get('user_id')
        role = request.data.get('role', 'attendee')  # Default to attendee
        
        if not user_id:
            return Response({'error': 'user_id is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        # Validate role
        valid_roles = ['organizer', 'manager', 'attendee']
        if role not in valid_roles:
            return Response({'error': f'Invalid role. Must be one of: {", ".join(valid_roles)}'}, 
                          status=status.HTTP_400_BAD_REQUEST)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        # Check if user can invite with this role
        if not event.can_invite_with_role(current_user_id, role):
            return Response({'error': f'You don\'t have permission to invite users as {role}'}, 
                          status=status.HTTP_403_FORBIDDEN)
        
        # Invite user with role
        success, message = event.invite_user(user_id, current_user_id, role)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Accept event invitation",
    responses={
        200: openapi.Response(
            description="Invitation accepted successfully",
            examples={
                'application/json': {
                    'message': 'Invitation accepted successfully'
                }
            }
        ),
        400: openapi.Response(
            description="No pending invite found",
            examples={
                'application/json': {
                    'error': 'No pending invite found'
                }
            }
        )
    }
)
@api_view(['POST'])
def accept_event_invitation(request, event_id):
    """Accept event invitation"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        success, message = event.accept_invite(current_user_id)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='post',
    operation_summary="Decline event invitation",
    responses={
        200: openapi.Response(
            description="Invitation declined successfully",
            examples={
                'application/json': {
                    'message': 'Invitation declined'
                }
            }
        ),
        400: openapi.Response(
            description="No pending invite found",
            examples={
                'application/json': {
                    'error': 'No pending invite found'
                }
            }
        )
    }
)
@api_view(['POST'])
def decline_event_invitation(request, event_id):
    """Decline event invitation"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        success, message = event.decline_invite(current_user_id)
        
        if success:
            return Response({'message': message}, status=status.HTTP_200_OK)
        else:
            return Response({'error': message}, status=status.HTTP_400_BAD_REQUEST)
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='get',
    operation_summary="Get user's events",
    operation_description="Get all events user is involved in (organizing, attending, managing, has roles)",
    responses={
        200: openapi.Response(
            description="Events retrieved successfully",
            examples={
                'application/json': {
                    'events': [
                        {
                            'id': 1,
                            'title': 'Music Night',
                            'organizer': {'id': 1, 'name': 'John Doe'},
                            'user_role': ['owner', 'organizer'],
                            'can_edit': True,
                            'attendees_count': 15,
                            'location': 'E1',
                            'event_start_time': '2025-07-05T19:00:00Z'
                        }
                    ],
                    'count': 1
                }
            }
        )
    }
)
@api_view(['GET'])
def get_my_events(request):
    """Get all events user is involved in"""
    try:
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        # Get all events user is involved in
        events = Events.get_my_events(request.user)
        
        events_data = []
        for event in events:
            # Determine user's relationship to this event
            user_roles = []
            if event.organizer == request.user:
                user_roles.append('organizer')
            if request.user in event.attendees.all():
                user_roles.append('attendee')
            if str(current_user_id) in event.managers:
                user_roles.append('manager')
            
            # Check role from user_roles field
            user_role_from_field = event.get_user_role(current_user_id)
            if user_role_from_field:
                user_roles.append(user_role_from_field)
            
            # Remove duplicates
            user_roles = list(set(user_roles))
            
            events_data.append({
                'id': event.id,
                'title': event.title,
                'organizer': {
                    'id': event.organizer.id,
                    'name': event.organizer.name,
                    'avatar': event.organizer.avatar if hasattr(event.organizer, 'avatar') else None
                },
                'description': event.description,
                'location': event.location,
                'image_url': event.image_url,
                'is_public': event.is_public,
                'attendees_count': event.attendee_count,
                'track_count': event.track_count,
                'user_roles': user_roles,  # Shows all relationships: ['organizer', 'attendee', 'owner', etc.]
                'can_edit': event.has_permission(current_user_id, 'edit_event') or event.organizer == request.user,
                'can_invite': event.has_permission(current_user_id, 'invite_users') or event.organizer == request.user,
                'event_start_time': event.event_start_time.isoformat(),
                'event_end_time': event.event_end_time.isoformat() if event.event_end_time else None,
                'created_at': event.created_at.isoformat(),
            })
        
        return Response({
            'events': events_data,
            'count': len(events_data)
        })
        
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

@swagger_auto_schema(
    method='get',
    operation_summary="Get my events",
    operation_description="Get all events where the authenticated user is involved (organizer, attendee, manager, or has any role)",
    manual_parameters=[
        openapi.Parameter(
            'user_id',
            openapi.IN_QUERY,
            description="User ID to get events for (optional - defaults to authenticated user)",
            type=openapi.TYPE_INTEGER,
            required=False
        ),
    ],
    responses={
        200: openapi.Response(
            description="Events retrieved successfully",
            examples={
                'application/json': {
                    'events': [
                        {
                            'id': 1,
                            'title': 'Music Night',
                            'description': 'A great music event',
                            'location': 'E1',
                            'start_time': '2025-07-05T19:00:00Z',
                            'end_time': '2025-07-05T22:00:00Z',
                            'is_public': True,
                            'organizer_id': 1,
                            'attendees_count': 15,
                            'user_role': 'organizer',
                            'can_edit': True
                        }
                    ],
                    'count': 1
                }
            }
        ),
        400: openapi.Response(
            description="Bad request",
            examples={
                'application/json': {
                    'error': 'Invalid user_id'
                }
            }
        )
    }
)
@api_view(['GET'])
def my_events(request):
    """Get all events where the user is involved (organizer, attendee, or has any role)"""
    # Get user_id from query parameter or use authenticated user
    user_id = request.GET.get('user_id')
    if not user_id:
        user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
    
    try:
        events = Events.get_my_events(user_id)
        
        events_data = []
        for event in events:
            event_data = {
                'id': event.id,
                'title': event.title,
                'description': event.description,
                'location': event.location,
                'start_time': event.event_start_time.isoformat() if event.event_start_time else None,
                'end_time': event.event_end_time.isoformat() if event.event_end_time else None,
                'is_public': event.is_public,
                'organizer': event.organizer.name,
                'created_at': event.created_at.isoformat(),
                'updated_at': event.updated_at.isoformat(),
                'attendees_count': event.attendees.count(),
                'user_role': event.get_user_actual_role(user_id),
                'can_edit': event.can_edit(user_id)
            }
            events_data.append(event_data)
        
        return Response({
            'events': events_data,
            'count': len(events_data)
        })
    
    except Exception as e:
        return Response({'error': str(e)}, status=500)

@api_view(['GET'])
def get_event_pending_invites(request, event_id):
    """Get pending invites for an event"""
    try:
        event = get_object_or_404(Events, id=event_id)
        
        # Get current user ID
        current_user_id = request.user.id if hasattr(request.user, 'id') else request.user.user_id
        
        # Check if user can view pending invites (organizer or manager)
        if not event.has_permission(current_user_id, 'manage_users'):
            return Response({'error': 'Only organizers and managers can view pending invites'}, 
                          status=status.HTTP_403_FORBIDDEN)
        
        # Get pending invites with roles
        pending_invites = event.get_pending_invites_with_roles()
        
        # Enrich with user information
        invites_data = []
        for invite in pending_invites:
            try:
                user = User.objects.get(id=int(invite['user_id']))
                invite_data = {
                    'user_id': invite['user_id'],
                    'name': user.name,
                    'email': user.email,
                    'role': invite['role'],
                    'invited_at': invite['invited_at']
                }
                invites_data.append(invite_data)
            except User.DoesNotExist:
                continue
        
        return Response({
            'pending_invites': invites_data,
            'count': len(invites_data)
        })
    
    except Exception as e:
        return Response({'error': str(e)}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)