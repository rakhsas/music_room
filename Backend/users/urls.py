from django.urls import path
from . import views


urlpatterns = [
    # Basic user CRUD
    path('', views.get_all_users_view, name='get_all_users'),
    #path('<str:user_name>/', views.get_user_by_name_view, name='get_user_by_name'),
    
    # Authentication endpoints (merged from accounts)
    path('create/', views.register_user_view, name='register_user'),
    path('login/', views.login_jwt_view, name='login_jwt'),
    path('logout/', views.logout_view, name='logout'),
    path('verify-email/', views.verify_email_view, name='verify_email'),
    path('password-reset/', views.password_reset_view, name='password_reset'),
    path('password-reset-verify-otp/', views.verify_password_reset_otp_view, name='verify_password_reset_otp'),
    path('password-reset-confirm/', views.password_reset_confirm_view, name='password_reset_confirm'),
    path('social-link/', views.social_link_view, name='social_link'),
    
    # Profile endpoints
    path('profile/', views.profile_view, name='profile'),
    path('profile/update/', views.profile_update_view, name='profile_update'),
    
    # Administrative endpoints
    
]