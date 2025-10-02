from .models import User
from django.contrib.auth import authenticate
from rest_framework_simplejwt.tokens import RefreshToken
import jwt
from datetime import datetime, timedelta
from django.conf import settings
from django.core.mail import send_mail
import requests
from rest_framework.exceptions import ValidationError
from django.core.exceptions import ValidationError
from django.db import IntegrityError
from django.contrib.auth.hashers import make_password, check_password



def get_user_by_name(name):
    """Get a user by name"""
    try:
        user = User.objects.get(name=name, is_active=True)
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'created_at': user.created_at,
            'updated_at': user.updated_at
        }
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}
    
    
def get_all_users():
    """Get all active users"""
    try:
        users = User.objects.filter(is_active=True).values(
            'id', 'name', 'email', 'avatar', 'created_at', 'updated_at'
        )
        return list(users)
    except Exception as e:
        return {'error': str(e)}

def get_user_by_id(user_id):
    """Get a specific user by ID"""
    try:
        user = User.objects.get(id=user_id, is_active=True)
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'created_at': user.created_at,
            'updated_at': user.updated_at
        }
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}

def get_user_by_email(email):
    """Get a user by email"""
    try:
        user = User.objects.get(email=email, is_active=True)
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'created_at': user.created_at,
            'updated_at': user.updated_at
        }
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}

def create_user(name, email, avatar=None, password=None):
    """Create a new user"""
    try:
        # Check if user already exists
        if User.objects.filter(email=email).exists():
            return {'error': 'User with this email already exists'}
        
        user_data = {
            'email': email,
            'name': name,
            'avatar': avatar,
        }
        
        if password:
            user_data['password'] = make_password(password)
        
        user = User.objects.create(**user_data)
        
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'created_at': user.created_at,
            'message': 'User created successfully'
        }
    except IntegrityError:
        return {'error': 'User with this email already exists'}
    except Exception as e:
        return {'error': str(e)}

def update_user(user_id, **kwargs):
    """Update user information"""
    try:
        user = User.objects.get(id=user_id, is_active=True)
        
        for field, value in kwargs.items():
            if hasattr(user, field) and field not in ['id', 'created_at']:
                setattr(user, field, value)
        
        user.save()
        
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'updated_at': user.updated_at,
            'message': 'User updated successfully'
        }
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}

def delete_user(user_id):
    """Soft delete a user"""
    try:
        user = User.objects.get(id=user_id)
        user.is_active = False
        user.save()
        return {'message': 'User deleted successfully'}
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}

def login_user(email, password):
    """Authenticate user with email and password"""
    try:
        # Get user by email
        user = User.objects.get(email=email, is_active=True)
        print(f"User found: {user.name} with email {user.email}")
        if user.is_verified is False:
            return {'error': 'User is not verified'}
        # Check password
        if check_password(password, user.password):
            # Update last login
            from django.utils import timezone
            user.last_login = timezone.now()
            user.save()
            
            return {
                'success': True,
                'user': {
                    'id': user.id,
                    'name': user.name,
                    'email': user.email,
                    'avatar': user.avatar,
                    'created_at': user.created_at,
                    'updated_at': user.updated_at
                }
            }
        
        else:
            return {'error': 'Invalid password'}
            
    except User.DoesNotExist:
        return {'error': 'User not found'}
    except Exception as e:
        return {'error': str(e)}

# NEW AUTHENTICATION FUNCTIONS FROM ACCOUNTS APP

def register_user(name, email, password, **extra_fields):
    """Register a new user with email verification"""
    try:
        if User.objects.filter(email=email).exists():
            return {'error': 'Email already exists'}
        
        user = User.objects.create_user(
            email=email,
            name=name,
            password=password,
            is_active=False,  # Require email verification
            is_verified=False,  # Set as unverified
            **extra_fields
        )
        
        # Generate email verification token
        token = jwt.encode({
            'user_id': user.id,
            'action': 'email_verification',
            'exp': datetime.utcnow() + timedelta(hours=24)
        }, settings.SECRET_KEY, algorithm='HS256')
        
        # Link points directly to backend verification endpoint
        verification_url = f"{settings.DEFAULT_API_URL}api/users/verify-email/?token={token}"
        send_mail(
            'Verify your MusicRoom account',
            f'Please click the link to verify your email: {verification_url}',
            settings.EMAIL_HOST_USER,
            [user.email],
            fail_silently=False,
        )
        
        return {
            'id': user.id,
            'name': user.name,
            'email': user.email,
            'avatar': user.avatar,
            'message': 'Registration successful. Please check your email to verify your account.'
        }
    except Exception as e:
        return {'error': str(e)}

def login_user_jwt(email, password):
    """Login user and return JWT tokens"""
    try:
        user = authenticate(email=email, password=password)
        if not user:
            return {'error': 'Invalid email or password'}
        
        if not user.is_active:
            return {'error': 'Account is not verified. Please check your email.'}
        
        refresh = RefreshToken.for_user(user)
        
        return {
            'user': {
                'id': user.id,
                'name': user.name,
                'email': user.email,
                'avatar': user.avatar
            },
            'tokens': {
                'refresh': str(refresh),
                'access': str(refresh.access_token),
            },
            'message': 'Login successful'
        }
    except Exception as e:
        return {'error': str(e)}

def verify_email(token):
    """Verify user email with token"""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        user = User.objects.get(id=payload['user_id'])
        
        # Check if this is an email verification token
        if payload.get('action') != 'email_verification':
            return {'error': 'Invalid token type'}
        
        if not user.is_verified:
            user.is_active = True
            user.is_verified = True
            user.save()
            return {'message': 'Email verified successfully! You can now log in to your account.'}
        else:
            return {'message': 'Email already verified'}
            
    except jwt.ExpiredSignatureError:
        return {'error': 'Verification link has expired. Please request a new verification email.'}
    except (jwt.DecodeError, User.DoesNotExist):
        return {'error': 'Invalid verification link'}
    except Exception as e:
        return {'error': str(e)}

def logout_user(refresh_token):
    """Logout user by blacklisting refresh token"""
    try:
        token = RefreshToken(refresh_token)
        token.blacklist()
        return {'message': 'Successfully logged out'}
    except Exception:
        return {'error': 'Invalid token'}

def reset_password_request(email):
    """Send password reset OTP to email"""
    try:
        if not User.objects.filter(email=email).exists():
            return {'error': 'User with this email does not exist'}
        
        user = User.objects.get(email=email)
        
        # Generate 6-digit OTP
        import random
        otp = ''.join([str(random.randint(0, 9)) for _ in range(6)])
        
        # Save OTP to user
        user.password_reset_otp = otp
        user.password_reset_otp_created_at = datetime.utcnow()
        user.password_reset_otp_verified = False
        user.save()
        
        # Send OTP via email
        send_mail(
            'MusicRoom Password Reset OTP',
            f'Your password reset OTP is: {otp}\n\nThis OTP will expire in 10 minutes.\n\nIf you did not request this, please ignore this email.',
            settings.EMAIL_HOST_USER,
            [user.email],
            fail_silently=False,
        )
        
        return {'message': 'OTP envoyée à votre e-mail'}
    except Exception as e:
        return {'error': str(e)}

def verify_password_reset_otp(email, otp):
    """Verify OTP for password reset"""
    try:
        user = User.objects.get(email=email)
        
        # Check if OTP exists
        if not user.password_reset_otp:
            return {'error': 'Aucun OTP trouvé. Veuillez demander une réinitialisation de mot de passe.'}
        
        # Check if OTP is expired (10 minutes)
        if user.password_reset_otp_created_at:
            time_diff = datetime.utcnow() - user.password_reset_otp_created_at.replace(tzinfo=None)
            if time_diff.total_seconds() > 600:  # 10 minutes
                # Clear expired OTP
                user.password_reset_otp = None
                user.password_reset_otp_created_at = None
                user.password_reset_otp_verified = False
                user.save()
                return {'error': 'L\'OTP a expiré. Veuillez demander une réinitialisation de votre mot de passe.'}
        
        # Check if OTP matches
        if user.password_reset_otp != otp:
            return {'error': 'OTP non valide. Veuillez réessayer.'}
        
        # Mark OTP as verified
        user.password_reset_otp_verified = True
        user.save()
        
        return {'message': 'OTP vérifié avec succès. Vous pouvez maintenant définir votre nouveau mot de passe.'}
        
    except User.DoesNotExist:
        return {'error': 'Aucun enregistrement trouvé par cet e-mail'}
    except Exception as e:
        return {'error': str(e)}

def reset_password_confirm(email, otp, password):
    """Confirm password reset with OTP and new password"""
    try:
        user = User.objects.get(email=email)
        
        # Check if OTP is verified
        if not user.password_reset_otp_verified:
            return {'error': 'OTP non vérifié. Veuillez vérifier l\'OTP d\'abord.'}
        
        # Check if OTP exists and matches
        if not user.password_reset_otp or user.password_reset_otp != otp:
            return {'error': 'OTP non valide.'}
        
        # Check if OTP is still valid (extend to 15 minutes for password change)
        if user.password_reset_otp_created_at:
            time_diff = datetime.utcnow() - user.password_reset_otp_created_at.replace(tzinfo=None)
            if time_diff.total_seconds() > 900:  # 15 minutes
                # Clear expired OTP
                user.password_reset_otp = None
                user.password_reset_otp_created_at = None
                user.password_reset_otp_verified = False
                user.save()
                return {'error': 'Votre session OTP a expiré. Veuillez demander une réinitialisation de votre mot de passe.'}
        
        # Set new password
        user.set_password(password)
        
        # Clear OTP data
        user.password_reset_otp = None
        user.password_reset_otp_created_at = None
        user.password_reset_otp_verified = False
        user.save()
        
        return {'message': 'Réinitialisation du mot de passe réussie ! Vous pouvez maintenant vous connecter avec votre nouveau mot de passe..'}
        
    except User.DoesNotExist:
        return {'error': 'Aucun enregistrement trouvé par cet e-mail'}
    except Exception as e:
        return {'error': str(e)}

def link_social_account(user, provider, token):
    """Link social media account to user profile"""
    try:
        if provider == "facebook":
            social_id = verify_facebook_token(token)
            
            if User.objects.exclude(id=user.id).filter(facebook_id=social_id).exists():
                return {'error': 'This Facebook account is already linked to another user'}
            
            user.facebook_id = social_id
            user.save()
            
        elif provider == "google":
            social_id = verify_google_token(token)
            
            if User.objects.exclude(id=user.id).filter(google_id=social_id).exists():
                return {'error': 'This Google account is already linked to another user'}
            
            user.google_id = social_id
            user.save()
        
        return {'message': f'{provider.capitalize()} account linked successfully'}
    except Exception as e:
        return {'error': str(e)}

def verify_facebook_token(token):
    """Verify Facebook access token and return user ID"""
    url = f"https://graph.facebook.com/me?access_token={token}"
    response = requests.get(url)
    
    if response.status_code != 200:
        raise ValidationError("Invalid Facebook token")
    
    data = response.json()
    if 'id' not in data:
        raise ValidationError("Could not retrieve Facebook user ID")
    
    return data['id']

def verify_google_token(token):
    """Verify Google token (supports both ID tokens and access tokens) and return user ID"""
    # First, try as ID token
    try:
        url = f"https://oauth2.googleapis.com/tokeninfo?id_token={token}"
        response = requests.get(url, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            if 'sub' in data:
                return data['sub']
    except Exception:
        pass
    
    # If ID token fails, try as access token
    try:
        url = f"https://oauth2.googleapis.com/tokeninfo?access_token={token}"
        response = requests.get(url, timeout=10)
        
        if response.status_code == 200:
            data = response.json()
            # For access tokens, we need to make another call to get user info
            if 'scope' in data:  # This confirms it's a valid access token
                user_info_url = f"https://www.googleapis.com/oauth2/v2/userinfo?access_token={token}"
                user_response = requests.get(user_info_url, timeout=10)
                
                if user_response.status_code == 200:
                    user_data = user_response.json()
                    if 'id' in user_data:
                        return user_data['id']
    except Exception:
        pass
    
    # If both fail, raise an error
    raise ValidationError("Jeton Google non valide")

def get_user_profile(user):
    """Get user profile data"""
    return {
        'id': user.id,
        'email': user.email,
        'name': user.name,
        'avatar': user.avatar,
        'bio': user.bio,
        'date_of_birth': user.date_of_birth,
        'phone_number': user.phone_number,
        'profile_privacy': user.profile_privacy,
        'email_privacy': user.email_privacy,
        'phone_privacy': user.phone_privacy,
        'facebook_id': user.facebook_id,
        'google_id': user.google_id,
        'subscription_type': user.subscription_type,
        'is_premium': user.is_premium,
        'is_subscribed': user.is_subscribed,
        'music_preferences': user.music_preferences,
        'liked_artists': user.liked_artists,
        'liked_albums': user.liked_albums,
        'liked_songs': user.liked_songs,
        'genres': user.genres,
        'created_at': user.created_at
    }

def update_user_profile(user, **data):
    """Update user profile"""
    try:
        allowed_fields = [
            'name', 'avatar', 'bio', 'date_of_birth', 'phone_number',
            'profile_privacy', 'email_privacy', 'phone_privacy',
            'music_preferences', 'liked_artists', 'liked_albums', 
            'liked_songs', 'genres', 'last_song_played'
        ]
        
        for field, value in data.items():
            if field in allowed_fields and hasattr(user, field):
                setattr(user, field, value)
        
        user.save()
        return get_user_profile(user)
    except Exception as e:
        return {'error': str(e)}
    
def resend_verification_email(email):
    """Resend verification email to user"""
    try:
        user = User.objects.get(email=email)
        
        if user.is_verified:
            return {'error': 'Email is already verified'}
        
        # Generate new verification token
        token = jwt.encode({
            'user_id': user.id,
            'action': 'email_verification',
            'exp': datetime.utcnow() + timedelta(hours=24)
        }, settings.SECRET_KEY, algorithm='HS256')
        
        # Send verification email with backend link
        verification_url = f"{settings.DEFAULT_API_URL}api/users/verify-email/?token={token}"
        send_mail(
            'Verify your MusicRoom account',
            f'Please click the link to verify your email: {verification_url}',
            settings.EMAIL_HOST_USER,
            [user.email],
            fail_silently=False,
        )
        
        return {'message': 'Verification email sent successfully'}
    except User.DoesNotExist:
        return {'error': 'User with this email does not exist'}
    except Exception as e:
        return {'error': str(e)}