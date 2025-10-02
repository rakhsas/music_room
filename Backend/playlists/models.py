from django.db import models
from django.utils import timezone
from users.models import User

class Playlist(models.Model):
    name = models.CharField(max_length=255)
    owner = models.ForeignKey(User, on_delete=models.CASCADE, related_name='playlists')
    tracks = models.JSONField(default=list)  # Stores array of track IDs
    collaborators = models.ManyToManyField(User, related_name='collaborative_playlists', blank=True)
    is_public = models.BooleanField(default=True)
    followers = models.ManyToManyField(User, related_name='followed_playlists', blank=True)
    could_edit = models.JSONField(default=list) #if the playlist is public, this will be empty
    pending_invites = models.JSONField(default=list)  # Stores array of user IDs with pending invites
    date = models.DateField(default=timezone.now)

    class Meta:
        db_table = 'playlists'
        ordering = ['-date']
        indexes = [
            models.Index(fields=['owner']),
            models.Index(fields=['is_public']),
        ]

    def __str__(self):
        return f"{self.name} by {self.owner.name}"
    
    def add_track(self, track_id):
        if track_id not in self.tracks:
            self.tracks.append(track_id)
            self.save()
    
    def remove_track(self, track_id):
        if track_id in self.tracks:
            self.tracks.remove(track_id)
            self.save()
            
    def add_collaborator(self, user):
        if user not in self.collaborators.all():
            self.collaborators.add(user)
            self.save()
            
    def add_follower(self, user):
        """Add user as follower and collaborator (gives edit rights)"""
        if user not in self.followers.all():
            self.followers.add(user)
            # Also add as collaborator to give edit rights
            if user not in self.collaborators.all():
                self.collaborators.add(user)
            self.save()
            
    def remove_follower(self, user):
        """Remove user from followers and collaborators"""
        if user in self.followers.all():
            self.followers.remove(user)
            # Also remove from collaborators
            if user in self.collaborators.all():
                self.collaborators.remove(user)
            self.save()
    
    def add_pending_invite(self, user_id):
        """Add user to pending invites list"""
        user_id = str(user_id)
        if user_id not in self.pending_invites:
            self.pending_invites.append(user_id)
            self.save()
            return True
        return False
    
    def remove_pending_invite(self, user_id):
        """Remove user from pending invites list"""
        user_id = str(user_id)
        if user_id in self.pending_invites:
            self.pending_invites.remove(user_id)
            self.save()
            return True
        return False
    
    def has_pending_invite(self, user_id):
        """Check if user has pending invite"""
        user_id = str(user_id)
        return user_id in self.pending_invites
    
    def invite_user(self, user_id, inviter_id):
        """Invite a user to the playlist (only for private playlists)"""
        from django.utils import timezone
        
        # Only allow invites for private playlists
        if self.is_public:
            return False, "Cannot invite users to public playlists. Public playlists can be followed directly."
        
        user_id = str(user_id)
        
        # Check if user is already a collaborator or follower
        try:
            user = User.objects.get(id=int(user_id))
            if user in self.collaborators.all() or user in self.followers.all():
                return False, "User already has access to this playlist"
        except User.DoesNotExist:
            return False, "User not found"
        
        # Check if user already has pending invite
        if self.has_pending_invite(user_id):
            return False, "User already has a pending invite"
        
        # Add to pending invites
        self.add_pending_invite(user_id)
        
        # Add notification to user's playlist notifications
        self.add_playlist_notification(user_id, inviter_id)
        
        return True, "Invitation sent successfully"
    
    def add_playlist_notification(self, user_id, inviter_id):
        """Add playlist notification to user"""
        try:
            user = User.objects.get(id=int(user_id))
            inviter = User.objects.get(id=int(inviter_id))
            
            # Get or create playlist_notifications field
            if not hasattr(user, 'playlist_notifications'):
                user.playlist_notifications = {}
            
            # Add notification
            notification_key = f"{self.id}_{inviter_id}"
            user.playlist_notifications[notification_key] = {
                'playlist_id': self.id,
                'playlist_name': self.name,
                'inviter_id': inviter_id,
                'inviter_name': inviter.name,
                'invited_at': timezone.now().isoformat(),
                'message': f"{inviter.name} has invited you to {self.name}",
                'note': 'You will get edit permissions when you accept'
            }
            user.save()
            
        except User.DoesNotExist:
            pass
    
    def accept_invite(self, user_id):
        """Accept playlist invitation - user becomes both collaborator and follower"""
        user_id = str(user_id)
        
        if not self.has_pending_invite(user_id):
            return False, "No pending invite found"
        
        try:
            user = User.objects.get(id=int(user_id))
            # Add user as both collaborator and follower
            if user not in self.collaborators.all():
                self.collaborators.add(user)
            if user not in self.followers.all():
                self.followers.add(user)
            
            # Remove from pending invites
            self.remove_pending_invite(user_id)
            # Remove playlist notification
            self.remove_playlist_notification(user_id)
            return True, "Invitation accepted successfully"
        except User.DoesNotExist:
            return False, "User not found"
    
    def decline_invite(self, user_id):
        """Decline playlist invitation"""
        user_id = str(user_id)
        
        if not self.has_pending_invite(user_id):
            return False, "No pending invite found"
        
        self.remove_pending_invite(user_id)
        # Remove playlist notification
        self.remove_playlist_notification(user_id)
        return True, "Invitation declined"
    
    def remove_playlist_notification(self, user_id):
        """Remove playlist notification from user"""
        try:
            user = User.objects.get(id=int(user_id))
            if hasattr(user, 'playlist_notifications'):
                # Remove notifications for this playlist
                keys_to_remove = [k for k in user.playlist_notifications.keys() if k.startswith(f"{self.id}_")]
                for key in keys_to_remove:
                    del user.playlist_notifications[key]
                user.save()
        except User.DoesNotExist:
            pass
    
    def set_status(self, is_public):
        """Set playlist status (public/private)"""
        self.is_public = is_public
        self.save()
    
    def can_edit(self, user):
        """Check if user has edit permissions"""
        # Owner can always edit
        if self.owner == user:
            return True
        
        # Collaborators can edit
        if user in self.collaborators.all():
            return True
        
        # Check if user ID is in could_edit list (for additional permissions)
        if str(user.id) in self.could_edit:
            return True
        
        return False
    
    def add_edit_permission(self, user_id):
        """Add user to could_edit list"""
        user_id = str(user_id)
        if user_id not in self.could_edit:
            self.could_edit.append(user_id)
            self.save()
            return True
        return False
    
    def remove_edit_permission(self, user_id):
        """Remove user from could_edit list"""
        user_id = str(user_id)
        if user_id in self.could_edit:
            self.could_edit.remove(user_id)
            self.save()
            return True
        return False