from django.urls import path
from . import views

urlpatterns = [
    # Event CRUD
    path('', views.event_list, name='event_list'),
    path('create/', views.create_event, name='create_event'),
    path('<int:event_id>/', views.event_detail, name='event_detail'),
    
    # Event locations
    path('locations/', views.get_available_locations, name='get_available_locations'),
    
    # Event membership
    path('<int:event_id>/join/', views.join_event, name='join_event'),
    path('<int:event_id>/leave/', views.leave_event, name='leave_event'),
    path('<int:event_id>/add-user/<int:user_id>/', views.add_user_to_event, name='add_user_to_event'),
    path('<int:event_id>/remove-user/<int:user_id>/', views.remove_user_from_event, name='remove_user_from_event'),
    path('<int:event_id>/attendees/', views.event_attendees, name='event_attendees'),
    
    # Event tracks
    path('<int:event_id>/tracks/', views.get_event_tracks, name='get_event_tracks'),
    path('<int:event_id>/tracks/<int:track_id>/add/', views.add_track_to_event, name='add_track_to_event'),
    path('<int:event_id>/tracks/<int:track_id>/remove/', views.remove_track_from_event, name='remove_track_from_event'),
    
    # Track voting
    path('<int:event_id>/tracks/<int:track_id>/vote/', views.vote_track_in_event, name='vote_track_in_event'),
    path('<int:event_id>/tracks/<int:track_id>/unvote/', views.unvote_track_in_event, name='unvote_track_in_event'),
    
    # Event settings
    path('<int:event_id>/visibility/', views.change_event_visibility, name='change_event_visibility'),
    
    # Role Management
    path('<int:event_id>/roles/', views.get_event_user_roles, name='get_event_user_roles'),
    path('<int:event_id>/assign-editor/<int:user_id>/', views.assign_editor_role, name='assign_editor_role'),
    path('<int:event_id>/transfer-ownership/<int:user_id>/', views.transfer_event_ownership, name='transfer_event_ownership'),
    path('<int:event_id>/remove-user-role/<int:user_id>/', views.remove_user_role_from_event, name='remove_user_role_from_event'),
    
    # Invitation Management
    path('<int:event_id>/invite/', views.invite_user_to_event, name='invite_user_to_event'),
    path('<int:event_id>/pending-invites/', views.get_event_pending_invites, name='get_event_pending_invites'),
    path('<int:event_id>/accept-invite/', views.accept_event_invitation, name='accept_event_invitation'),
    path('<int:event_id>/decline-invite/', views.decline_event_invitation, name='decline_event_invitation'),
    
    # User's events
    path('my-events/', views.my_events, name='my_events'),
]