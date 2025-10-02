"""
URL configuration for MusicRoom project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path, include, re_path
from rest_framework import permissions
from drf_yasg.views import get_schema_view
from drf_yasg import openapi
from drf_yasg.generators import OpenAPISchemaGenerator
from . import views

class CustomSchemaGenerator(OpenAPISchemaGenerator):
    def get_operation(self, view, path, prefix, method, components, request):
        operation = super().get_operation(view, path, prefix, method, components, request)
                
        # Automatically set tags based on URL path
        if '/home/' in path:
            operation.tags = ['Home']
        elif '/events/' in path:
            operation.tags = ['Events']
        elif '/playlists/' in path:
            operation.tags = ['Playlists']
        elif '/music/' in path:
            operation.tags = ['Music']
        elif '/users/' in path:
            operation.tags = ['Users']
        elif path.startswith('/api/') and not any(x in path for x in ['/home/', '/events/', '/playlists/', '/music/', '/users/']):
            # For paths that start with /api/ but don't match specific apps
            operation.tags = ['Users']  # Since you have path('api/', include('users.urls'))
        
        return operation

# Simplified schema view for testing
schema_view = get_schema_view(
    openapi.Info(
        title="Music Room API",
        default_version='v1',
        description="API documentation for Music Room application",
    ),
    public=True,
    permission_classes=[permissions.AllowAny],
    # url='https://crispy-fishstick-v7x7p6vgj75hpxrx-8000.app.github.dev/',  # Add this line
    url='http://10.32.126.37:8000/',  # Add this line

)

from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('api/home/', include('home.urls')),    
    path('api/events/', include('events.urls')),
    path('api/playlists/', include('playlists.urls')),
    path('api/users/', include('users.urls')),
    path('api/music/', include('music.urls')),
    
    # Swagger URLs
    path('swagger/', schema_view.with_ui('swagger', cache_timeout=0), name='schema-swagger-ui'),
    path('redoc/', schema_view.with_ui('redoc', cache_timeout=0), name='schema-redoc'),
    
    path('api-auth/', include('rest_framework.urls', namespace='rest_framework')),
]

# Serve static and media files in development
if settings.DEBUG:
    urlpatterns += static(settings.STATIC_URL, document_root=settings.STATIC_ROOT)
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)