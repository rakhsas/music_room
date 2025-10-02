from django.db import models
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
import uuid

class UserManager(BaseUserManager):
    def create_user(self, email, name, password=None, **extra_fields):
        if not email:
            raise ValueError("Email is required")
        email = self.normalize_email(email)
        user = self.model(email=email, name=name, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, name, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(email, name, password, **extra_fields)
    
        
class User(AbstractBaseUser, PermissionsMixin):
    # Use AutoField for compatibility with existing data
    id = models.AutoField(primary_key=True)
    
    # Required fields for account creation (keeping your existing structure)
    name = models.CharField(max_length=255)
    email = models.EmailField(max_length=255, unique=True)
    
    # Optional fields - keeping your existing fields
    avatar = models.CharField(max_length=255, default='default_avatar.png')
    liked_artists = models.JSONField(default=list)
    liked_albums = models.JSONField(default=list)
    liked_songs = models.JSONField(default=list)
    genres = models.JSONField(default=list)
    last_song_played = models.CharField(max_length=255, null=True, blank=True)
    events = models.JSONField(default=list)
    friends = models.JSONField(default=list)
    
    # Profile fields (merging from accounts)
    date_of_birth = models.DateField(null=True, blank=True)  # Keeping your field name
    bio = models.TextField(blank=True)
    phone_number = models.CharField(max_length=20, blank=True)
    
    # Privacy settings from accounts
    profile_privacy = models.CharField(max_length=10, default='public')
    email_privacy = models.CharField(max_length=10, default='friends')
    phone_privacy = models.CharField(max_length=10, default='private')
    
    # Social media integration from accounts
    facebook_id = models.CharField(max_length=100, blank=True, null=True)
    google_id = models.CharField(max_length=100, blank=True, null=True)
    
    # Subscription fields (keeping your existing structure but adding accounts features)
    is_subscribed = models.BooleanField(default=False)  # Your existing field
    is_premium = models.BooleanField(default=False)     # From accounts
    subscription_type = models.CharField(
        max_length=20, 
        choices=[('free', 'Free'), ('premium', 'Premium')], 
        default='free'
    )
    
    # Music preferences from accounts
    music_preferences = models.JSONField(default=dict)
    
    # Notification fields for invitations
    event_notifications = models.JSONField(default=dict)  # {"event_id_inviter_id": {"event_id": 1, "inviter_name": "John", "message": "..."}}
    playlist_notifications = models.JSONField(default=dict)  # {"playlist_id_inviter_id": {"playlist_id": 1, "inviter_name": "John", "message": "..."}}
    
    # OTP fields for password reset
    password_reset_otp = models.CharField(max_length=6, blank=True, null=True)
    password_reset_otp_created_at = models.DateTimeField(blank=True, null=True)
    password_reset_otp_verified = models.BooleanField(default=False)
    
    # Auto-generated fields (keeping your structure)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    last_login = models.DateTimeField(null=True, blank=True)
    
    # Authentication fields (from accounts)
    is_verified = models.BooleanField(default=False)    # Your existing field
    is_active = models.BooleanField(default=True)       # Your existing field
    is_staff = models.BooleanField(default=False)       # Required for admin
    
    # OTP fields for password reset
    password_reset_otp = models.CharField(max_length=6, blank=True, null=True)
    password_reset_otp_created_at = models.DateTimeField(blank=True, null=True)
    password_reset_otp_verified = models.BooleanField(default=False)
    
    # Set up the user manager
    objects = UserManager()
    
    # Required for Django authentication
    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name']
    
    class Meta:
        db_table = 'users'
        indexes = [
            models.Index(fields=['email']),
            models.Index(fields=['created_at']),
        ]

    def __str__(self):
        return self.name