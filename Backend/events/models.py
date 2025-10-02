from django.db import models
from django.utils import timezone
from users.models import User

# Predefined location choices
from django.db import models
from django.utils import timezone
from users.models import User

# Predefined location choices
LOCATION_CHOICES = [
    ('E1', 'E1'),
    ('E2', 'E2'),
    ('P1', 'P1'),
    ('P2', 'P2'),
    ('C3', 'C3'),
    ('C4', 'C4'),
    ('Agora', 'Agora'),
    ('E3', 'E3'),
    ('C3-Room', 'C3-Room'),
    ('C3-Relax', 'C3-Relax'),
    ('C4-rooms', 'C4-rooms'),
    ('Elevator-room', 'Elevator-room'),
]

# Create your models here.
class Events(models.Model):
    title = models.CharField(max_length=255)
    organizer = models.ForeignKey(User, on_delete=models.CASCADE, related_name='organized_events')
    attendees = models.ManyToManyField(User, related_name='attended_events', blank=True)
    description = models.TextField(blank=True, null=True)    
    location = models.CharField(max_length=50, choices=LOCATION_CHOICES)
    image_url = models.URLField(blank=True, null=True)
    songs = models.JSONField(default=list)  # Stores array of song IDs
    managers = models.JSONField(default=list)  # Stores array of manager IDs
    track_votes = models.JSONField(default=dict)  # Stores track votes: {"track_id": ["user_id1", "user_id2"]}
    # New field for user roles mapping
    user_roles = models.JSONField(default=dict)  # {"user_id": "role"} - roles: "owner", "editor", "listener"
    # New field for pending invites
    pending_invites = models.JSONField(default=list)  # Stores array of user IDs with pending invites
    is_public = models.BooleanField(default=True)
    event_start_time = models.DateTimeField(default=timezone.now)
    event_end_time = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'events'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['organizer']),
            models.Index(fields=['is_public']),
            models.Index(fields=['event_start_time']),
        ]

    def __str__(self):
        return self.title
    
    def add_attendee(self, user):
        """Add user to event attendees with appropriate role"""
        if user not in self.attendees.all():
            self.attendees.add(user)
            # Assign listener role by default when joining
            self.assign_role(user.id, 'listener')
            # Remove from pending invites if they were invited
            self.remove_pending_invite(user.id)
            return True
        return False
    
    def remove_attendee(self, user):
        """Remove user from event attendees and their role"""
        if user in self.attendees.all():
            self.attendees.remove(user)
            # Remove from managers if they were a manager
            user_id = str(user.id)
            if user_id in self.managers:
                self.managers.remove(user_id)
                self.save()
            # Remove user role when leaving
            self.remove_user_role(user.id)
            return True
        return False
    
    def add_pending_invite(self, user_id):
        """Add user to pending invites list (legacy method for backward compatibility)"""
        return self.add_pending_invite_with_role(user_id, 'attendee')
    
    def add_pending_invite_with_role(self, user_id, role):
        """Add user to pending invites list with role information"""
        user_id = str(user_id)
        
        # Check if user already has pending invite
        for invite in self.pending_invites:
            if isinstance(invite, dict) and invite.get('user_id') == user_id:
                return False
            elif isinstance(invite, str) and invite == user_id:
                return False
        
        # Add new invite with role
        invite_data = {
            'user_id': user_id,
            'role': role,
            'invited_at': timezone.now().isoformat()
        }
        self.pending_invites.append(invite_data)
        self.save()
        return True
    
    def remove_pending_invite(self, user_id):
        """Remove user from pending invites list"""
        user_id = str(user_id)
        
        # Handle both old format (string) and new format (dict)
        for i, invite in enumerate(self.pending_invites):
            if isinstance(invite, dict) and invite.get('user_id') == user_id:
                self.pending_invites.pop(i)
                self.save()
                return True
            elif isinstance(invite, str) and invite == user_id:
                self.pending_invites.pop(i)
                self.save()
                return True
        return False
    
    def has_pending_invite(self, user_id):
        """Check if user has pending invite"""
        user_id = str(user_id)
        
        for invite in self.pending_invites:
            if isinstance(invite, dict) and invite.get('user_id') == user_id:
                return True
            elif isinstance(invite, str) and invite == user_id:
                return True
        return False
    
    def get_pending_invite_role(self, user_id):
        """Get the role for a pending invite"""
        user_id = str(user_id)
        
        for invite in self.pending_invites:
            if isinstance(invite, dict) and invite.get('user_id') == user_id:
                return invite.get('role', 'attendee')
            elif isinstance(invite, str) and invite == user_id:
                return 'attendee'  # Default for old format
        return None
    
    def invite_user(self, user_id, inviter_id, role='attendee'):
        """Invite a user to the event with a specific role"""
        user_id = str(user_id)
        inviter_id = str(inviter_id)
        
        # Validate role - using the actual event structure
        valid_roles = ['organizer', 'manager', 'attendee']
        if role not in valid_roles:
            return False, "Invalid role. Must be 'organizer', 'manager', or 'attendee'"
        
        # Check if user is already attending
        try:
            user = User.objects.get(id=int(user_id))
            if user in self.attendees.all():
                return False, "User is already attending this event"
        except User.DoesNotExist:
            return False, "User not found"
        
        # Check if user already has pending invite
        if self.has_pending_invite(user_id):
            return False, "User already has a pending invite"
        
        # Check if inviter has permission to invite with this role
        if not self.can_invite_with_role(inviter_id, role):
            return False, "You don't have permission to invite with this role"
        
        # For organizer role, check if current user is organizer
        if role == 'organizer' and str(self.organizer.id) != inviter_id:
            return False, "Only the current organizer can invite new organizers"
        
        # Add to pending invites with role information
        self.add_pending_invite_with_role(user_id, role)
        
        # Add notification to user
        try:
            inviter = User.objects.get(id=int(inviter_id))
            inviter_name = inviter.name
            
            # Get current event notifications
            current_notifications = user.event_notifications or {}
            
            # Add new notification
            current_notifications[str(self.id)] = {
                'event_id': self.id,
                'event_title': self.title,
                'inviter_id': inviter_id,
                'inviter_name': inviter_name,
                'invited_role': role,
                'type': 'event_invite',
                'created_at': timezone.now().isoformat()
            }
            
            user.event_notifications = current_notifications
            user.save()
            
            return True, f"Invitation sent successfully for {role} role"
        except User.DoesNotExist:
            return False, "Inviter not found"
    
    def accept_invite(self, user_id):
        """Accept event invitation"""
        user_id = str(user_id)
        
        if not self.has_pending_invite(user_id):
            return False, "No pending invite found"
        
        # Get the invited role
        invited_role = self.get_pending_invite_role(user_id)
        
        try:
            user = User.objects.get(id=int(user_id))
            
            # Handle different role assignments
            if invited_role == 'organizer':
                # Transfer ownership - current organizer becomes manager
                current_organizer = self.organizer
                self.organizer = user
                
                # Add current organizer as manager if they're not already
                if str(current_organizer.id) not in self.managers:
                    self.managers.append(str(current_organizer.id))
                
                # Add new organizer as attendee and assign owner role
                self.attendees.add(user)
                self.assign_role(user.id, 'owner')
                
                # Make previous organizer an editor in user_roles
                self.assign_role(current_organizer.id, 'editor')
                
            elif invited_role == 'manager':
                # Add as manager and attendee
                if str(user_id) not in self.managers:
                    self.managers.append(str(user_id))
                self.attendees.add(user)
                # Assign editor role in user_roles for managers
                self.assign_role(user.id, 'editor')
                
            else:  # attendee
                # Add as regular attendee
                self.attendees.add(user)
                self.assign_role(user.id, 'listener')
            
            # Remove from pending invites
            self.remove_pending_invite(user_id)
            
            # Remove notification
            current_notifications = user.event_notifications or {}
            if str(self.id) in current_notifications:
                del current_notifications[str(self.id)]
                user.event_notifications = current_notifications
                user.save()
            
            self.save()
            return True, f"Invitation accepted successfully. You are now a {invited_role}"
        except User.DoesNotExist:
            return False, "User not found"
    
    def decline_invite(self, user_id):
        """Decline event invitation"""
        user_id = str(user_id)
        
        if not self.has_pending_invite(user_id):
            return False, "No pending invite found"
        
        self.remove_pending_invite(user_id)
        
        # Remove notification
        try:
            user = User.objects.get(id=int(user_id))
            current_notifications = user.event_notifications or {}
            if str(self.id) in current_notifications:
                del current_notifications[str(self.id)]
                user.event_notifications = current_notifications
                user.save()
        except User.DoesNotExist:
            pass
        
        return True, "Invitation declined"
    
    def assign_role(self, user_id, role):
        """Assign role to user. Roles: 'owner', 'editor', 'listener'"""
        valid_roles = ['owner', 'editor', 'listener']
        if role not in valid_roles:
            return False
        
        user_id = str(user_id)
        self.user_roles[user_id] = role
        self.save()
        return True
    
    def assign_editor_role(self, user_id):
        """Assign 'editor' role to user"""
        return self.assign_role(user_id, 'editor')
    
    def transfer_ownership(self, new_owner_id, current_owner_id):
        """Transfer ownership to another user, current owner becomes editor"""
        new_owner_id = str(new_owner_id)
        current_owner_id = str(current_owner_id)
        
        # Check if current user is actually the owner
        if self.get_user_role(current_owner_id) != 'owner':
            return False
        
        # Assign new owner
        self.assign_role(new_owner_id, 'owner')
        # Make previous owner an editor
        self.assign_role(current_owner_id, 'editor')
        
        # Update the organizer field to reflect the new owner
        try:
            new_owner = User.objects.get(id=int(new_owner_id))
            self.organizer = new_owner
            self.save()
            return True
        except User.DoesNotExist:
            return False
    
    def can_invite_with_role(self, inviter_id, role):
        """Check if inviter can invite someone with the specified role"""
        inviter_id = str(inviter_id)
        
        # Check if inviter is the organizer
        is_organizer = str(self.organizer.id) == inviter_id
        
        # Check if inviter is a manager
        is_manager = inviter_id in self.managers
        
        # Permission matrix based on actual event roles
        if role == 'organizer':
            # Only current organizer can invite new organizers
            return is_organizer
        elif role == 'manager':
            # Only organizers can invite managers
            return is_organizer
        elif role == 'attendee':
            # Organizers and managers can invite attendees
            return is_organizer or is_manager
        
        return False
    
    def get_users_by_role(self, role):
        """Get list of user IDs with specific role"""
        return [user_id for user_id, user_role in self.user_roles.items() if user_role == role]
    
    def get_all_roles(self):
        """Get all user roles in the event"""
        return self.user_roles

    def get_user_role(self, user_id):
        """Get user's role in the event"""
        user_id = str(user_id)
        return self.user_roles.get(user_id, None)
    
    def remove_user_role(self, user_id):
        """Remove user's role from the event"""
        user_id = str(user_id)
        if user_id in self.user_roles:
            del self.user_roles[user_id]
            self.save()
            return True
        return False
    
    def has_permission(self, user_id, action):
        """Check if user has permission for specific action"""
        user_id = str(user_id)
        
        # Check database relationships
        is_organizer = str(self.organizer.id) == user_id
        is_manager = user_id in self.managers
        is_attendee = self.attendees.filter(id=int(user_id)).exists()
        
        # Check user_roles field
        user_role = self.get_user_role(user_id)
        
        # Permission matrix combining both systems
        permissions = {
            'owner': ['edit_event', 'delete_event', 'add_tracks', 'remove_tracks', 'manage_users', 'invite_users', 'invite_organizers', 'invite_managers', 'invite_attendees'],
            'editor': ['edit_event', 'add_tracks', 'remove_tracks', 'invite_attendees'],
            'listener': ['vote_tracks']
        }
        
        # Check permissions based on database role first
        if is_organizer:
            return action in permissions.get('owner', [])
        elif is_manager:
            return action in permissions.get('editor', [])
        elif is_attendee and user_role:
            # For attendees, check their specific role in user_roles
            return action in permissions.get(user_role, [])
        elif is_attendee:
            # Default attendee permissions
            return action in permissions.get('listener', [])
        
        return False
    
    def get_user_actual_role(self, user_id):
        """Get user's actual role based on database fields and user_roles field"""
        user_id = str(user_id)
        
        # Check if user is organizer
        if str(self.organizer.id) == user_id:
            # Return user_roles value if available, otherwise 'organizer'
            user_role = self.get_user_role(user_id)
            return user_role if user_role else 'organizer'
        
        # Check if user is manager
        if user_id in self.managers:
            # Return user_roles value if available, otherwise 'manager'
            user_role = self.get_user_role(user_id)
            return user_role if user_role else 'manager'
        
        # Check if user is attendee
        if self.attendees.filter(id=int(user_id)).exists():
            # Return user_roles value if available, otherwise 'attendee'
            user_role = self.get_user_role(user_id)
            return user_role if user_role else 'attendee'
        
        return None
    
    def is_organizer(self, user_id):
        """Check if user is the organizer"""
        return str(self.organizer.id) == str(user_id)
    
    def is_manager(self, user_id):
        """Check if user is a manager"""
        return str(user_id) in self.managers
    
    def is_attendee(self, user_id):
        """Check if user is an attendee"""
        return self.attendees.filter(id=int(user_id)).exists()
    
    def can_edit(self, user_id):
        """Check if user can edit the event"""
        return self.has_permission(user_id, 'edit_event')
    
    def add_track(self, track_id):
        """Add track to event"""
        if str(track_id) not in self.songs:
            self.songs.append(str(track_id))
            self.save()
            return True
        return False
    
    def remove_track(self, track_id):
        """Remove track from event"""
        if str(track_id) in self.songs:
            self.songs.remove(str(track_id))
            # Also remove votes for this track
            if str(track_id) in self.track_votes:
                del self.track_votes[str(track_id)]
            self.save()
            return True
        return False
    
    def vote_track(self, user_id, track_id):
        """Vote for a track"""
        track_id = str(track_id)
        user_id = str(user_id)
        
        if track_id not in self.songs:
            return False
        
        if track_id not in self.track_votes:
            self.track_votes[track_id] = []
        
        if user_id not in self.track_votes[track_id]:
            self.track_votes[track_id].append(user_id)
            self.save()
            return True
        return False
    
    def unvote_track(self, user_id, track_id):
        """Remove vote for a track"""
        track_id = str(track_id)
        user_id = str(user_id)
        
        if track_id in self.track_votes and user_id in self.track_votes[track_id]:
            self.track_votes[track_id].remove(user_id)
            self.save()
            return True
        return False
    
    def get_track_votes(self, track_id):
        """Get vote count for a track"""
        track_id = str(track_id)
        return len(self.track_votes.get(track_id, []))
    
    def toggle_visibility(self):
        """Toggle event visibility"""
        self.is_public = not self.is_public
        self.save()
    
    @property
    def attendee_count(self):
        return self.attendees.count()
    
    @property
    def track_count(self):
        return len(self.songs)
    
    def get_pending_invites_with_roles(self):
        """Get all pending invites with their roles"""
        invites = []
        for invite in self.pending_invites:
            if isinstance(invite, dict):
                invites.append(invite)
            else:
                # Handle old format
                invites.append({
                    'user_id': invite,
                    'role': 'attendee',
                    'invited_at': None
                })
        return invites
    
    def get_user_invitation_info(self, user_id):
        """Get invitation info for a specific user"""
        user_id = str(user_id)
        for invite in self.pending_invites:
            if isinstance(invite, dict) and invite.get('user_id') == user_id:
                return invite
            elif isinstance(invite, str) and invite == user_id:
                return {
                    'user_id': user_id,
                    'role': 'attendee',
                    'invited_at': None
                }
        return None
    
    @classmethod
    def get_available_locations(cls):
        """Get list of all available locations"""
        return [choice[0] for choice in LOCATION_CHOICES]
    
    @classmethod
    def get_my_events(cls, user_id):
        """Get all events where the user is involved (organizer, attendee, or has any role)"""
        from django.db.models import Q
        
        user_id = str(user_id)
        
        # Use Q objects to combine all conditions into a single query
        q_filter = Q(organizer_id=user_id) | Q(attendees__id=user_id)
        
        # Add condition for managers (stored in JSON field)
        # Check if user_id is in the managers JSON array
        q_filter |= Q(managers__contains=user_id)
        
        # Get all events matching any of the conditions
        all_events = cls.objects.filter(q_filter).distinct()
        
        return all_events.order_by('-created_at')
    
    @classmethod
    def get_location_choices(cls):
        """Get location choices for forms/API"""
        return LOCATION_CHOICES