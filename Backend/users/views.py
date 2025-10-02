from django.conf import settings
from django.shortcuts import render
import jwt
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from drf_yasg.utils import swagger_auto_schema
from drf_yasg import openapi
from .services import (
    get_all_users, 
    get_user_by_id, 
    get_user_by_email, 
    create_user, 
    update_user, 
    delete_user,
    login_user,
    # New authentication functions
    register_user,
    login_user_jwt,
    logout_user,
    verify_email,
    reset_password_request,
    verify_password_reset_otp,
    reset_password_confirm,
    link_social_account,
    get_user_profile,
    update_user_profile,
    get_user_by_name
)


@swagger_auto_schema(
    method='get',
    operation_summary="Get user by name",
    operation_description="Retrieve user details by their name",
    manual_parameters=[
        openapi.Parameter(
            'user_name',
            openapi.IN_PATH,
            description="Name of the user to retrieve",
            type=openapi.TYPE_STRING,
            required=True,
            example="John Doe"
        )
    ],
    responses={
        200: openapi.Response(
            description="User found",
            examples={
                'application/json': {
                    'id': 1,
                    'name': 'John Doe',
                    'email': 'john@example.com',
                    'created_at': '2024-01-01T12:00:00Z',
                    'updated_at': '2024-01-01T12:00:00Z'
                }
            }
        ),
        404: openapi.Response(
            description="User not found",
            examples={
                'application/json': {
                    'error': 'User not found'
                }
            }
        )
    }
)
@api_view(['GET'])
@permission_classes([AllowAny])
def get_user_by_name_view(request, user_name):
    """Get user details by name"""
    user = get_user_by_name(user_name)
    if 'error' in user:
        return Response(user, status=status.HTTP_404_NOT_FOUND)
    return Response(user, status=status.HTTP_200_OK)

@swagger_auto_schema(method='get', operation_summary="Get all users except current user")
@api_view(['GET'])
@permission_classes([AllowAny]) # Require authentication to know the current user
def get_all_users_view(request):
    """Get list of all users except current user"""
    users = get_all_users()
    
    if 'error' in users:
        return Response(users, status=status.HTTP_400_BAD_REQUEST)

    current_user_id = request.user.id  

    # Exclude current user from the list
    filtered_users = [user for user in users if user.get('id') != current_user_id]

    return Response(filtered_users, status=status.HTTP_200_OK)

# NEW AUTHENTICATION VIEWS FROM ACCOUNTS APP


@swagger_auto_schema(
    method='post',
    operation_summary="Register a new user",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'name': openapi.Schema(type=openapi.TYPE_STRING, description="User's full name"),
            'email': openapi.Schema(type=openapi.TYPE_STRING, description="User's email address"),
            'password': openapi.Schema(type=openapi.TYPE_STRING, description="User's password"),
        },
        required=['name', 'email', 'password']
    ),
    responses={
        201: openapi.Response(description="User registered successfully"),
        400: openapi.Response(description="Invalid input"),
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def register_user_view(request):
    """Register a new user"""
    data = request.data
    name = data.get('name')
    email = data.get('email')
    password = data.get('password')

    if not all([name, email, password]):
        return Response({'error': 'Missing required fields'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = register_user(name, email, password)

    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)

    return Response(result, status=status.HTTP_201_CREATED)


@swagger_auto_schema(
    method='post',
    operation_summary="Login with JWT tokens",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'email': openapi.Schema(type=openapi.TYPE_STRING, description="User's email"),
            'password': openapi.Schema(type=openapi.TYPE_STRING, description="User's password"),
        },
        required=['email', 'password']
    ),
    responses={
        200: openapi.Response(description="Login successful"),
        400: openapi.Response(description="Invalid credentials"),
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def login_jwt_view(request):
    """Login user with JWT tokens"""
    data = request.data
    email = data.get('email')
    password = data.get('password')

    if not email or not password:
        return Response({'error': 'Email and password are required'}, status=status.HTTP_400_BAD_REQUEST)

    result = login_user_jwt(email, password)

    if 'error' in result:
        return Response(result, status=status.HTTP_401_UNAUTHORIZED)

    return Response(result, status=status.HTTP_200_OK)

@swagger_auto_schema(
    method='post',
    operation_summary="Logout user",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'refresh_token': openapi.Schema(type=openapi.TYPE_STRING, description="Refresh token"),
        },
        required=['refresh_token']
    )
)
@api_view(['POST'])
def logout_view(request):
    """Logout user by blacklisting refresh token"""
    refresh_token = request.data.get('refresh_token')
    
    if not refresh_token:
        return Response({'error': 'Refresh token is required'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = logout_user(refresh_token)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response(result, status=status.HTTP_200_OK)


@swagger_auto_schema(
    method='post',
    operation_summary="Verify user email",
    operation_description="Verify user email using token from email link",
    manual_parameters=[
        openapi.Parameter(
            'token',
            openapi.IN_QUERY,
            description="Email verification token (for GET requests)",
            type=openapi.TYPE_STRING,
            required=False
        )
    ],
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'token': openapi.Schema(
                type=openapi.TYPE_STRING,
                description="Email verification token (for POST requests)"
            )
        }
    ),
    responses={
        200: openapi.Response(
            description="Email verified successfully",
            examples={
                'application/json': {
                    'message': 'Email verified successfully! You can now log in to your account.'
                }
            }
        ),
        400: openapi.Response(
            description="Invalid or expired token",
            examples={
                'application/json': {
                    'error': 'Invalid verification link'
                }
            }
        )
    }
)
@api_view(['GET', 'POST'])
@permission_classes([AllowAny])
def verify_email_view(request):
    """Verify user email with token"""
    # Handle GET request (when user clicks email link)
    if request.method == 'GET':
        token = request.GET.get('token')
        
        # For GET requests, render the HTML template
        if not token:
            context = {
                'status': 'error',
                'title': 'Verification Failed',
                'message': 'No verification token provided',
                'icon_type': 'error'
            }
            return render(request, 'users/email_verified.html', context)
        
        result = verify_email(token)
        
        if 'error' in result:
            # Handle different error types
            if 'Invalid verification link' in result['error']:
                context = {
                    'status': 'error',
                    'title': 'Invalid Link',
                    'message': 'This verification link is invalid or corrupted. Please request a new verification email.',
                    'icon_type': 'error'
                }
            elif 'expired' in result['error'].lower():
                context = {
                    'status': 'error',
                    'title': 'Link Expired',
                    'message': 'This verification link has expired. Please request a new verification email.',
                    'icon_type': 'error'
                }
            else:
                context = {
                    'status': 'error',
                    'title': 'Verification Failed',
                    'message': result['error'],
                    'icon_type': 'error'
                }
        else:
            # Handle success cases
            if 'already verified' in result['message'].lower():
                context = {
                    'status': 'already_verified',
                    'title': 'Already Verified',
                    'message': 'Your email has already been verified. You can proceed to log in.',
                    'icon_type': 'info'
                }
            else:
                context = {
                    'status': 'success',
                    'title': 'Verified Successfully',
                    'message': 'Email verified successfully! You can now log in to your account.',
                    'icon_type': 'success'
                }
        
        return render(request, 'users/email_verified.html', context)
    
        
        # For GET requests, render the HTML template
        if not token:
            context = {
                'status': 'error',
                'title': 'Verification Failed',
                'message': 'No verification token provided',
                'icon_type': 'error'
            }
            return render(request, 'users/email_verified.html', context)
        
        result = verify_email(token)
        
        if 'error' in result:
            # Handle different error types
            if 'Invalid verification link' in result['error']:
                context = {
                    'status': 'error',
                    'title': 'Invalid Link',
                    'message': 'This verification link is invalid or corrupted. Please request a new verification email.',
                    'icon_type': 'error'
                }
            elif 'expired' in result['error'].lower():
                context = {
                    'status': 'error',
                    'title': 'Link Expired',
                    'message': 'This verification link has expired. Please request a new verification email.',
                    'icon_type': 'error'
                }
            else:
                context = {
                    'status': 'error',
                    'title': 'Verification Failed',
                    'message': result['error'],
                    'icon_type': 'error'
                }
        else:
            # Handle success cases
            if 'already verified' in result['message'].lower():
                context = {
                    'status': 'already_verified',
                    'title': 'Already Verified',
                    'message': 'Your email has already been verified. You can proceed to log in.',
                    'icon_type': 'info'
                }
            else:
                context = {
                    'status': 'success',
                    'title': 'Verified Successfully',
                    'message': 'Email verified successfully! You can now log in to your account.',
                    'icon_type': 'success'
                }
        
        return render(request, 'users/email_verified.html', context)
    
    # Handle POST request (API call)
    else:
        token = request.data.get('token')
        
        if not token:
            return Response({'error': 'Token is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        result = verify_email(token)
        
        if 'error' in result:
            return Response(result, status=status.HTTP_400_BAD_REQUEST)
        
        return Response(result, status=status.HTTP_200_OK)
        
        if not token:
            return Response({'error': 'Token is required'}, status=status.HTTP_400_BAD_REQUEST)
        
        result = verify_email(token)
        
        if 'error' in result:
            return Response(result, status=status.HTTP_400_BAD_REQUEST)
        
        return Response(result, status=status.HTTP_200_OK)



@swagger_auto_schema(
    method='post',
    operation_summary="Request password reset OTP",
    operation_description="Send a 6-digit OTP to user's email for password reset",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'email': openapi.Schema(type=openapi.TYPE_STRING, description="User's email"),
        },
        required=['email']
    ),
    responses={
        200: openapi.Response(
            description="OTP sent successfully",
            examples={
                'application/json': {
                    'message': 'Password reset OTP sent to your email'
                }
            }
        ),
        400: openapi.Response(
            description="Invalid email or user not found",
            examples={
                'application/json': {
                    'error': 'User with this email does not exist'
                }
            }
        )
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def password_reset_view(request):
    """Request password reset OTP"""
    email = request.data.get('email')
    
    if not email:
        return Response({'error': 'Email is required'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = reset_password_request(email)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response(result, status=status.HTTP_200_OK)

@swagger_auto_schema(
    method='post',
    operation_summary="Verify password reset OTP",
    operation_description="Verify the 6-digit OTP sent to user's email",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'email': openapi.Schema(type=openapi.TYPE_STRING, description="User's email"),
            'otp': openapi.Schema(type=openapi.TYPE_STRING, description="6-digit OTP from email"),
        },
        required=['email', 'otp']
    ),
    responses={
        200: openapi.Response(
            description="OTP verified successfully",
            examples={
                'application/json': {
                    'message': 'OTP verified successfully. You can now set your new password.'
                }
            }
        ),
        400: openapi.Response(
            description="Invalid or expired OTP",
            examples={
                'application/json': {
                    'error': 'Invalid OTP. Please try again.'
                }
            }
        )
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def verify_password_reset_otp_view(request):
    """Verify OTP for password reset"""
    email = request.data.get('email')
    otp = request.data.get('otp')
    
    if not all([email, otp]):
        return Response({'error': 'Email and OTP are required'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = verify_password_reset_otp(email, otp)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response(result, status=status.HTTP_200_OK)


@swagger_auto_schema(
    method='post',
    operation_summary="Reset password with OTP",
    operation_description="Reset password using verified OTP and new password",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'email': openapi.Schema(
                type=openapi.TYPE_STRING,
                description="User's email address"
            ),
            'otp': openapi.Schema(
                type=openapi.TYPE_STRING,
                description="6-digit OTP (must be verified first)"
            ),
            'password': openapi.Schema(
                type=openapi.TYPE_STRING,
                description="New password (minimum 8 characters)"
            ),
            'password_confirm': openapi.Schema(
                type=openapi.TYPE_STRING,
                description="Confirm new password"
            )
        },
        required=['email', 'otp', 'password', 'password_confirm']
    ),
    responses={
        200: openapi.Response(
            description="Password reset successful",
            examples={
                'application/json': {
                    'message': 'Password reset successful! You can now log in with your new password.'
                }
            }
        ),
        400: openapi.Response(
            description="Invalid OTP, passwords don't match, or other validation error",
            examples={
                'application/json': {
                    'error': 'OTP not verified. Please verify OTP first.'
                }
            }
        )
    }
)
@api_view(['POST'])
@permission_classes([AllowAny])
def password_reset_confirm_view(request):
    """Reset password with verified OTP"""
    email = request.data.get('email')
    otp = request.data.get('otp')
    password = request.data.get('password')
    password_confirm = request.data.get('password_confirm')
    
    if not all([email, otp, password, password_confirm]):
        return Response({'error': 'Email, OTP, password, and password confirmation are required'}, status=status.HTTP_400_BAD_REQUEST)
    
    if password != password_confirm:
        return Response({'error': 'Passwords do not match'}, status=status.HTTP_400_BAD_REQUEST)
    
    if len(password) < 8:
        return Response({'error': 'Password must be at least 8 characters long'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = reset_password_confirm(email, otp, password)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response(result, status=status.HTTP_200_OK)

@swagger_auto_schema(
    method='post',
    operation_summary="Link social account",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'provider': openapi.Schema(type=openapi.TYPE_STRING, enum=['facebook', 'google']),
            'access_token': openapi.Schema(type=openapi.TYPE_STRING, description="Social media token (access token or ID token for Google)"),
        },
        required=['provider', 'access_token']
    )
)
@api_view(['POST'])
def social_link_view(request):
    """Link social media account"""
    if not request.user.is_authenticated:
        return Response({'error': 'Authentication required'}, status=status.HTTP_401_UNAUTHORIZED)
    
    data = request.data
    provider = data.get('provider')
    access_token = data.get('access_token')
    
    if not all([provider, access_token]):
        return Response({'error': 'Provider and access_token are required'}, status=status.HTTP_400_BAD_REQUEST)
    
    if provider not in ['facebook', 'google']:
        return Response({'error': 'Invalid provider'}, status=status.HTTP_400_BAD_REQUEST)
    
    result = link_social_account(request.user, provider, access_token)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response(result, status=status.HTTP_200_OK)

@api_view(['GET'])
@swagger_auto_schema(operation_summary="Get user profile")
def profile_view(request):
    """Get current user profile"""
    if not request.user.is_authenticated:
        return Response({'error': 'Authentication required'}, status=status.HTTP_401_UNAUTHORIZED)
    
    profile = get_user_profile(request.user)
    return Response(profile, status=status.HTTP_200_OK)

@swagger_auto_schema(
    method='put',
    operation_summary="Update user profile",
    request_body=openapi.Schema(
        type=openapi.TYPE_OBJECT,
        properties={
            'name': openapi.Schema(type=openapi.TYPE_STRING),
            'bio': openapi.Schema(type=openapi.TYPE_STRING),
            'date_of_birth': openapi.Schema(type=openapi.TYPE_STRING, format='date'),
            'phone_number': openapi.Schema(type=openapi.TYPE_STRING),
            'profile_privacy': openapi.Schema(type=openapi.TYPE_STRING),
            'email_privacy': openapi.Schema(type=openapi.TYPE_STRING),
            'phone_privacy': openapi.Schema(type=openapi.TYPE_STRING),
            'music_preferences': openapi.Schema(type=openapi.TYPE_OBJECT),
            'liked_artists': openapi.Schema(type=openapi.TYPE_ARRAY, items=openapi.Schema(type=openapi.TYPE_STRING)),
            'liked_albums': openapi.Schema(type=openapi.TYPE_ARRAY, items=openapi.Schema(type=openapi.TYPE_STRING)),
            'liked_songs': openapi.Schema(type=openapi.TYPE_ARRAY, items=openapi.Schema(type=openapi.TYPE_STRING)),
            'genres': openapi.Schema(type=openapi.TYPE_ARRAY, items=openapi.Schema(type=openapi.TYPE_STRING)),
        }
    )
)
@api_view(['PUT'])
def profile_update_view(request):
    """Update user profile"""
    if not request.user.is_authenticated:
        return Response({'error': 'Authentication required'}, status=status.HTTP_401_UNAUTHORIZED)
    
    result = update_user_profile(request.user, **request.data)
    
    if 'error' in result:
        return Response(result, status=status.HTTP_400_BAD_REQUEST)
    
    return Response({'message': 'Profile updated successfully', 'user': result}, status=status.HTTP_200_OK)