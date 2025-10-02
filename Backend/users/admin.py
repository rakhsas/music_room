from django.contrib import admin
from .models import User

@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ['name', 'email', 'is_active', 'subscription_type', 'created_at']
    list_filter = ['is_active', 'subscription_type', 'is_verified']
    search_fields = ['name', 'email']
